import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Responsible for obtaining an address
 * list from the seed server
 */
public class RequestorRunnable implements Runnable {
    @Override
    public void run() {
        Socket connection = null;
        try {
            connection = new Socket();
            connection.setReuseAddress(true);
            connection.bind(new InetSocketAddress(ConnectionManager.inboundPort));
            connection.connect(new InetSocketAddress("localhost", Constants.SEED_PORT));

            PrintWriter outputStream = new PrintWriter(connection.getOutputStream(), true);
            BufferedReader inputStream = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));
            outputStream.println("list");
            String addrList = inputStream.readLine();
            if (addrList.compareTo("") != 0) {
                System.out.println("list: " + addrList);
                String[] addrs = addrList.split("/");
                ConnectionManager.knownClientsLock.lock();
                try {
                    for (String addr : addrs) {
                        ConnectionManager.knownClients.add(Integer.parseInt(addr));
                    }
                } finally {
                    ConnectionManager.knownClientsLock.unlock();
                }
            }
            outputStream.println("disconnect");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}