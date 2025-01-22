import Simulator.Casino;
import Utils.NetworkUtils;

import java.io.*;
import java.net.*;

/**
 * Server class that runs a secondary server to process requests from a Principal Server.
 */
public class Server implements Runnable {

    private final ServerSocket serverSocket;
    private final String hostAddress;
    private volatile ServerState state;  // Tracks the current state of the server

    /**
     * Constructs a Server instance that listens on a random port on the provided WiFi address.
     *
     * @throws IOException if an I/O error occurs when initializing the server socket
     */
    public Server() throws IOException {
        InetAddress wifiAddress = NetworkUtils.getAddress(0);
        this.serverSocket = new ServerSocket(0, 50, wifiAddress);
        this.hostAddress = wifiAddress.getHostAddress();
        this.state = ServerState.WAIT_FOR_CLIENT;  // Initialize the state
    }

    /**
     * Runs the server, accepting client connections and processing their requests.
     */
    @Override
    public void run() {
        System.out.println("Secondary Server running at: " + hostAddress + ":" + serverSocket.getLocalPort());

        try (Socket clientSocket = serverSocket.accept();
             DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            state = ServerState.SEND_INITIAL_MESSAGE;
            out.writeUTF("I can help you with the task assigned by Principal Server.");

            state = ServerState.PROCESS_CLIENT_MESSAGE;
            processClientMessages(in, out);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            shutdownServer();
        }
    }

    /**
     * Processes client messages by receiving casino details and simulating the casino operation.
     *
     * @param in  the input stream to receive data from the client
     * @param out the output stream to send responses to the client
     * @throws IOException if an I/O error occurs while processing client messages
     */
    private void processClientMessages(DataInputStream in, DataOutputStream out) throws IOException {
        Casino casino = receiveCasinoDetails(in);

        System.out.println("Launching casino GUI...");
        casino.open();  // Open the casino

        byte duration = in.readByte();  // The duration the casino will run (in seconds)
        System.out.println("The casino will run for " + duration + " seconds.");

        state = ServerState.RUN_SIMULATION;
        new Thread(() -> runCasinoSimulation(casino, duration), "Server Run Simulation").start();
    }

    /**
     * Simulates the casino operation for a given duration.
     *
     * @param casino the Casino object to be simulated
     * @param duration the duration for which the casino will run, in seconds
     */
    private void runCasinoSimulation(Casino casino, byte duration) {
        try {
            System.out.println("Simulating casino for " + duration + " seconds...");
            Thread.sleep(duration * 60000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Closing casino...");
            casino.close();  // Close the casino
            state = ServerState.WAIT_FOR_CLIENT;  // Reset to wait for new client
        }
    }

    /**
     * Receives and parses casino details from the client.
     *
     * @param in the input stream from which the casino details are read
     * @return the instantiated Casino object with the received details
     * @throws IOException if an I/O error occurs while reading the casino details
     */
    private static Casino receiveCasinoDetails(DataInputStream in) throws IOException {
        byte players = in.readByte();    // Number of players
        byte cashiers = in.readByte();   // Number of cashiers
        byte slotMachines = in.readByte(); // Number of slot machines
        byte croupiers = in.readByte();  // Number of croupiers
        byte buffer = in.readByte();
        byte monitorPort = in.readByte(); // Monitor port

        System.out.println("Received casino details:");
        System.out.println("Players: " + players);
        System.out.println("Cashiers: " + cashiers);
        System.out.println("Slot Machines: " + slotMachines);
        System.out.println("Croupiers: " + croupiers);
        System.out.println("Buffer: " + buffer);
        System.out.println("Monitor Port: " + monitorPort);

        return new Casino(players, cashiers, slotMachines, croupiers, buffer, monitorPort);
    }

    /**
     * Shuts down the server and closes the server socket.
     */
    private void shutdownServer() {
        try {
            serverSocket.close();
            System.out.println("Secondary Server shut down.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the host address of the server.
     *
     * @return the host address
     */
    public String getHostAddress() {
        return hostAddress;
    }

    /**
     * Returns the port on which the server is listening.
     *
     * @return the port number
     */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * Returns the current state of the server.
     *
     * @return the current server state
     */
    public ServerState getState() {
        return state;
    }

    /**
     * Enum to represent the different states of the server.
     */
    public enum ServerState {
        WAIT_FOR_CLIENT,
        SEND_INITIAL_MESSAGE,
        PROCESS_CLIENT_MESSAGE,
        RUN_SIMULATION
    }
}
