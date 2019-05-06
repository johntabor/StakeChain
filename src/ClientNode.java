import java.util.Scanner;

/*
// treat address as the public key
class Wallet {
    private String address;

    public String adjustTo64(String s) {
        switch (s.length()) {
            case 62:
                return "00" + s;
            case 63:
                return "0" + s;
            case 64:
                return s;
            default:
                throw new IllegalArgumentException("not a valid key: " + s);
        }
    }

    public Wallet(String address) {
        this.address = address;
    }
}*/


public class ClientNode {
    /*
     * Indicates whether or not the client is connected
     * to the network
     */
    private Thread algorand;
    private boolean connected = false;

    public ClientNode(int inboundPort, int outboundPort) {
        ConnectionManager.init(inboundPort, outboundPort);
        startCommandInterface();
    }

    /**
     * Connects the client to the network
     */
    private void connect() {
        ConnectionManager.start();
        LedgerManager.start();
        algorand = new Thread(new AlgorandRunnable());
        algorand.start();
        connected = true;
    }

    /**
     * Disconnects the client from the network
     */
    private void disconnect() {
        ConnectionManager.stop();
        connected = false;
    }

    private void startCommandInterface() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Commands: (send <transaction>) (disconnect) (quit)");
        connect();
        while(true) {
            //System.out.print("> ");
            String input = sc.nextLine();
            switch(input) {
                /*
                case "connect":
                    if (connected) {
                        System.out.println("Client already connected");
                    } else {
                        System.out.println("Connecting client...");
                        connect();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                 */
                case "send":
                    if (!connected) {
                        System.out.println("Must connect to the network first");
                    } else {
                        Scanner sc2 = new Scanner(System.in);
                        System.out.println("Enter source address: ");
                        String source = sc2.nextLine();
                        System.out.println("Enter recipient address: ");
                        String recipient = sc2.nextLine();
                        System.out.println("Enter id: ");
                        int id = sc2.nextInt();
                        System.out.println("Enter amount to transfer: ");
                        int amount = sc2.nextInt();
                        ConnectionManager.sendTransaction(id, source, recipient, amount);
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case "disconnect":
                    if (!connected) {
                        System.out.println("Client already disconnected");
                    } else {
                        System.out.println("Disconnecting client...");
                        disconnect();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case "quit":
                    if (connected) {
                        System.out.println("Disconnecting client...");
                        disconnect();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Goodbye");
                    return;
                default:
                    System.out.println("Invalid command");
            }
        }
    }
}
