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
    private static Thread inboundDataRouter;
    /**
     * Responsible for sending data to clients
     */
    private static Thread outboundDataRouter;
    /**
     * Queue of messages to be sent out to clients
     */
    protected static BlockingQueue<Message> outBoundMessageQueue = new LinkedBlockingQueue<Message>();
    /**
     * Monitored by Algorand. Contains transactions that the node receives
     */
    protected static BlockingQueue<Transaction> transactionQueue = new LinkedBlockingQueue<>();
    /**
     * Monitored by Algorand. Contains proposed blocks (proposals) received.
     */
    protected static BlockingQueue<Block> proposedBlockQueue = new LinkedBlockingQueue<>();
    /**
     * Monitored by Algorand. Contains block hashes (votes) received
     */
    public static BlockingQueue<Message> blockHashQueue = new LinkedBlockingQueue<>();
    /**
     * Monitored by Algorand. Contains requested blocks.
     */
    public static BlockingQueue<Block> requestedBlockQueue = new LinkedBlockingQueue<>();
    /**
     * Contains all transactions ever seen by this node. Helps it decide whether or not
     * to gossip a transaction
     */
    public static Set<Integer> transactionsSeen = new HashSet<>();
    /**
     * Contains all block proposals ever seen by this node. Helps it decide whether or not
     * to gossip a block proposal.
     */
    public static Set<String> blockProposalsSeen = new HashSet<>();
    /**
     * Contains all block hash votes ever seen by this node. Helps it decide whether or not
     * to gossip a block hash vote.
     */
    public static Set<String> blockHashVotesSeen = new HashSet<>();


    /**
     * Initializes ConnectionManager
     * @param inboundPort - listening port
     * @param outboundPort - data sending port
     */
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

        inboundDataRouter = new Thread(new InboundDataRouter());
        outboundDataRouter = new Thread(new OutboundDataRouter());
        inboundDataRouter.start();
        outboundDataRouter.start();

        /* make connection to known clients and ask them to
           forward your inbound address to other clients */
        System.out.println("Finding peers...");
        gossip(Message.MessageType.ADDR, inboundPort, null);
        System.out.println("Catching node up with the network...");
        bootstrap();
        System.out.println("Node caught up");
    }

    /**
     * Stops the threads that compose the connection manager and its functions
     */
    public static void stop() {
        seedServerRequestor.interrupt();
        inboundDataRouter.interrupt();
        outboundDataRouter.interrupt();
    }

    /**
     * Catches the node up to the rest of the network. Ask peers for the chain
     * until you receive it
     */
    public static void bootstrap() {
        knownClientsLock.lock();
        try {
            for (int address : knownClients) {
                Message message = Message.buildBlockChainMessage(inboundPort, address);
                outBoundMessageQueue.add(message);
                /* wait some amount of time to get back a response*/
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Algorand.round = LedgerManager.getLastBlock().round + 1;
                break;
                /* check that the ledger updated */
                /*
                if (LedgerManager.getLedgerSize() > 1) {
                    Block lastBlock = LedgerManager.getLastBlock();
                    Algorand.round = lastBlock.round + 2;
                    return;
                }*/
            }
        } finally {
            knownClientsLock.unlock();
        }
    }

    /**
     * Send a transaction created from this client
     */
    public static void sendTransaction(int id ,String source, String recipient, int amount) {
        Transaction tx = new Transaction(id, source, recipient, amount);
        gossip(Message.MessageType.TRANSACTION, tx, null);
        transactionQueue.add(tx);
    }

    /**
     * Gossips messages to all known addresses that are in the supplied excluded set
     * @param messageType - type of the message to gossip
     * @param data  - data specific to the message
     * @param excludedAddresses - list of addresses to not send the message to
     */
    public static void gossip(Message.MessageType messageType, Object data, Set<Integer> excludedAddresses) {
        knownClientsLock.lock();
        try {
            for(int address: knownClients) {
                if (address != inboundPort
                        && (excludedAddresses == null || (excludedAddresses != null && !excludedAddresses.contains(address)))) {
                    Message message = null;
                    switch(messageType) {
                        case ADDR:
                            message = Message.buildAddrMessage(inboundPort, address, (Integer) data);
                            break;
                        case BLOCK:
                            message = Message.buildBlockMessage(inboundPort, address, (Block) data);
                            break;
                        case BLOCK_HASH:
                            message = Message.buildBlockHashMessage(
                                    inboundPort, address, (BlockHashMessageData) data);
                            break;
                        case BLOCK_REQUEST:
                            message = Message.buildBlockReqMessage(inboundPort, address, (String) data);
                            break;
                        case TRANSACTION:
                            message = Message.buildTransactionMessage(inboundPort, address, (Transaction) data);
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
