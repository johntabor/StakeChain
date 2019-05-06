public class Constants {
    public static final int BA_STAR_MAX_STEPS = 6;
    /**
     * Number of transactions per a block
     */
    public static final int BLOCK_SIZE = 2;
    /**
     * Size of the Algorand committee
     */
    public static final int COMMITTEE_SIZE = 2;
    /**
     * Percentage of the committee needed for a block hash to win a vote
     */
    public static final float COMMITTEE_SIZE_FACTOR = (float)(2/3);
    /**
     * Total currency supply
     */
    public static final int CURRENCY_SUPPLY = 1000;
    /**
     * Initial Algorand round
     */
    public static final int INITIAL_ROUND = 0;
    /**
     * Initial seed for Algorand
     */
    public static final int INITIAL_SEED  = 0;
    /**
     * Number of block proposers for a round
     */
    public static final int NUM_PROPOSERS = 2;
    /**
     * Port for the seed server
     */
    public static final int SEED_PORT = 9000;

    public static final double VOTING_TIMEOUT = 2500;
    /**
     * Client will wait 5 seconds for proposed blocks
     */
    public static final int PROPOSAL_TIMEOUT = 5000;
}