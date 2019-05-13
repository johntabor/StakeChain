import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Special steps in Algorand
 */
class Steps {
    public static final int REDUCTION_ONE = -1;
    public static final int REDUCTION_TWO = 0;
    public static final int FINAL = 1000;
}

/**
 * Types of consensus in Algorand
 */
enum Consensus {
    FINAL,
    TENTATIVE
}

/**
 * The Algorand Consensus Algorithm
 */
public class Algorand implements Runnable {
    /**
     * Current round
     */
    public static int round = Constants.INITIAL_ROUND;
    /**
     * Block proposals seen. Helps the node track down a block by its hash.
     */
    private static Map<String, Block> blockProposals = new HashMap<>();

    private static Set<Block> tentativeBlocks = new HashSet<>();

    @Override
    public void run() {
        runAlgorand();
    }

    /**
     * Main method for Algorand. Two stages:
     * 1. retrieve highest priority proposed block
     * 2. run BA* on this block
     */
    public static void runAlgorand() {
        while(true) {
            System.out.println("Round: " + round);
            Block highestPriorityBlock = runProposalStage();
            System.out.println("Highest block proposal: " + Block.getHash(highestPriorityBlock));
            BAStarResult result = runBAStar(highestPriorityBlock);
            Block winner = result.block;
            String winningHash = Block.getHash(winner);
            String emptyHash = Block.getHash(Block.getEmptyBlock());
            System.out.println("Hash of winning block: " + Block.getHash(winner));
            System.out.println("Hash of empty block: " + Block.getHash(Block.getEmptyBlock()));
            System.out.println("Consensus type: " + result.consensus);

            if (result.consensus == Consensus.FINAL) {
                if (winningHash.compareTo(emptyHash) != 0) {
                    // see if there is a tentative block that this
                    // block references
                    // might have to check several back
                    for(Block tentative : tentativeBlocks) {
                        String tentativeHash = Block.getHash(tentative);
                        if (winner.prevBlockHash.compareTo(tentativeHash) == 0) {
                            LedgerManager.addBlock(tentative);
                            tentativeBlocks.remove(tentative);
                            break;
                        }
                    }
                    LedgerManager.addBlock(winner);
                }
            } else {
                if (winningHash.compareTo(emptyHash) != 0) {
                    tentativeBlocks.add(winner);
                }
            }

            /* remove block proposals from just completed round */
            Set<Block> futureProposals = new HashSet<>();
            while(!ConnectionManager.proposedBlockQueue.isEmpty()) {
                try {
                    Block block = ConnectionManager.proposedBlockQueue.take();
                    if (block.round > round) {
                        futureProposals.add(block);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ConnectionManager.proposedBlockQueue.addAll(futureProposals);

            /* remove votes from just completed round */
            Set<Message> futureVotes = new HashSet<>();
            while(!ConnectionManager.blockHashQueue.isEmpty()) {
                try {
                    Message message = ConnectionManager.blockHashQueue.take();
                    if (message.round > round) {
                        futureVotes.add(message);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ConnectionManager.blockHashQueue.addAll(futureVotes);
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
                // exit
            }
        }

        //System.out.println("Attempting to propose a block...");
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

        /* Wait PROPOSAL_TIMEOUT milliseconds for other block proposals to roll in */
        int i = 0;
        while(i < Constants.PROPOSAL_TIMEOUT) {
            try {
                System.out.println("...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i+=1000;
        }

        /* Obtain the highest priority block from the locally proposed block (if exists)
         * and proposals received from other nodes */
        Block highestPriorityBlock = Block.getEmptyBlock();
        if (proposedBlock != null) {
            highestPriorityBlock = proposedBlock;
        }

        /* Check proposals received from others */
        Set<Block> futureBlockProposals = new HashSet<>();
        while(!ConnectionManager.proposedBlockQueue.isEmpty()) {
            try {
                Block block = ConnectionManager.proposedBlockQueue.take();
                /* proposal for next round, so keep it */
                if (block.round > Algorand.round) {
                    futureBlockProposals.add(block);
                /* proposal for this round */
                } else if (block.round == Algorand.round && block.priority > highestPriorityBlock.priority) {
                    blockProposals.put(Block.getHash(block), block);
                    highestPriorityBlock = block;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        /* add back in proposals for future rounds */
        ConnectionManager.proposedBlockQueue.addAll(futureBlockProposals);

        /* If the highest priority block is valid, use it. If not, use an empty block*/
        if (LedgerManager.validateBlock(highestPriorityBlock)) {
            return highestPriorityBlock;
        } else {
            return Block.getEmptyBlock();
        }
    }

    /**
     * Proposes a block to peers
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

            //System.out.println("Proposing block with hash: " + Block.getHash(block));
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
        //System.out.println("VOTING IN STEP " + step + ": committeeVote()");
        // temporarily set to 1000 so always picked. will change
        int priority = sortition(1000);
        if (priority > 0) { /* on committee, so vote! */
            String prevBlockHash = Block.getHash(LedgerManager.getLastBlock());
            BlockHashMessageData data = new BlockHashMessageData(ConnectionManager.inboundPort, round, step, prevBlockHash, blockHash);
            ConnectionManager.gossip(Message.MessageType.BLOCK_HASH, data, null);
            ConnectionManager.blockHashQueue.add(
                    Message.buildBlockHashMessage(ConnectionManager.inboundPort, ConnectionManager.inboundPort, data));
        }
    }

    static class BAStarResult {
        public final Consensus consensus;
        public final Block block;

        public BAStarResult(Consensus consensus, Block block) {
            this.consensus = consensus;
            this.block = block;
        }
    }

    public static BAStarResult runBAStar(Block block) {
        //System.out.println("Starting BA*...");
        String blockHash = reduction(Block.getHash(block));
        //System.out.println("RESULT OF REDUCTION: " + blockHash);
        String blockHashStar = binaryBAStar(blockHash);
        //System.out.println("blockHashStar returned from binaryBASTAR: " + blockHashStar);
        String r = countVotes(Steps.FINAL, Constants.VOTING_TIMEOUT);
        //System.out.println("value of r in runBAStar: " + r);
        String emptyHash = Block.getHash(Block.getEmptyBlock());
        if (blockHashStar.compareTo(r) == 0) {
            if (blockHashStar.compareTo(emptyHash) == 0) {
                return new BAStarResult(Consensus.FINAL, Block.getEmptyBlock());
            }
            return new BAStarResult(Consensus.FINAL, getBlockFromHash(blockHashStar));
        } else {
            if (blockHashStar.compareTo(emptyHash) == 0) {
                return new BAStarResult(Consensus.TENTATIVE, Block.getEmptyBlock());
            }
            return new BAStarResult(Consensus.TENTATIVE, getBlockFromHash(blockHashStar));
        }
    }

    /**
     * Obtains the winning block from either local storage or the network
     * @param blockHash - want to get the block of this hash
     * @return the requested block
     */
    private static Block getBlockFromHash(String blockHash) {
        if (blockProposals.containsKey(blockHash)) {
            return blockProposals.get(blockHash);
        } else {
            ConnectionManager.gossip(Message.MessageType.BLOCK_REQUEST, blockHash, null);
            while(true) {
                try {
                    Block block = ConnectionManager.requestedBlockQueue.take();
                    if (blockHash.compareTo(Block.getHash(block)) == 0) {
                        return block;
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
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

    public static String countVotes(int step, double timeout) {
        long start = System.currentTimeMillis();
        List<Message> futureVotes = new LinkedList<>();
        Map<String, Integer> votes = new HashMap<>();
        Set<Integer> voters = new HashSet<>();
        String result; // a blockHash or TIMEOUT
        while(true) {
            try {
                if (ConnectionManager.blockHashQueue.isEmpty()) {
                    double elapsedTime = (System.currentTimeMillis() - start);
                    if (elapsedTime >= timeout) {
                        result = "TIMEOUT";
                        break;
                    }
                } else {
                    Message message = ConnectionManager.blockHashQueue.take();
                    //ProcessMessageObject data = processMessage(message);

                    /* future votes to store for later */
                    if (message.round > round || (message.round == round && message.step > step)) {
                        futureVotes.add(message);
                    /* votes for this round and step */
                    } else if (message.round == round && message.step == step) {
                        if (!voters.contains(message.sourceAddress)) {
                            voters.add(message.sourceAddress);
                            String blockHash = message.blockHash;
                            if (votes.containsKey(blockHash)) {
                                votes.put(blockHash, votes.get(blockHash)+1);
                            } else {
                                votes.put(blockHash, 1);
                            }

                            int majorityVotes = (int) Math.round(Constants.COMMITTEE_SIZE * Constants.COMMITTEE_SIZE_FACTOR);
                            if (votes.get(blockHash) >= majorityVotes) {
                                result = blockHash;
                                break;
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        ConnectionManager.blockHashQueue.addAll(futureVotes);
        return result;
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
        int step = 1;
        String r = blockHash;
        String emptyHash = Block.getHash(Block.getEmptyBlock());
        while(step < Constants.BA_STAR_MAX_STEPS) {
            committeeVote(step, r);
            r = countVotes(step, Constants.VOTING_TIMEOUT);
            if (r.compareTo("TIMEOUT") == 0) {
                r = blockHash;
            } else if (r.compareTo(emptyHash) != 0) {
                /* vote in the next three steps */
                for (int i = step + 1; i <= (step + 3); i++) {
                    committeeVote(i, r);
                }
                /* vote in FINAL consensus round */
                if (step == 1) {
                    committeeVote(Steps.FINAL, r);
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

    /**
     * Calculates the unique hash of a vote
     * @param message - message that represents the vote
     * @return hash of the vote
     */
    public static String getVoteHash(Message message) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String voteString =
                    Integer.toString(message.voter) +
                    message.round +
                    Integer.toString(message.step) +
                    message.prevBlockHash +
                    message.blockHash;
            byte[] digest = md.digest(voteString.getBytes());
            BigInteger i = new BigInteger(1, digest);
            String hash = i.toString(16);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
}
