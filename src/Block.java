import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Date;

public class Block implements Serializable {
    public final Transaction[] transactions;
    public final int round;
    public final int priority;
    public final int seed;
    public final Timestamp timestamp;
    public final String prevBlockHash;

    private Block() {
        this.transactions = null;
        this.round = -1;
        this.priority = -1;
        this.seed = -1;
        this.timestamp = null;
        this.prevBlockHash = null;
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

    public static String getHash(Block block) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String blockString =
                    Integer.toString(block.transactions[0].amount) +
                            Integer.toString(block.transactions[1].amount) +
                            block.prevBlockHash + Integer.toString(block.round);
            byte[] digest = md.digest(blockString.getBytes());
            BigInteger i = new BigInteger(1, digest);
            String hash = i.toString(16);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Block getEmptyBlock() {
        return new Block();
    }

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