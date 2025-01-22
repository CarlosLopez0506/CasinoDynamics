import Utils.NetworkUtils;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MainServer class for handling client connections and starting secondary servers.
 * Includes a Swing-based monitor to display server states using client IP and port as the connection ID.
 */
public class MainServer {

    private static final int PRINCIPAL_PORT = 12345;
    private static final ConcurrentHashMap<String, Server> serverMap = new ConcurrentHashMap<>();
    private static final DefaultTableModel tableModel = new DefaultTableModel(new String[]{"Connection ID", "State"}, 0);

    /**
     * Main method to start the principal server and handle client connections.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainServer::createMonitorGUI);

        try {
            InetAddress wifiAddress = NetworkUtils.getAddress(0);
            ServerSocket principalSocket = new ServerSocket(PRINCIPAL_PORT, 50, wifiAddress);

            System.out.println("Principal Server running at: " + wifiAddress.getHostAddress() + ":" + principalSocket.getLocalPort());

            while (true) {
                Socket clientSocket = principalSocket.accept();
                String connectionId = getConnectionId(clientSocket);
                System.out.println("Client connected: " + connectionId);
                new Thread(() -> handleClient(clientSocket, connectionId), "Main Server").start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles a connected client by starting a secondary server and sending the server information.
     *
     * @param clientSocket the client socket
     * @param connectionId the connection ID for tracking
     */
    private static void handleClient(Socket clientSocket, String connectionId) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            Server secondaryServer = new Server();
            serverMap.put(connectionId, secondaryServer);
            updateTable();

            new Thread(secondaryServer, "Server-" + connectionId).start();
            Thread.sleep(100);  // Adjust this value if needed
            System.out.println(serverMap.size() + " Secondary Servers started.");
            int monitorPort = 5000 + serverMap.size();
            out.println(secondaryServer.getHostAddress() + ":" + secondaryServer.getPort() + ":" + monitorPort);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the table in the Swing monitor with the latest server states.
     */
    private static void updateTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);  // Clear the table
            serverMap.forEach((id, server) -> {
                tableModel.addRow(new Object[]{id, server.getState().name()});
            });
        });
    }

    /**
     * Creates the Swing monitor GUI to display server states.
     */
    private static void createMonitorGUI() {
        JFrame frame = new JFrame("Server Monitor");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        JTable table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        frame.add(scrollPane, BorderLayout.CENTER);

        frame.setVisible(true);

        // Periodically update the table to reflect the latest states
        Timer timer = new Timer(100, e -> updateTable());
        timer.start();
    }

    /**
     * Generates a unique connection ID using the client's IP address and port.
     *
     * @param socket the client socket
     * @return a string combining the client's IP address and port
     */
    private static String getConnectionId(Socket socket) {
        return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
    }
}
