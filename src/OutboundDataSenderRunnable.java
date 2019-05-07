import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;

/**
 * Runnable used for the thread that is responsible for sending messages
 * out to clients
 */
public class OutboundDataSenderRunnable implements Runnable {
    @Override
    public void run() {
        while(true) {
            Socket connection = null;
            try {
                Message message = ConnectionManager.outBoundMessageQueue.take();
                System.out.println("OUTBOUND: " + message.type + " to " + message.destinationAddress);
                connection = new Socket();
                connection.setReuseAddress(true);
                connection.bind(new InetSocketAddress(ConnectionManager.outboundPort));
                connection.connect(new InetSocketAddress("localhost", message.destinationAddress));
                ObjectOutputStream outputStream = new ObjectOutputStream(connection.getOutputStream());
                outputStream.writeObject(message);
                outputStream.flush();
            } catch (ConnectException e) {
                // don't retry the message
            } catch (NoRouteToHostException e) {
                // don't retry the message
            } catch (SocketException e) {
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