import java.util.Scanner;

public class ClientNode {
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
        LedgerManager.start();
        ConnectionManager.start();
        System.out.println("Starting algorand");
        algorand = new Thread(new Algorand());
        algorand.start();
        connected = true;
    }

    /**
     * Disconnects the client from the network
     */
    private void disconnect() {
        ConnectionManager.stop();
        algorand.interrupt();
        connected = false;
    }

    private void startCommandInterface() {
        Scanner sc = new Scanner(System.in);
        System.out.println("Commands: (send <transaction>) (check <ledger block #>) (disconnect) (quit)");
        connect();
        while(true) {
            String input = sc.nextLine();
            switch(input) {
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
                    break;
                case "check":
                    if (!connected) {
                        System.out.println("Must connect to the network first");
                    } else {
                        Scanner sc2 = new Scanner(System.in);
                        System.out.println("Enter block number: ");
                        int num = sc2.nextInt();
                        Block block = LedgerManager.getBlock(num);
                        if (block == null) {
                            System.out.println("Block " + num + " doesn't exist.");
                        } else {
                            System.out.println("Block " + num + " hash: " + Block.getHash(block));
                        }
                    }
                    break;
                case "disconnect":
                    if (!connected) {
                        System.out.println("Client already disconnected");
                    } else {
                        System.out.println("Disconnecting client...");
                        disconnect();
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

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
