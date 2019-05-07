
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

class Steps {
    public static final int REDUCTION_ONE = -1;
    public static final int REDUCTION_TWO = 0;
    public static final int FINAL = 1000;
}

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
     * Block proposals seen. Helps the node track down a block by its hash.
     */
    private static Map<String, Block> blockProposals = new HashMap<>();

    /**
     * Main method for Algorand. Has two stages:
     * 1. retrieve highest priority proposed block
     * 2. run BA* on this block
     */
    public static void runAlgorand() {
        while(true) {
            if (round == 1) {
                return;
            }
            System.out.println("Round: " + round);
            Block highestPriorityBlock = runProposalStage();
            System.out.println("Hash of highest priority block proposal: " + Block.getHash(highestPriorityBlock));
            Block winner = runBAStar(highestPriorityBlock);
            System.out.println("Hash of winning block: " + Block.getHash(winner));
            System.out.println("hash of empty block: " + Block.getHash(Block.getEmptyBlock()));
            //System.out.println("block to string: ");
            //System.out.println(winner.toString());
            System.out.println("done");
            // add the block and remove it's transactions from the transaction pool
            //LedgerManager.addBlock();
            round++;
        }
    }

    public static Block runProposalStage() {
        /* Wait until client either has transactions or block proposals
            to process. Check every second */
        // need to improve this
        // could be proposals from last round sitting around
        //
        while(ConnectionManager.proposedBlockQueue.isEmpty()
                && ConnectionManager.transactionQueue.size() < Constants.BLOCK_SIZE) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Attempting to propose a block...");
        /* Attempt to propose a block */
        Block proposedBlock = null;
        if (ConnectionManager.transactionQueue.size() >= Constants.BLOCK_SIZE) {
            int priority = sortition(1000);
            if (priority > 0) {
                // CHANGE THIS LATER (random priority helps get the agreement process going)
                Random r = new Random();
                priority = r.nextInt(1000);
                proposedBlock = proposeBlock(priority);
            }
        }
        System.out.println("Waiting for block proposals to roll in...");
        /* Wait PROPOSAL_TIMEOUT seconds for other block proposals to roll in */
        int i = 0;
        while(i < 5) {
            try {
                System.out.println("...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i++;
        }
        /*
        try {
            Thread.sleep(Constants.PROPOSAL_TIMEOUT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
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
        System.out.println("Processing received block proposals...");
        while(!ConnectionManager.proposedBlockQueue.isEmpty()) {
            try {
                Block block = ConnectionManager.proposedBlockQueue.peek();
                if (block.round <= Algorand.round) {
                    block = ConnectionManager.proposedBlockQueue.take();
                    if (block.round == Algorand.round && block.priority > highestPriorityBlock.priority) {
                        blockProposals.put(Block.getHash(block), block);
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
            for(int i = 0; i < Constants.BLOCK_SIZE; i++) {
                transactions[i] = ConnectionManager.transactionQueue.take();
            }

            String prevBlockHash = Block.getHash(LedgerManager.getLastBlock());
            Block block = new Block(transactions, Algorand.round, priority, prevBlockHash);
            blockProposals.put(Block.getHash(block), block);

            System.out.println("Proposing block with hash: " + Block.getHash(block));
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
        System.out.println("starting committeeVote on block hash " + blockHash + "...");
        // temporarily set to 1000 so always picked. will change
        int priority = sortition(1000);
        if (priority > 0) {
            String prevBlockHash = Block.getHash(LedgerManager.getLastBlock());
            BlockHashMessageData data = new BlockHashMessageData(round, step, prevBlockHash, blockHash);
            ConnectionManager.gossip(Message.MessageType.BLOCK_HASH, data, null);
        }
    }

    public static Block runBAStar(Block block) {
        System.out.println("Starting BA*...");
        String blockHash = reduction(Block.getHash(block));
        String blockHashStar = binaryBAStar(blockHash);
        System.out.println("blockHashStar: " + blockHashStar);
        String r = countVotes(Steps.FINAL, Constants.VOTING_TIMEOUT);

        // get the block for it
        if (blockProposals.containsKey(blockHashStar)) {
            System.out.println("found it");
            return blockProposals.get(blockHashStar);
        } else {
            System.out.println("couldnt find it. need to ask a peer for it");
            return block;
        }

        /*if (blockHashStar.compareTo(r) == 0) {
            // final consensus
            System.out.println("Reached final consensus");
            return block;
            //return BlockOfHash(hblockStar);
        } else {
            // tentative consensus
            System.out.println("reached tentative consensus");
            return block;
            //return BlockOfHash(hblockStar);
        }*/
    }

    /**
     * Attempts to reach agreement on the hash of the proposed block
     * Performs two voting rounds
     * @param blockHash - hash to agree on
     * @return agreed  upon hash
     */
    public static String reduction(String blockHash) {
        System.out.println("Starting reduction...");
        // step 1: gossip block hash and get votes
        committeeVote(Steps.REDUCTION_ONE, blockHash);
        String popularHash = countVotes(Steps.REDUCTION_ONE, Constants.VOTING_TIMEOUT);
        // step 2: re-gossip the popular block hash and get votes
        String emptyHash = Block.getHash(Block.getEmptyBlock());
        if (popularHash.compareTo("TIMEOUT") == 0) {
            committeeVote(Steps.REDUCTION_TWO, emptyHash);
        } else {
            committeeVote(Steps.REDUCTION_TWO, popularHash);
        }
        popularHash = countVotes(Steps.REDUCTION_TWO, Constants.VOTING_TIMEOUT);
        if (popularHash.compareTo("TIMEOUT") == 0) {
            return emptyHash;
        }
        return popularHash;
    }

    // need to make sure we are only working with messages from current round and step
    public static String countVotes(int step, double timeout) {
        System.out.println("Counting votes...");
        long start = System.currentTimeMillis();
        Map<String, Integer> votes = new HashMap<>();
        Set<Integer> voters = new HashSet<>();
        while(true) {
            try {
                if (ConnectionManager.blockHashQueue.isEmpty()) {
                    double elapsedTime = (System.currentTimeMillis() - start);
                    if (elapsedTime >= timeout) {
                        return "TIMEOUT";
                    }
                } else {
                    // will lose messages from later rounds this way
                    Message message = ConnectionManager.blockHashQueue.take();
                    ProcessMessageObject data = processMessage(message);
                    if (message.round < round || message.step < step ||
                        voters.contains(message.sourceAddress) ||
                        data.votes == 0) {
                        continue; // discard the message
                    }

                    voters.add(message.sourceAddress);
                    String blockHash = message.blockHash;
                    if (votes.containsKey(blockHash)) {
                        votes.put(blockHash, votes.get(blockHash)+1);
                    } else {
                        votes.put(blockHash, 1);
                    }
                    if (votes.get(blockHash) >= (Constants.COMMITTEE_SIZE * Constants.COMMITTEE_SIZE_FACTOR)) {
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
        /*String ledgerLastBlockHash = Block.getHash(LedgerManager.getLastBlock());
        if (ledgerLastBlockHash.compareTo(message.prevBlockHash) != 0) {
            return new ProcessMessageObject(0, "");
        }*/

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
        System.out.println("Starting binaryBAStar");
        int step = 1;
        String r = blockHash;
        String emptyHash = Block.getHash(Block.getEmptyBlock());
        while(step < Constants.BA_STAR_MAX_STEPS) {
            committeeVote(step, r);
            r = countVotes(step, Constants.VOTING_TIMEOUT);
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
            r = countVotes(step, Constants.VOTING_TIMEOUT);
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
