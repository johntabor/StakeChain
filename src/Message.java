import java.io.Serializable;
import java.util.List;

/**
 * Holds data specific to a block hash message
 */
class BlockHashMessageData {
    public final int voter;
    public final int round;
    public final int step;
    public final String prevBlockHash;
    public final String blockHash;

    public BlockHashMessageData(int voter, int round, int step, String prevBlockHash, String blockHash) {
        this.voter = voter;
        this.round = round;
        this.step = step;
        this.prevBlockHash = prevBlockHash;
        this.blockHash = blockHash;
    }
}


public class Message implements Serializable {
    /**
     * Types of messages
     */
    public enum MessageType implements Serializable {
        /**
         * Request for a list of addresses from a client
         */
        GETADDR,
        /**
         * Response to GETADDR that contains a list of addresses
         */
        GETADDR_RES,
        /**
         * Request for a client to forward the sender's address to other clients
         */
        ADDR,
        /**
         * Transmission of a transaction
         */
        TRANSACTION,
        /**
         * Transmission of a block
         */
        BLOCK,
        /**
         * Transmission of a block hash
         */
        BLOCK_HASH,
        /**
         * Request for a copy of the blockchain
         */
        BLOCK_CHAIN,
        /**
         * Response to request for a copy of the blockchain
         */
        BLOCK_CHAIN_RES,
        /**
         * Request for a specific block
         */
        BLOCK_REQUEST,
        /**
         * Response to request for a specific block
         */
        BLOCK_REQUEST_RES
    }
    /**
     * The type of the message being sent.
     */
    public MessageType type = null;
    /**
     * Holds the inbound address (port) associated with the client
     * sending the message. Used by receiver to send message back
     * to the appropriate port.
     */
    public int sourceAddress = -1;
    /**
     * The destination of the message
     */
    public int destinationAddress = -1;
    /**
     * Address of a client that requested to be relayed to other clients
     */
    public int relayAddress = -1;
    /**
     * List of addresses to send back to a client
     */
    public List<Integer> addresses = null;
    /**
     * Transaction to be sent to a client
     */
    public Transaction transaction = null;
    /**
     * Block to be sent to a client
     */
    public Block block = null;
    /**
     * Hash of a block
     */
    public String blockHash = null;
    /**
     * Hash of previous block in the chain
     */
    public String prevBlockHash = null;
    /**
     * Origin address of vote
     */
    public int voter;
    /**
     * Round of the message
     */
    public int round;
    /**
     * Step of the message
     */
    public int step;
    /**
     * Blockchain to send
     */
    List<Block> blockchain = null;

    private Message(MessageType type, int sourceAddress, int destinationAddress) {
        this.type = type;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
    }


    public static Message buildAddrMessage(int sourceAddress, int destinationAddress, int relayAddress) {
        Message message = new Message(MessageType.ADDR, sourceAddress, destinationAddress);
        message.relayAddress = relayAddress;
        return message;
    }

    public static Message buildGetAddrMessage(int sourceAddress, int destinationAddress) {
        Message message = new Message(MessageType.GETADDR, sourceAddress, destinationAddress);
        return message;
    }

    public static Message buildAddrResponseMessage(int sourceAddress, int destinationAddress, List<Integer> addresses) {
        Message message = new Message(MessageType.GETADDR_RES, sourceAddress, destinationAddress);
        message.addresses = addresses;
        return message;
    }

    public static Message buildTransactionMessage(int sourceAddress, int destinationAddress, Transaction tx) {
        Message message = new Message(MessageType.TRANSACTION, sourceAddress, destinationAddress);
        message.transaction = tx;
        return message;
    }

    public static Message buildBlockMessage(int sourceAddress, int destinationAddress, Block block) {
        Message message = new Message(MessageType.BLOCK, sourceAddress, destinationAddress);
        message.block = block;
        return message;
    }

    public static Message buildBlockHashMessage(int sourceAddress, int destinationAddress, BlockHashMessageData data) {
        Message message = new Message(MessageType.BLOCK_HASH, sourceAddress, destinationAddress);
        message.voter = data.voter;
        message.round = data.round;
        message.step = data.step;
        message.prevBlockHash = data.prevBlockHash;
        message.blockHash = data.blockHash;
        return message;
    }

    public static Message buildBlockChainMessage(int sourceAddress, int destinationAddress) {
        Message message = new Message(MessageType.BLOCK_CHAIN, sourceAddress, destinationAddress);
        return message;
    }

    public static Message buildBlockChainResMessage(int sourceAddress, int destinationAddress, List<Block> data) {
        Message message = new Message(MessageType.BLOCK_CHAIN_RES, sourceAddress, destinationAddress);
        message.blockchain = data;
        return message;
    }

    public static Message buildBlockReqMessage(int sourceAddress, int destinationAddress, String blockHash) {
        Message message = new Message(MessageType.BLOCK_REQUEST, sourceAddress, destinationAddress);
        message.blockHash = blockHash;
        return message;
    }

    public static Message buildBlockReqResMessage(int sourceAddress, int destinationAddress, Block block) {
        Message message = new Message(MessageType.BLOCK_REQUEST_RES, sourceAddress, destinationAddress);
        message.block = block;
        return message;
    }
}