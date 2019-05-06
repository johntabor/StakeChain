import java.sql.Timestamp;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Responsible for managing Blockchain ledger state
 */
public class LedgerManager {
    /**
     * First block in the ledger. Every client has the same genesis block
     * that serves as the root of the chain
     */
    private static Block genesis;
    /**
     * The actual ledger
     */
    private static LinkedList<Block> ledger = new LinkedList<>();
    /**
     * Read and write lock for the ledger
     */
    public static final ReentrantLock ledgerLock = new ReentrantLock();

    private static Set<Integer> seenTransactions = new HashSet<>();

    /**
     * Initializes the ledger by creating the genesis block
     */
    public static void start() {
        genesis = buildGenesis();
        ledger.add(genesis);
    }

    public static Block buildGenesis() {
        Transaction[] transactions = new Transaction[Constants.BLOCK_SIZE];
        for(int i = 0; i < Constants.BLOCK_SIZE; i++) {
            String recipient = "address" + i;
            transactions[i] = new Transaction(
                    i, "genesis", recipient,
                    Constants.CURRENCY_SUPPLY / Constants.BLOCK_SIZE);
        }
        return new Block(transactions, -1, -1, "");
    }

    /*
    public static boolean verifyTransaction(Transaction tx) {
        seenPool.add(tx.id);
        int balance = 0;
        for(Block block : ledger) {
            for(Transaction blockTx : block.transactions) {
                if (tx.senderAddress.compareTo(blockTx.recipientAddress) == 0) {
                    balance += blockTx.amount;
                } else if (tx.senderAddress.compareTo(blockTx.senderAddress) == 0) {
                    balance -= blockTx.amount;
                }
            }
        }

        if (balance >= tx.amount) {
            return true;
        }
        return false;
    }*/

    public static boolean seenTransaction(Transaction tx) {
        if (!seenTransactions.contains(tx.id)) {
            seenTransactions.add(tx.id);
            return false;
        }
        return true;
    }


    private static void addBlock(Block block) {
        // validate eventually
        //bloch.
        //block.hash = blockHash(block);
        //ledger.add(block);
    }

    public static boolean validateBlock(Block block) {
        return true;
    }

    /**
     * Returns the first block in the ledger (genesis)
     * @return genesis block
     */
    public static Block getGenesis() {
        return genesis;
    }

    /**
     * Retrieves the most recent block in the ledger
     * @return most recent block
     */
    public static Block getLastBlock() {
        Block lastBlock = null;
        ledgerLock.lock();
        try {
            lastBlock = ledger.getLast();
        } finally {
            ledgerLock.unlock();
        }
        return lastBlock;
    }
}

