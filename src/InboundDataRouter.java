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
public class InboundDataRouter implements Runnable {
    @Override
    public void run() {
        try {
            ServerSocket server = new ServerSocket(ConnectionManager.inboundPort);
            while(true) {
                Socket connection = server.accept();
                try {
                    ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream());
                    Message message = (Message) inputStream.readObject();
                    //System.out.println("INBOUND: " + message.type + " from " + message.sourceAddress);
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
                            break;
                        case BLOCK_CHAIN:
                            block_chain(message);
                            break;
                        case BLOCK_CHAIN_RES:
                            block_chain_res(message);
                            break;
                        case BLOCK_REQUEST:
                            block_request(message);
                            break;
                        case BLOCK_REQUEST_RES:
                            block_request_res(message);
                            break;
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
        ConnectionManager.knownClientsLock.lock();
        try {
            List<Integer> addresses = new LinkedList<>(ConnectionManager.knownClients);
            Message response = Message.buildAddrResponseMessage(ConnectionManager.inboundPort, message.sourceAddress, addresses);
            ConnectionManager.outBoundMessageQueue.add(response);
        } finally {
            ConnectionManager.knownClientsLock.unlock();
        }
    }

    /**
     * Merges received address list from message with this client's
     * known addresses
     * @param message - message received from another client
     */
    private void getaddr_res(Message message) {
        ConnectionManager.knownClientsLock.lock();
        try {
            ConnectionManager.knownClients.addAll(message.addresses);
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
            ConnectionManager.gossip(Message.MessageType.ADDR, message.relayAddress, excludedAddresses);
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
        Transaction tx = message.transaction;
        if (!ConnectionManager.transactionsSeen.contains(tx.id)) {
            ConnectionManager.transactionsSeen.add(tx.id);
            ConnectionManager.transactionQueue.add(tx);
            Set<Integer> excludedAddresses = new HashSet<>();
            excludedAddresses.add(message.sourceAddress);
            ConnectionManager.gossip(Message.MessageType.TRANSACTION, tx, excludedAddresses);
        }
    }

    /**
     * Process a block proposal message
     * @param message - message received from another client
     */
    private void block(Message message) {
        String blockHash = Block.getHash(message.block);
        if (!ConnectionManager.blockProposalsSeen.contains(blockHash)) {
            ConnectionManager.blockProposalsSeen.add(blockHash);
            Set<Integer> excludedAddresses = new HashSet<>();
            excludedAddresses.add(message.sourceAddress);
            ConnectionManager.gossip(Message.MessageType.BLOCK, message.block, excludedAddresses);
            ConnectionManager.proposedBlockQueue.add(message.block);
        }
    }

    /**
     * Process a block hash vote message.
     * @param message
     */
    private void block_hash(Message message) {
        String voteHash = Algorand.getVoteHash(message);
        if (!ConnectionManager.blockHashVotesSeen.contains(voteHash)) {
            ConnectionManager.blockHashVotesSeen.add(voteHash);
            ConnectionManager.blockHashQueue.add(message);

            Set<Integer> excludedAddresses = new HashSet<>();
            excludedAddresses.add(message.sourceAddress);
            excludedAddresses.add(message.voter);
            BlockHashMessageData data = new BlockHashMessageData(
                    message.voter, message.round, message.step, message.prevBlockHash, message.blockHash);
            ConnectionManager.gossip(Message.MessageType.BLOCK_HASH, data, excludedAddresses);
        }
    }

    private void block_chain(Message message) {
        List<Block> data = new LinkedList<>(LedgerManager.getLedger());
        data.remove(0); /* don't need to send genesis */
        Message response = Message.buildBlockChainResMessage(ConnectionManager.inboundPort, message.sourceAddress, data);
        ConnectionManager.outBoundMessageQueue.add(response);
    }

    private void block_chain_res(Message message) {
        List<Block> data = message.blockchain;
        LedgerManager.setLedger(data);
    }

    private void block_request(Message message) {
        /* check ledger */
        Block block = LedgerManager.getBlock(message.blockHash);
        if (block == null) { /* check proposed block queue */
            Set<Block> notIts = new HashSet<>();
            while (!ConnectionManager.proposedBlockQueue.isEmpty()) {
                try {
                    Block proposedBlock = ConnectionManager.proposedBlockQueue.take();
                    if (message.blockHash.compareTo(Block.getHash(proposedBlock)) == 0) {
                        block = proposedBlock;
                        break;
                    } else {
                        notIts.add(proposedBlock);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ConnectionManager.proposedBlockQueue.addAll(notIts);
        }

        if (block != null) {
            Message response = Message.buildBlockReqResMessage(ConnectionManager.inboundPort, message.sourceAddress, block);
            ConnectionManager.outBoundMessageQueue.add(response);
        }
    }

    private void block_request_res(Message message) {
        ConnectionManager.requestedBlockQueue.add(message.block);
    }
}