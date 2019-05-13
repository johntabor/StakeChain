public class Constants {
    public static final int BA_STAR_MAX_STEPS = 6;
    /**
     * Number of transactions per a block
     */
    public static final int BLOCK_SIZE = 10;
    /**
     * Size of the Algorand committee
     */
    public static final int COMMITTEE_SIZE = 3;
    /**
     * Percentage of the committee needed for a block hash to win a vote
     */
    public static final double COMMITTEE_SIZE_FACTOR = 0.66;
    /**
     * Total currency supply
     */
    public static final int CURRENCY_SUPPLY = 10000;
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

    public static final double VOTING_TIMEOUT = 2000;
    /**
     * Client will wait 5 seconds for proposed blocks
     */
    public static final int PROPOSAL_TIMEOUT = 3000;
}