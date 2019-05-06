import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Responsible for accepting connection
 * requests from other clients and routing incoming
 * messages
 */
public class InboundDataListenerRunnable implements Runnable {
    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(ConnectionManager.inboundPort);
            while(true) {
                Socket connection = server.accept();
                try {
                    ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream());
                    Message message = (Message) inputStream.readObject();
                    System.out.println("INBOUND: " + message.type + " from " + message.sourceAddress);
                    switch(message.type) {
                        case GETADDR:
                            getaddr(message);
                            break;
                        case GETADDR_RES:
                            getaddr_res(message);
                            break;
                        case ADDR:
                            addr(message);
                            break;
                        case TRANSACTION:
                            transaction(message);
                            break;
                        case BLOCK:
                            block(message);
                            break;
                        case BLOCK_HASH:
                            block_hash(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        connection.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prepares a response message with list of this client's known addresses
     * to send back.
     * @param message - message received from another client
     */
    private void getaddr(Message message) {
        System.out.println("Requesting addresses from client at port " + message.sourceAddress);
        List<Integer> addresses = new LinkedList<Integer>(ConnectionManager.knownClients);
        Message response = Message.buildAddrResponseMessage(ConnectionManager.inboundPort, message.sourceAddress, addresses);
        ConnectionManager.outBoundMessageQueue.add(response);
    }

    /**
     * Merges received address list from message with this client's
     * known addresses
     * @param message - message received from another client
     */
    private void getaddr_res(Message message) {
        System.out.println("Received address list");
        ConnectionManager.knownClientsLock.lock();
        try {
            for(int address : message.addresses) {
                ConnectionManager.knownClients.add(address);
            }
        } finally {
            ConnectionManager.knownClientsLock.unlock();
        }
    }

    /**
     * Relays the address of the client who sent to message
     * to all known clients
     * @param message
     */
    private void addr(Message message) {
        boolean gossip = false;
        ConnectionManager.knownClientsLock.lock();
        try {
            if (!ConnectionManager.knownClients.contains(message.relayAddress)) {
                ConnectionManager.knownClients.add(message.relayAddress);
                gossip = true;

            }
        } finally {
            ConnectionManager.knownClientsLock.unlock();
        }

        if (gossip) {
            Set<Integer> excludedAddresses = new HashSet<>();
            excludedAddresses.add(message.relayAddress);
            excludedAddresses.add(message.sourceAddress);
            ConnectionManager.gossip(message.relayAddress, excludedAddresses);
            //ConnectionManager.gossipAddress(message.relayAddress);
        }
    }

    /**
     * Process a received address
     * @param message - message received from another client
     */
    private void addr_res(Message message) {
        System.out.println("Received address");
        ConnectionManager.knownClientsLock.lock();
        try {
            ConnectionManager.knownClients.add(message.relayAddress);
        } finally {
            ConnectionManager.knownClientsLock.unlock();
        }
    }

    /**
     * Process a received transaction
     * @param message - message received from another client
     */
    private void transaction(Message message) {
        System.out.println("Received a transaction");
        Transaction tx = message.transaction;
        if (!LedgerManager.seenTransaction(tx)) {
            Set<Integer> excludedAddresses = new HashSet<>();
            excludedAddresses.add(message.sourceAddress);
            ConnectionManager.gossip(tx, excludedAddresses);
            //ConnectionManager.gossipTransaction(tx);
            ConnectionManager.transactionQueue.add(tx);
        }
    }

    /**
     *
     * @param message - message received from another client
     */
    private void block(Message message) {
        Algorand.highestPriorityProposalLock.lock();
        try {
            if (message.block.priority > Algorand.highestPriorityProposal) {
                Set<Integer> excludedAddresses = new HashSet<>();
                excludedAddresses.add(message.sourceAddress);
                ConnectionManager.gossip(message.block, excludedAddresses);
                ConnectionManager.proposedBlockQueue.add(message.block);
            }
        } finally {
            Algorand.highestPriorityProposalLock.unlock();
        }
    }

    private void block_hash(Message message) {
        ConnectionManager.blockHashQueue.add(message);
    }
}