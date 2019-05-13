import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;

/**
 * Runnable used for the thread that is responsible for sending messages
 * out to clients
 */
public class OutboundDataRouter implements Runnable {
    @Override
    public void run() {
        while(true) {
            Message message;
            Socket connection = null;
            try {
                message = ConnectionManager.outBoundMessageQueue.take();
                //System.out.println("OUTBOUND: " + message.type + " to " + message.destinationAddress);
                connection = new Socket("127.0.0.1", message.destinationAddress);
                ObjectOutputStream outputStream = new ObjectOutputStream(connection.getOutputStream());
                outputStream.writeObject(message);
                outputStream.flush();
            } catch (ConnectException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                // shutdown
            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}