public class Main {
    /*
    public static String adjustTo64(String s) {
        switch(s.length()) {
            case 62: return "00" + s;
            case 63: return "0" + s;
            case 64: return s;
            default:
                throw new IllegalArgumentException("not a valid key: " + s);
        }
    }

    public static boolean verifyVRF(byte[] hash, byte[] proof, int seedRoleSmush) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] proofHash = sha256.digest(proof);
        return Arrays.equals(hash, proofHash);
    }*/

    public static void main(String[] args) {
        if (args[0].compareTo("seedserver") == 0) {
            DNSSeedNode seedNode = new DNSSeedNode(Constants.SEED_PORT);
        } else if (args[0].compareTo("client") == 0) {
            ClientNode client = new ClientNode(Integer.parseInt(args[1]), Integer.parseInt(args[2]));
        }
    }
}