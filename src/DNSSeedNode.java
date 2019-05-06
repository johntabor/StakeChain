import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/* Inspiration came from bitcoin protocol.
    clients initially discover other nodes on bitcoin network by
    connecting to 4-5 DNS seed servers that contain lists of names
    of other clients. The seed servers relay these lists to clients that connect to them.
    For this project, one DNS seed server will suffice.
    It's port will be statically set at 5000.
 */
public class DNSSeedNode {
    private final int port;
    private Map<Integer, Socket> knownClients = new HashMap<>();
    private ServerSocket server;
    private Thread connHandler;    // manages connection requests and dispatches worker threads
    private boolean running = false;

    public DNSSeedNode(int port) {
        this.port = port;
        Scanner sc = new Scanner(System.in);
        System.out.println("Commands: (start) (stop) (quit)");
        while(true) {
            System.out.print("> ");
            String input = sc.nextLine();
            switch(input) {
                case "start":
                    if (running) {
                        System.out.println("Server already running");
                    } else {
                        System.out.println("Starting server...");
                        start();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case "stop":
                    if (!running) {
                        System.out.println("Server already stopped");
                    } else {
                        System.out.println("Stopping server...");
                        stop();
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    break;
                case "quit":
                    if (running) {
                        System.out.println("Stopping server...");
                        stop();
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

    private void start() {
        connHandler = new Thread(new ConnHandler());
        connHandler.start();
        running = true;
    }

    private void stop() {
        try {
            if (server != null) {
                server.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
    }

    class ConnHandler implements Runnable {
        @Override
        public void run() {
            try {
                System.out.println("\tCreating server socket...");
                server = new ServerSocket(port);
                System.out.println("\tDNS Seed Server created");
                while (true) {
                    System.out.println("\tListening for a connection...");
                    Socket client = server.accept();
                    System.out.println("\tAccepted a connection from port " + client.getPort());
                    knownClients.putIfAbsent(client.getPort(), client);
                    new Thread(new Worker(client)).start();
                }
            } catch (SocketException e) { // interrupted by stop() function
                try {
                    if (server != null) {
                        server.close();
                    }
                    System.out.println("\tSuccessfully closed server socket");
                } catch (IOException e1) {
                    System.out.println("Failed to close server socket");
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                System.out.println("Failed to create DNS seed server on port " + port);
                e.printStackTrace();
            }
        }
    }

    class Worker implements Runnable {
        private final Socket client;
        public Worker(Socket client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                PrintWriter serverWriter = new PrintWriter(client.getOutputStream(), true);
                BufferedReader clientStream = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                while(true) {
                    String req = clientStream.readLine();
                    if (req.compareTo("list") == 0) {
                        serverWriter.println(toStringKnownClients(client.getPort()));
                    } else if (req.compareTo("disconnect") == 0) {
                        client.close();
                        return;
                    }
                }
            } catch (IOException e) {
                System.out.println("failed to create serverWriter to port " + client.getPort());
                e.printStackTrace();
            }

            try {
                client.close();
            } catch (IOException e) {
                System.out.println("failed to close connection with client on port " + client.getPort());
                e.printStackTrace();
            }
        }
    }

    // will probably have to synchronize this
    public String toStringKnownClients(int connPort) {
        StringBuilder clients = new StringBuilder();
        for(int clientPort : knownClients.keySet()) {
            if (clientPort != connPort) {
                clients.append(clientPort);
                clients.append('/');
            }
        }
        return clients.toString();
    }
}

