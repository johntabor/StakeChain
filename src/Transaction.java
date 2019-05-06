import java.io.Serializable;

public class Transaction implements Serializable {
    public final int id;
    public final String senderAddress;
    public final String recipientAddress;
    public final int amount;

    public Transaction(int id, String senderAddress, String recipientAddress, int amount) {
        this.id = id;
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "txid:" + this.id + "\n"
                + "  sender: " + this.senderAddress + "\n"
                + "  recipient: " + this.recipientAddress + "\n"
                + "  amount: " + this.amount + "\n";
    }
}