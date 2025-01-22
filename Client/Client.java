package Client;

import java.io.*;
import java.net.*;
import javax.swing.*;

import GUI.ClientGUI;
import GUI.MonitorGUI;

/**
 * Client class responsible for managing connections and communication with the casino server.
 */
public class Client {

    private static final String PRINCIPAL_HOST = "127.0.0.1";
    private static final int PRINCIPAL_PORT = 12345;
    private static final int MONITOR_PORT = 5000;

    /**
     * Main method that initializes the client GUI.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }

    /**
     * Sends the casino configuration details to the server.
     *
     * @param players   The number of players.
     * @param cashiers  The number of cashiers.
     * @param slots     The number of slots.
     * @param croupiers The number of croupiers.
     * @param duration  The duration of the casino operation.
     */
    public static void sendCasinoConfiguration(int players, int cashiers, int slots, int croupiers, int buffer, int duration) {
        try {
            String response = connectToPrincipalServer();
            if (response != null) {
                processSecondaryServerResponse(response, players, cashiers, slots, croupiers, buffer, duration);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connects to the principal server and retrieves secondary server information.
     *
     * @return The response from the principal server.
     * @throws IOException If an error occurs during the connection.
     */
    private static String connectToPrincipalServer() throws IOException {
        try (Socket principalSocket = new Socket(PRINCIPAL_HOST, PRINCIPAL_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(principalSocket.getInputStream()));
             PrintWriter out = new PrintWriter(principalSocket.getOutputStream(), true)) {

            out.println("Request Secondary Server Info");
            return in.readLine();
        }
    }

    /**
     * Processes the response from the principal server to connect to the secondary server.
     *
     * @param response  The response containing secondary server details.
     * @param players   The number of players.
     * @param cashiers  The number of cashiers.
     * @param slots     The number of slots.
     * @param croupiers The number of croupiers.
     * @param duration  The duration of the casino operation.
     * @throws IOException If an error occurs during the connection.
     */
    private static void processSecondaryServerResponse(String response, int players, int cashiers, int slots, int croupiers, int buffer, int duration) throws IOException {
        String[] parts = response.split(":");
        if (parts.length == 3) {
            String secondaryHost = parts[0].trim();
            int secondaryPort = Integer.parseInt(parts[1].trim());
            int monitorPort = Integer.parseInt(parts[2].trim());
            System.out.println(monitorPort);
            connectToSecondaryServer(secondaryHost, secondaryPort, monitorPort, players, cashiers, slots, croupiers, buffer, duration);
        } else {
            System.err.println("Invalid response from Principal Server.");
        }
    }

    /**
     * Connects to the secondary server and sends the casino details.
     *
     * @param secondaryHost The host of the secondary server.
     * @param secondaryPort The port of the secondary server.
     * @param monitorPort   The monitor port.
     * @param players       The number of players.
     * @param cashiers      The number of cashiers.
     * @param slots         The number of slots.
     * @param croupiers     The number of croupiers.
     * @param duration      The duration of the casino operation.
     * @throws IOException If an error occurs during the connection.
     */
    private static void connectToSecondaryServer(String secondaryHost, int secondaryPort, int monitorPort, int players, int cashiers, int slots, int croupiers, int buffer, int duration) throws IOException {
        try (Socket secondarySocket = new Socket(secondaryHost, secondaryPort);
             DataInputStream in = new DataInputStream(secondarySocket.getInputStream());
             DataOutputStream out = new DataOutputStream(secondarySocket.getOutputStream())) {

            byte monitorServerPort = (byte) (monitorPort - 5000);

            sendCasinoDetails(out, (byte) players, (byte) cashiers, (byte) slots, (byte) croupiers, (byte) buffer, (byte) duration, monitorServerPort);
            new Thread(() -> listenToMonitor(monitorPort), "Client Casino").start();

        }
    }

    /**
     * Listens for updates from the monitor server.
     *
     * @param monitorPort The monitor server port.
     */
    private static void listenToMonitor(int monitorPort) {
        int retryInterval = 500;
        int maxRetries = 40;
        int attempt = 0;

        while (attempt < maxRetries) {
            try (Socket monitorSocket = new Socket(PRINCIPAL_HOST, monitorPort);
                 BufferedReader in = new BufferedReader(new InputStreamReader(monitorSocket.getInputStream()))) {

                String message;
                while ((message = in.readLine()) != null) {
                    handleMonitorUpdate(message);
                }
                return;
            } catch (IOException e) {
                attempt++;
                try {
                    Thread.sleep(retryInterval);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        System.err.println("Failed to connect to the monitor server after " + maxRetries + " attempts.");
    }

    /**
     * Handles monitor updates by processing player data and updating the GUI.
     *
     * @param message The update message from the monitor server.
     */
    private static void handleMonitorUpdate(String message) {
        String[] playerUpdates = message.split("\\|");
        SwingUtilities.invokeLater(() -> updateGUI(playerUpdates));
    }

    /**
     * Updates the GUI with the latest player data.
     *
     * @param playerUpdates The array of player update data.
     */
    private static void updateGUI(String[] playerUpdates) {
        MonitorGUI monitorGUI = MonitorGUI.getInstance();
        monitorGUI.update(playerUpdates);
    }

    /**
     * Sends casino details to the secondary server.
     *
     * @param out         The output stream to send data.
     * @param players     The number of players.
     * @param cashiers    The number of cashiers.
     * @param slots       The number of slots.
     * @param croupiers   The number of croupiers.
     * @param duration    The duration of the casino operation.
     * @param monitorPort The monitor server port.
     * @throws IOException If an error occurs during data transmission.
     */
    private static void sendCasinoDetails(DataOutputStream out, byte players, byte cashiers, byte slots, byte croupiers, byte buffer, byte duration, byte monitorPort) throws IOException {
        out.writeByte(players);
        out.writeByte(cashiers);
        out.writeByte(slots);
        out.writeByte(croupiers);
        out.writeByte(buffer);
        out.writeByte(monitorPort);
        out.writeByte(duration);
        out.flush();
    }
}
