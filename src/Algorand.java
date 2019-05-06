import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Thread that runs the Algorand consensus algorithm
 */
class AlgorandRunnable implements Runnable {
    @Override
    public void run() {
        Algorand.runAlgorand();
    }
}

public class Algorand {
    /**
     * Current round
     */
    public static int round = Constants.INITIAL_ROUND;
    /**
     * Keeps track of the highest priority proposal for the round
     */
    public static int highestPriorityProposal = -1;
    /**
     * Lock for highestPriorityProposal
     */
    public static ReentrantLock highestPriorityProposalLock = new ReentrantLock();
    /**
     * Stores votes for block hashes for current step of BA*
     */
    //public static Map<String, Integer> votes = new HashMap<>();

    /**
     * Main method for Algorand. Has two stages:
     * 1. retrieve highest priority proposed block
     * 2. run BA* on this block
     */
    public static void runAlgorand() {
        while(true) {
            System.out.println("Round: " + round);
            Block highestPriorityBlock = runProposalStage();
            System.out.println("highestPriorityBlockHash: " + Block.getHash(highestPriorityBlock));
            System.out.println("highestPriorityBlock:\n" + highestPriorityBlock.toString());
            runBAStar(highestPriorityBlock);
            round++;
        }
    }

    public static Block runProposalStage() {
        /* Wait until client either has transactions or block proposals
            to process. Check every second */
        while(ConnectionManager.proposedBlockQueue.isEmpty()
                && ConnectionManager.transactionQueue.size() < Constants.BLOCK_SIZE) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /* Attempt to propose a block */
        Block proposedBlock = null;
        if (ConnectionManager.transactionQueue.size() >= Constants.BLOCK_SIZE) {
            int priority = sortition(1000);
            if (priority > 0) {
                proposedBlock = proposeBlock(priority);
            }
        }
        /* Wait PROPOSAL_TIMEOUT seconds for other block proposals to roll in */
        try {
            Thread.sleep(Constants.PROPOSAL_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        /*
         * Obtain the highest priority block from the locally proposed block (if exists)
         * and proposals received from other nodes
         */
        Block highestPriorityBlock = Block.getEmptyBlock();
        if (proposedBlock != null) {
            highestPriorityBlock = proposedBlock;
            highestPriorityProposalLock.lock();
            try {
                highestPriorityProposal = proposedBlock.priority;

            } finally {
                highestPriorityProposalLock.unlock();
            }
        }

        while(!ConnectionManager.proposedBlockQueue.isEmpty()) {
            try {
                Block block = ConnectionManager.proposedBlockQueue.peek();
                if (block.round <= Algorand.round) {
                    block = ConnectionManager.proposedBlockQueue.take();
                    if (block.round == Algorand.round && block.priority > highestPriorityBlock.priority) {
                        highestPriorityBlock = block;
                        highestPriorityProposalLock.lock();
                        try {
                            highestPriorityProposal = block.priority;
                        } finally {
                            highestPriorityProposalLock.unlock();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /* If the highest priority block is valid, use it. If not, use an empty block*/
        if (LedgerManager.validateBlock(highestPriorityBlock)) {
            return highestPriorityBlock;
        } else {
            return Block.getEmptyBlock();
        }
    }

    /**
     * Proposes the block to peers
     * @return the proposed block
     */
    public static Block proposeBlock(int priority) {
        Transaction[] transactions = new Transaction[Constants.BLOCK_SIZE];
        try {
            transactions[0] = ConnectionManager.transactionQueue.take();
            transactions[1] = ConnectionManager.transactionQueue.take();
            Block block = new Block(transactions, Algorand.round, priority,"");
            System.out.println("proposing block with hash: " + Block.getHash(block));
            ConnectionManager.gossip(Message.MessageType.BLOCK, block, null);
            return block;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Flips a weighted coin in order to decide if the user becomes a committee member
     * Simulation of sortition
     *
     * @param balance - balance of the user
     * @return true if chosen as a committee member; false if not
     */
    public static int sortition(int balance) {
        /*double chance = (double) balance / (double) Constants.CURRENCY_SUPPLY;
        double flip = Math.random();
        if (flip <= chance) {
            return true;
        }
        return false;*/
        return balance;
    }

    public static void committeeVote(int step, String blockHash) {
        // temporarily set to 1000 so always picked. will change
        int priority = sortition(1000);
        if (priority > 0) {
            // have it pass a blockHashMessageObject that contains the fields that need to be passed
            String prevBlockHash = Block.getHash(LedgerManager.getLastBlock());
            BlockHashMessageData data = new BlockHashMessageData(round, step, prevBlockHash, blockHash);
            ConnectionManager.gossip(Message.MessageType.BLOCK_HASH, data, null);
        }
    }

    public static void runBAStar(Block block) {
        System.out.println("block hash to send: " + Block.getHash(block));
        String blockHash = reduction(Block.getHash(block));
        String blockHashStar = binaryBAStar(blockHash);
        String r = countVotes(Constants.VOTING_TIMEOUT);
        if (blockHashStar.compareTo(r) == 0) {
            // final consensus
            //return BlockOfHash(hblockStar);
        } else {
            // tentative consensus
            //return BlockOfHash(hblockStar);
        }
    }

    /**
     * Attempts to reach agreement on the hash of the proposed block
     * Performs two voting rounds
     * @param blockHash - hash to agree on
     * @return agreed  upon hash
     */
    public static String reduction(String blockHash) {
        // step 1: gossip block hash and get votes
        committeeVote(0, blockHash);
        String popularHash = countVotes(Constants.VOTING_TIMEOUT);
        // step 2: re-gossip the popular block hash and get votes
        String emptyHash = Block.getHash(Block.getEmptyBlock());
        if (popularHash.compareTo("TIMEOUT") == 0) {
            committeeVote(1, emptyHash);
        } else {
            committeeVote(1, popularHash);
        }
        popularHash = countVotes(Constants.VOTING_TIMEOUT);
        if (popularHash.compareTo("TIMEOUT") == 0) {
            return emptyHash;
        }
        return popularHash;
    }

    // need to make sure we are only working with messages from current round and step
    public static String countVotes(double timeout) {
        long start = System.currentTimeMillis();
        Map<String, Integer> votes = new HashMap<>();
        Set<Integer> voters = new HashSet<>();
        while(true) {
            try {
                if (ConnectionManager.blockHashQueue.isEmpty()) {
                    double elapsedTime = (System.currentTimeMillis() - start) / 1000F;
                    if (elapsedTime >= timeout) {
                        return "TIMEOUT";
                    }
                } else {
                    Message message = ConnectionManager.blockHashQueue.take();
                    ProcessMessageObject values = processMessage(message);
                    if (voters.contains(message.sourceAddress)) {
                        continue;
                    }
                    voters.add(message.sourceAddress);
                    String blockHash = message.blockHash;
                    System.out.println("block hash received: " + blockHash);
                    if (votes.containsKey(blockHash)) {
                        votes.put(blockHash, votes.get(blockHash)+1);
                    } else {
                        votes.put(blockHash, 1);
                    }
                    if (votes.get(blockHash) > (Constants.COMMITTEE_SIZE * Constants.COMMITTEE_SIZE_FACTOR)) {
                        return blockHash;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static ProcessMessageObject processMessage(Message message) {
        // verify signature

        // see if
        String ledgerLastBlockHash = Block.getHash(LedgerManager.getLastBlock());
        if (ledgerLastBlockHash.compareTo(message.prevBlockHash) != 0) {
            return new ProcessMessageObject(0, "");
        }

        // verify the sorthash to get the number of votes
        // manually set votes to 1 right now
        return new ProcessMessageObject(1, message.blockHash);
    }

    static class ProcessMessageObject {
        public final int votes;
        public final String blockHash;

        ProcessMessageObject(int votes, String blockHash) {
            this.votes = votes;
            this.blockHash = blockHash;
        }
    }

    private static String binaryBAStar(String blockHash) {
        int step = 1;
        String r = blockHash;
        String emptyHash = Block.getHash(Block.getEmptyBlock());
        while(step < Constants.BA_STAR_MAX_STEPS) {
            committeeVote(step, r);
            r = countVotes(Constants.VOTING_TIMEOUT);
            if (r.compareTo("TIMEOUT") == 0) {
                r = blockHash;
            } else if (r.compareTo(emptyHash) != 0) {
                for (int i = step; i <= step + 3; i++) {
                    committeeVote(step, r);
                }
                if (step == 1) {
                    committeeVote(step, r);
                }
                return r;
            }
            step++;

            committeeVote(step, r);
            r = countVotes(Constants.VOTING_TIMEOUT);
            if (r.compareTo("TIMEOUT") == 0) {
                r = emptyHash;
            } else if (r.compareTo(emptyHash) == 0) {
                for (int i = step; i <= step + 3; i++) {
                    committeeVote(step, r);
                }
                if (step == 1) {
                    committeeVote(step, r);
                }
                return r;
            }
            step++;
        }
        return Block.getHash(Block.getEmptyBlock());
    }
}
