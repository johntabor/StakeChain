import java.io.Serializable;
import java.util.List;

/**
 * Holds data specific to a block hash message
 */
class BlockHashMessageData {
    public final int round;
    public final int step;
    public final String prevBlockHash;
    public final String blockHash;

    public BlockHashMessageData(int round, int step, String prevBlockHash, String blockHash) {
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
        BLOCK_HASH
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
     * Disclaimer: only one of the four fields to follow may
     * be set for any given Message
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

    public int round;
    public int step;

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
        message.blockHash = data.blockHash;
        message.prevBlockHash = data.prevBlockHash;
        message.round = data.round;
        message.step = data.step;
        return message;
    }
}