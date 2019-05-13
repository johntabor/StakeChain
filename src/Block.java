import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * Represents a block of transactions in the blockchain
 */
public class Block implements Serializable {
    public final Transaction[] transactions;
    public final int round;
    public final int priority;
    public final int seed;
    public final Timestamp timestamp;
    public final String prevBlockHash;

    /* Empty block constructor */
    private Block() {
        this.transactions = new Transaction[Constants.BLOCK_SIZE];
        for(int i = 0; i < Constants.BLOCK_SIZE; i++) {
            this.transactions[i] = new Transaction(-1, "", "", -1);

        }
        this.round = -1;
        this.priority = -1;
        this.seed = -1;
        this.timestamp = null;
        this.prevBlockHash = "";
    }

    public Block(Transaction[] transactions, int round, int priority, String prevBlockHash) {
        this.transactions = new Transaction[Constants.BLOCK_SIZE];
        for(int i = 0; i < Constants.BLOCK_SIZE; i++) {
            this.transactions[i] = transactions[i];
        }
        this.round = round;
        this.priority = priority;
        this.seed = 1;
        this.timestamp = new Timestamp(new Date().getTime());
        this.prevBlockHash = prevBlockHash;
    }

    /**
     * Computes the hash of a block
     * @param block - block to compute hash of
     * @return hash of the block
     */
    public static String getHash(Block block) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            StringBuilder transactionString = new StringBuilder();
            for(int i = 0; i < Constants.BLOCK_SIZE; i++) {
                transactionString.append(block.transactions[i].toString());
            }
            String blockString =
                    transactionString.toString() +
                    Integer.toString(block.round) +
                    block.priority +
                    block.prevBlockHash;
            byte[] digest = md.digest(blockString.getBytes());
            BigInteger i = new BigInteger(1, digest);
            String hash = i.toString(16);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Retrieves the empty block
     * @return empty block
     */
    public static Block getEmptyBlock() {
        return new Block();
    }

    /**
     * Prints out block information in a readable format
     * @return block information
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < Constants.BLOCK_SIZE; i++) {
            sb.append(this.transactions[i].toString());
        }
        sb.append("round: " + this.round + "\n");
        sb.append("priority: " + this.priority + "\n");
        return sb.toString();
    }
}