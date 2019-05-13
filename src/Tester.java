
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.Random;
import java.util.Scanner;

public class Tester {
    private final int PORT = 9010;
    private final int CONNECT_PORT = 5000;
    private int currentTxId;
    //private Map<String, Integer> balances = new HashMap<>();
    private int[] balances = new int[Constants.BLOCK_SIZE];
    private int[] newBalances = new int[Constants.BLOCK_SIZE];

    public Tester() {
        // populate initial wallet balances
        Block genesis = LedgerManager.buildGenesis();
        for (int i = 0; i < Constants.BLOCK_SIZE; i++) {
            //String address = "address" + i;
            //balances.put(address, genesis.transactions[i].amount);
            balances[i] = genesis.transactions[i].amount;
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

                    //int[] addresses = new int[6];

                    /*
                    addresses[0] = 5000;
                    addresses[1] = 6000;
                    addresses[2] = 7000;
                    addresses[3] = 10000;
                    addresses[4] = 9400;
                    addresses[5] = 11000;
                    Random r = new Random();
                    sendTransaction(tx, addresses[r.nextInt(6)]); */
                    //sendTransaction(tx,
                    sendTransaction(tx, 5000);
                }
                balances = newBalances;
            }
        }
    }

    private void sendTransaction(Transaction tx, int address) {
        Message message = Message.buildTransactionMessage(PORT, address, tx);
        try (Socket connection = new Socket("localhost", address)) {
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
            if (balances[senderAddressNumber] > 0) {
                break;
            }
            /*if (balances.get("address" + senderAddressNumber) > 0) {
                break;
            }*/

        }

        // get a valid, random amount to send
        int balance = balances[senderAddressNumber];
        //int balance = balances.get("address" + senderAddressNumber);
        int amount;
        while(true) {
            amount = 1;
            //amount = r.nextInt(balance) + 1;
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

        // should really get balances from the ledger, BUT for now,
        // change them here
        newBalances[senderAddressNumber] = balances[senderAddressNumber] - amount;
        newBalances[recipientAddressNumber] = balances[recipientAddressNumber] + amount;
        //balances[senderAddressNumber] -= amount;
        //balances[recipientAddressNumber] += amount;
        //balances.put("address" + senderAddressNumber, balances.get("address" + senderAddressNumber) - amount);
        //balances.put("address" + recipientAddressNumber, balances.get("address" + recipientAddressNumber) + amount);

        currentTxId++;
        String sender = "address" + senderAddressNumber;
        String recipient = "address" + recipientAddressNumber;
        return new Transaction(currentTxId, sender, recipient, amount);
    }
}
