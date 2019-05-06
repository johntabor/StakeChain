import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;


public class ConnectionManager {
    /**
     * Port on which the client listens for data from other clients
     */
    protected static int inboundPort;
    /**
     * Port on which the client sends data to other clients
     */
    protected static int outboundPort;
    /**
     * Set of addresses of other clients that the client is aware of
     */
    protected static Set<Integer> knownClients = new HashSet<>();
    /**
     * Lock for knownClients
     */
    protected static ReentrantLock knownClientsLock = new ReentrantLock();
    /**
     * Responsible for obtaining a client address list from the seed server
     */
    private static Thread seedServerRequestor;
    /**
     * Responsible for accepting incoming data from clients
     */
    private static Thread inboundDataListener;
    /**
     * Responsible for sending data to clients
     */
    private static Thread outboundDataSender;
    /**
     * Queue of messages to be sent out to clients
     */
    protected static BlockingQueue<Message> outBoundMessageQueue = new LinkedBlockingQueue<Message>();
    /**
     * Monitored by Algorand. Contains all transactions that the node receives
     */
    protected static BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>();
    /**
     * Monitored by Algorand. Contains all proposed blocks received.
     */
    protected static BlockingQueue<Block> proposedBlockQueue = new LinkedBlockingQueue<>();
    /**
     * Monitored by Algorand. Contains all block hashes (votes) received
     */
    public static BlockingQueue<Message> blockHashQueue = new LinkedBlockingQueue<>();

    public static void init(int inboundPort, int outboundPort) {
        ConnectionManager.inboundPort = inboundPort;
        ConnectionManager.outboundPort = outboundPort;
    }

    /**
     * Obtains address list from seed server and then fires up
     * inbound and outbound data threads
     */
    public static void start() {
        seedServerRequestor = new Thread(new RequestorRunnable());
        seedServerRequestor.start();
        try {
            seedServerRequestor.join();
        } catch (InterruptedException e) {
            System.out.println("seedServerRequestor was interrupted");
            e.printStackTrace();
        }

        inboundDataListener = new Thread(new InboundDataListenerRunnable());
        outboundDataSender = new Thread(new OutboundDataSenderRunnable());
        inboundDataListener.start();
        outboundDataSender.start();

        /* make connection to known clients and ask them to
           forward your inbound address to other clients
         */
        gossip(Message.MessageType.ADDR, inboundPort, null);
    }

    /**
     * Stops the threads that compose the connection manager and its functions
     */
    public static void stop() {
        seedServerRequestor.interrupt();
        inboundDataListener.interrupt();
        outboundDataSender.interrupt();
    }

    public static void sendTransaction(int id ,String source, String recipient, int amount) {
        Transaction tx = new Transaction(id, source, recipient, amount);
        gossip(Message.MessageType.TRANSACTION, tx, null);
        transactionQueue.add(tx);
    }

    /**
     * Gossips messages to all known addresses that are not excluded
     * @param messageType - type of the message to gossip
     * @param dataObject  - data specific to the message
     * @param excludedAddresses - list of addresses to not send the message to
     */
    public static void gossip(Message.MessageType messageType, Object dataObject, Set<Integer> excludedAddresses) {
        knownClientsLock.lock();
        try {
            for(int address: knownClients) {
                if (address != inboundPort
                        && (excludedAddresses == null || (excludedAddresses != null && !excludedAddresses.contains(address)))) {
                    Message message = null;
                    switch(messageType) {
                        case TRANSACTION:
                            message = Message.buildTransactionMessage(inboundPort, address, (Transaction) dataObject);
                            break;
                        case BLOCK:
                            message = Message.buildBlockMessage(inboundPort, address, (Block) dataObject);
                            break;
                        case BLOCK_HASH:
                            message = Message.buildBlockHashMessage(
                                    inboundPort, address, (BlockHashMessageData) dataObject);
                            break;
                        case ADDR:
                            message = Message.buildAddrMessage(inboundPort, address, (Integer) dataObject);
                            break;
                    }

                    if (message != null) {
                        outBoundMessageQueue.add(message);
                    }
                }
            }
        } finally {
            knownClientsLock.unlock();
        }
    }
}
