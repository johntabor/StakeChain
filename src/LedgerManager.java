import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
    //public static final ReentrantLock ledgerLock = new ReentrantLock();

    /**
     * Initializes the ledger by creating the genesis block
     */
    public synchronized static void start() {
        genesis = buildGenesis();
        ledger.add(genesis);
        System.out.print("after just adding genesis in start(): " + ledger.size());
    }

    public synchronized static Block buildGenesis() {
        Transaction[] transactions = new Transaction[Constants.BLOCK_SIZE];
        for(int i = 0; i < Constants.BLOCK_SIZE; i++) {
            String recipient = "address" + i;
            transactions[i] = new Transaction(
                    i, "genesis", recipient,
                    Constants.CURRENCY_SUPPLY / Constants.BLOCK_SIZE);
        }
        return new Block(transactions, -1, -1, "");
    }

    /**
     * Check the ledger and see if a transaction is valid
     * @param tx - transaction to validate
     * @return true if valid; false otherwise
     */
    public synchronized static boolean validateTransaction(Transaction tx) {
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
    }

    /**
     * Add a block to the ledger
     * @param block - block to add to the ledger
     */
    public synchronized static boolean addBlock(Block block) {
        if (block.prevBlockHash.compareTo(Block.getHash(getLastBlock())) != 0) {
            System.out.println("Prevs don't match up");
            return false;
        }

        for(Transaction tx : block.transactions) {
            if (!validateTransaction(tx)) {
                System.out.println("A transaction is invalid");
                return false;
            }
        }

        ledger.add(block);
        return true;
        /*
        ledgerLock.lock();
        try {
            if (block.prevBlockHash.compareTo(Block.getHash(getLastBlock())) != 0) {
                System.out.println("Prevs don't match up");
                return;
            }

            for(Transaction tx : block.transactions) {
                if (!validateTransaction(tx)) {
                    System.out.println("A transaction is invalid");
                    return;
                }
             }

             ledger.add(block);
        } finally {
            ledgerLock.unlock();
        }*/
    }

    public synchronized static boolean validateBlock(Block block) {
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
     * Retrieves block with specified blockHash
     * @param blockHash
     * @return requested block
     */
    public synchronized static Block getBlock(String blockHash) {
        for(Block block : ledger) {
            if (blockHash.compareTo(Block.getHash(block)) == 0) {
                return block;
            }
        }
        return null;
    }

    /**
     * Retrieves block at position num (aka height) in ledger
     * @param height - position
     * @return
     */
    public synchronized static Block getBlock(int height) {
        if (height >= 0 && height < ledger.size()) {
            return ledger.get(height);
        }
        /*ledgerLock.lock();
        try {
            if (num >= 0 && num < ledger.size()) {
                return ledger.get(num);
            }
        } finally {
            ledgerLock.unlock();
        }*/
        return null;
    }

    /**
     * Retrieves the most recent block in the ledger
     * @return most recent block
     */
    public synchronized static Block getLastBlock() {
        //lastBlock = ledger.getLast();
        //Block lastBlock = null;
        /*ledgerLock.lock();
        try {
            lastBlock = ledger.getLast();
        } finally {
            ledgerLock.unlock();
        }*/
        return ledger.getLast();
    }

    public synchronized static boolean setLedger(List<Block> data) {
        for(Block block : data) {
            if (!addBlock(block)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Obtains the entire ledger
     * @return the ledger
     */
    public synchronized static List<Block> getLedger() {
        return ledger;
    }

    public synchronized static int getLedgerSize() {
        return ledger.size();
    }
}

