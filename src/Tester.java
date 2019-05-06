import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

// will assume that a node listening on port 5000 always exists
// could also ask the dns seed server for the list of all nodes and randomly distribute transactions
// that is a good idea
public class Tester {
    private final int PORT = 9010;
    private final int CONNECT_PORT = 5000;
    private int currentTxId;
    private Map<String, Integer> balances = new HashMap<>();

    public Tester() {
        // populate initial wallet balances
        Block genesis = LedgerManager.buildGenesis();
        for (int i = 0; i < Constants.BLOCK_SIZE; i++) {
            String address = "address" + i;
            balances.put(address, genesis.transactions[i].amount);
        }
        currentTxId = Constants.BLOCK_SIZE - 1;

        // user input
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("Enter number of blocks to send: ");
            int numBlocks = sc.nextInt();
            // need to be able to generate a random block of valid transactions
            for(int i = 0; i < numBlocks; i++) {
                for(int j = 0; j < Constants.BLOCK_SIZE; j++) {
                    Transaction tx = generateRandomValidTransaction();
                    System.out.println("tx: ");
                    System.out.println(tx.toString());
                    sendTransaction(tx);
                }
            }
        }
    }

    private void sendTransaction(Transaction tx) {
        Message message = Message.buildTransactionMessage(PORT, CONNECT_PORT, tx);
        try (Socket connection = new Socket("localhost", CONNECT_PORT)) {
            ObjectOutputStream outputStream = new ObjectOutputStream(connection.getOutputStream());
            outputStream.writeObject(message);
            outputStream.flush();
            System.out.println("Sent transaction with id " + tx.id + " successfully");
        } catch (IOException e) {
        }
    }

    private Transaction generateRandomValidTransaction() {
        Random r = new Random();
        // get an address with a non-zero balance
        int senderAddressNumber = -1;
        while(true) {
            senderAddressNumber = r.nextInt(Constants.BLOCK_SIZE);
            if (balances.get("address" + senderAddressNumber) > 0) {
                break;
            }

        }

        // get a valid, random amount to send
        int balance = balances.get("address" + senderAddressNumber);
        int amount = balance + 1;
        while(true) {
            amount = r.nextInt(balance) + 1;
            if (amount <= balance) {
                break;
            }
        }

        // get random address that isn't the sender
        int recipientAddressNumber = -1;
        while(true) {
            recipientAddressNumber = r.nextInt(Constants.BLOCK_SIZE);
            if (recipientAddressNumber != senderAddressNumber) {
                break;
            }
        }

        currentTxId++;
        String sender = "address" + senderAddressNumber;
        String recipient = "address" + recipientAddressNumber;
        return new Transaction(currentTxId, sender, recipient, amount);
    }
}
