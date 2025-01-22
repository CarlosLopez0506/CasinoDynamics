package GUI;

import Agent.Agent;
import Simulator.*;
import Utils.NetworkUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

enum FILTER_OPTION {
    ALL, PLAYERS, CASHIER, SLOT_MACHINES, CROUPIER, CONTADOR, THREADS
}

final class OptionButton extends JButton {
    private final FILTER_OPTION value;

    private boolean active = false;

    public OptionButton(String label, FILTER_OPTION value) {
        super(label);
        this.value = value;
        this.setBorderPainted(false);
        this.setFocusPainted(false);
        this.setFont(Fonts.TEXT_FONT);

        setActive(false);
    }

    public void setActive(boolean active) {
        this.active = active;

        Color fg = isActive() ? Palette.BLACK : Palette.WHITE;
        Color bg = isActive() ? Palette.GRAY : Palette.GOLD;

        this.setBackground(bg);
        this.setForeground(fg);
    }

    public boolean isActive() {
        return this.active;
    }

    public FILTER_OPTION getValue() {
        return this.value;
    }
}

public class MonitorView {
    private final JFrame frame;

    //filter Buttons
    private OptionButton[] optionButtons = new OptionButton[6];
    private FILTER_OPTION filter = FILTER_OPTION.ALL;

    //Main Components
    private JTable agentTable, playerTable, cashierTable, croupierTable, slotTable, threadTable, counterTable;
    private JScrollPane agentSP, playerSP, cashierSP, croupierSP, slotSP, threadSP, counterSP;
    public JPanel mainContent;
    public JPanel headerContent;
    private JLabel tableTitle;
    private JLabel agentStates;

    private final ArrayList<Player> players;
    private final ArrayList<Agent> agents;
    private final ArrayList<Croupier> croupiers;
    private final ArrayList<SlotMachine> slotMachines;
    private final ArrayList<Cashier> cashiers;
    private ArrayList<Thread> threads;
    private final int monitorPort;
    private Thread monitor;
    private boolean monitorAlive = false;
    private final boolean toggleThreads = false;
    private Thread tcpThread;
    private boolean tcpThreadAlive = false;

    public MonitorView(Casino casino) {
        this.players = casino.getPlayers();
        this.players.sort(Comparator.comparing(Player::getName));
        this.agents = casino.getAgents();
        this.agents.sort(Comparator.comparing(Agent::getName));
        this.croupiers = casino.getCroupiers();
        this.croupiers.sort(Comparator.comparing(Croupier::getName));
        this.slotMachines = casino.getSlotMachines();
        this.cashiers = new ArrayList<>(casino.getCashiers());
        this.cashiers.sort(Comparator.comparing(Cashier::getName));
        this.threads = new ArrayList<>(Thread.getAllStackTraces().keySet());
        this.threads.sort(Comparator.comparing(Thread::getName));
        this.monitorPort = casino.getMonitorPort() + 5000;

        this.frame = new JFrame("Monitor General");

        this.frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.frame.setSize(840, 480);
        this.frame.setLayout(new BorderLayout());
        this.frame.setBackground(Palette.GRAY);
        this.frame.setResizable(false);
        this.frame.setLocation(
                (int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.25 - frame.getWidth() * 0.5),
                (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.5 - frame.getHeight() * 0.5)
        );

        //Agent Table
        String[] colNames = {"#", "Agente", "Estado"};
        Object[][] data = new Object[this.agents.size()][3];
        for (int i = 0; i < this.agents.size(); i++) {
            Agent agent = this.agents.get(i);
            data[i][0] = i;
            data[i][1] = agent.getName();
            data[i][2] = agent.getAgentState();

            System.out.println(agent.getName());
        }
        this.agentTable = new JTable(data, colNames);
        this.agentTable.setDefaultEditor(Object.class, null);
        this.agentTable.setAlignmentX(SwingConstants.CENTER);
        this.agentSP = new JScrollPane(agentTable);
        this.agentSP.setBorder(BorderFactory.createEmptyBorder());

        //Cashier Table
        colNames = new String[]{"#", "Agente", "Estado", "Tamaño de Fila", "Fila de Cajero"};
        data = new Object[this.cashiers.size()][5];
        for (int i = 0; i < this.cashiers.size(); i++) {
            Cashier agent = this.cashiers.get(i);
            data[i][0] = i;
            data[i][1] = agent.getName();
            data[i][2] = agent.getAgentState();
            data[i][3] = agent.getQueueSize();
            data[i][4] = agent.getQueue().stream().map(Player::getName).collect(Collectors.joining(", "));
        }
        this.cashierTable = new JTable(data, colNames);
        this.cashierTable.setDefaultEditor(Object.class, null);
        this.cashierSP = new JScrollPane(cashierTable);
        this.cashierSP.setBorder(BorderFactory.createEmptyBorder());

        //Croupier Table
        colNames = new String[]{"#", "Agente", "Estado", "Tamaño de Mesa", "Jugadores"};
        data = new Object[this.croupiers.size()][5];
        for (int i = 0; i < this.croupiers.size(); i++) {
            Croupier agent = this.croupiers.get(i);
            data[i][0] = i;
            data[i][1] = agent.getName();
            data[i][2] = agent.getAgentState();
            data[i][3] = agent.numPlayers();
            data[i][4] = agent.getPlayers().stream().map(Player::getName).collect(Collectors.joining(", "));
        }
        this.croupierTable = new JTable(data, colNames);
        this.croupierTable.setDefaultEditor(Object.class, null);
        this.croupierSP = new JScrollPane(croupierTable);
        this.croupierSP.setBorder(BorderFactory.createEmptyBorder());

        //SlotMachine Table
        colNames = new String[]{"#", "Agente", "Estado", "Jugador"};
        data = new Object[this.slotMachines.size()][4];
        for (int i = 0; i < this.slotMachines.size(); i++) {
            SlotMachine agent = this.slotMachines.get(i);
            data[i][0] = i;
            data[i][1] = agent.getName();
            data[i][2] = agent.getAgentState();
            data[i][3] = agent.getPlayer() != null ? agent.getPlayer().getName() : "";
        }
        this.slotTable = new JTable(data, colNames);
        this.slotTable.setDefaultEditor(Object.class, null);
        this.slotSP = new JScrollPane(slotTable);
        this.slotSP.setBorder(BorderFactory.createEmptyBorder());

        //Player Table
        String[] playerColNames = {"#", "Agente", "Estado", "Balance", "Fichas"};
        Object[][] playerData = new Object[this.players.size()][5];
        for (int i = 0; i < this.players.size(); i++) {
            Player player = this.players.get(i);
            playerData[i][0] = i;
            playerData[i][1] = player.getName();
            playerData[i][2] = player.getAgentState();
            playerData[i][3] = "$" + player.getBalance();
            playerData[i][4] = player.getChips();
        }
        this.playerTable = new JTable(playerData, playerColNames);
        this.playerTable.setDefaultEditor(Object.class, null);
        this.playerSP = new JScrollPane(playerTable);
        this.playerSP.setBorder(BorderFactory.createEmptyBorder());

        //Thread Table
        colNames = new String[]{"#", "Nombre", "Estado"};
        this.threadTable = new JTable(new DefaultTableModel(colNames, 0));
        this.threadTable.setDefaultEditor(Object.class, null);
        this.threadSP = new JScrollPane(threadTable);
        this.threadSP.setBorder(BorderFactory.createEmptyBorder());

        initComponents();
    }

    private void initComponents() {
        ImageIcon logo = new ImageIcon("Assets/LogoUP-dorado.jpg");
        frame.setIconImage(logo.getImage());

        //SideBar
        JPanel sideBar = new JPanel();
        sideBar.setPreferredSize(new Dimension(200, 0));
        sideBar.setLayout(new GridBagLayout());

        GridBagConstraints sidebarConstraints = new GridBagConstraints();
        sidebarConstraints.fill = GridBagConstraints.HORIZONTAL;
        sidebarConstraints.insets = new Insets(10, 0, 10, 10);
        sidebarConstraints.gridx = 0;
        sidebarConstraints.weightx = 1.0;

        //Title Section
        JPanel titleSection = new JPanel();
        titleSection.setLayout(new GridBagLayout());

        GridBagConstraints titleConstraints = new GridBagConstraints();
        titleConstraints.fill = GridBagConstraints.BOTH;
        titleConstraints.insets = new Insets(5, 5, 5, 0);
        titleConstraints.gridy = 0;

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new GridLayout(0, 1));

        JLabel sidebarTitle = new JLabel("Monitor");
        sidebarTitle.setAlignmentX(SwingConstants.LEFT);

        sidebarTitle.setFont(Fonts.HEADING_FONT);
        titlePanel.add(sidebarTitle);

        JLabel sidebarSubtitle = new JLabel("GENERAL");
        sidebarSubtitle.setAlignmentX(SwingConstants.LEFT);
        sidebarSubtitle.setFont(Fonts.SUBHEADING_FONT);

        titlePanel.add(sidebarSubtitle);

        titleConstraints.gridx = 0;
        titleConstraints.weightx = 2.0;
        titleSection.add(titlePanel, titleConstraints);

        ImageIcon tahaliImage = new ImageIcon("Assets/tahali-dorado.png");
        JLabel tahali = new JLabel(new ImageIcon(
                tahaliImage.getImage().getScaledInstance(28, 50, Image.SCALE_SMOOTH)
        ));
        tahali.setBounds(0, 0, 100, 100);
        titleConstraints.gridx = 2;
        titleConstraints.weightx = 1.0;
        tahali.setAlignmentX(SwingConstants.RIGHT);
        titleSection.add(tahali, titleConstraints);

        sidebarConstraints.gridy = 0;
        sidebarConstraints.weighty = 1.0;
        sideBar.add(titleSection, sidebarConstraints);

        //Option Section
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(0, 1, 0, 10));

        //Options Buttons
        optionButtons[0] = new OptionButton("Todos", FILTER_OPTION.ALL);
        optionButtons[0].setActive(true);

        optionButtons[1] = new OptionButton("Jugadores", FILTER_OPTION.PLAYERS);
        optionButtons[2] = new OptionButton("Cajeros", FILTER_OPTION.CASHIER);
        optionButtons[3] = new OptionButton("Slot Machines", FILTER_OPTION.SLOT_MACHINES);
        optionButtons[4] = new OptionButton("Croupiers", FILTER_OPTION.CROUPIER);
        optionButtons[5] = new OptionButton("Threads", FILTER_OPTION.THREADS);
        optionButtons = Arrays.copyOf(optionButtons, 7);
        optionButtons[6] = new OptionButton("Contador", FILTER_OPTION.CONTADOR);

        // Create the counter table
        String[] counterColNames = {"Agente", "Estado", "Número de Agentes"};
        this.counterTable = new JTable(new DefaultTableModel(counterColNames, 0));
        this.counterSP = new JScrollPane(counterTable);
        this.counterSP.setBorder(BorderFactory.createEmptyBorder());

        for (OptionButton btn : optionButtons) {
            if (btn != null) {
                btn.addActionListener((ActionEvent actionListner) -> {
                    this.filter = btn.getValue();
                    for (OptionButton option : optionButtons) {
                        if (option != null) {
                            option.setActive(option.getValue() == this.filter);
                        }
                    }

                    mainContent.remove(agentSP);
                    mainContent.remove(playerSP);
                    mainContent.remove(cashierSP);
                    mainContent.remove(slotSP);
                    mainContent.remove(croupierSP);
                    mainContent.remove(threadSP);
                    mainContent.remove(counterSP);  // Remove counter scroll pane

                    this.agentStates.setVisible(true);
                    switch (this.filter) {
                        case ALL -> {
                            mainContent.add(agentSP);
                            this.tableTitle.setText("TABLA DE AGENTES");
                            this.agentStates.setVisible(false);
                        }
                        case PLAYERS -> {
                            mainContent.add(playerSP);
                            this.tableTitle.setText("TABLA DE JUGADORES");
                            this.agentStates.setText("<html><div style='text-align: center;'> " +
                                    Arrays.stream(PlayerState.values()).map(Enum::name).collect(Collectors.joining(", ")) + "</div></html>");
                        }
                        case CASHIER -> {
                            mainContent.add(cashierSP);
                            this.tableTitle.setText("TABLA DE CAJEROS");
                            this.agentStates.setText("<html><div style='text-align: center;'> " +
                                    Arrays.stream(CashierState.values()).map(Enum::name).collect(Collectors.joining(", ")) + "</div></html>");
                        }
                        case SLOT_MACHINES -> {
                            mainContent.add(slotSP);
                            this.tableTitle.setText("TABLA DE MÁQUINAS TRAGAMONEDAS");
                            this.agentStates.setText("<html><div style='text-align: center;'> " +
                                    Arrays.stream(SlotMachineSate.values()).map(Enum::name).collect(Collectors.joining(", ")) + "</div></html>");
                        }
                        case CROUPIER -> {
                            mainContent.add(croupierSP);
                            this.tableTitle.setText("TABLA DE CROUPIERS");
                            this.agentStates.setText("<html><div style='text-align: center;'> " +
                                    Arrays.stream(CroupierState.values()).map(Enum::name).collect(Collectors.joining(", ")) + "</div></html>");
                        }
                        case THREADS -> {
                            mainContent.add(threadSP);
                            this.tableTitle.setText("TABLA DE HILOS");
                            this.agentStates.setVisible(false);
                        }
                        case CONTADOR -> {
                            mainContent.add(counterSP);
                            this.tableTitle.setText("CONTADOR DE ESTADOS");
                            this.agentStates.setVisible(false);
                        }
                        default -> throw new AssertionError();
                    }

                    frame.revalidate();
                    frame.repaint();
                });
                btn.setHorizontalAlignment(SwingConstants.LEFT);
                optionsPanel.add(btn);
            }
        }

        sidebarConstraints.gridy = 1;
        sidebarConstraints.weighty = 2;
        sideBar.add(optionsPanel, sidebarConstraints);
        frame.getContentPane().add(sideBar, BorderLayout.LINE_START);

        //Main Content
        mainContent = new JPanel();
        mainContent.setLayout(new BorderLayout());

        headerContent = new JPanel();
        headerContent.setLayout(new BorderLayout());

        tableTitle = new JLabel("TABLA DE AGENTES");
        tableTitle.setFont(Fonts.SUBHEADING_FONT);

        agentStates = new JLabel();
        agentStates.setFont(Fonts.STATES_FONT);
        agentStates.setHorizontalAlignment(SwingConstants.CENTER);

        headerContent.add(this.tableTitle, BorderLayout.NORTH);
        headerContent.add(this.agentStates, BorderLayout.SOUTH);

        mainContent.add(headerContent, BorderLayout.NORTH);
        mainContent.add(agentSP, BorderLayout.CENTER);

        frame.getContentPane().add(mainContent, BorderLayout.CENTER);
    }

    private void updatePlayerTable() {
        for (int p = 0; p < this.players.size(); p++) {
            Player curr = players.get(p);
            this.playerTable.setValueAt(curr.getAgentState(), p, 2);
            this.playerTable.setValueAt("$" + curr.getBalance(), p, 3);
            this.playerTable.setValueAt(curr.getChips(), p, 4);
        }
    }

    private void updateAgentTable() {
        for (int a = 0; a < this.agents.size(); a++) {
            Agent curr = this.agents.get(a);
            this.agentTable.setValueAt(curr.getAgentState(), a, 2);
        }
    }

    private void updateCashierTable() {
        for (int c = 0; c < this.cashiers.size(); c++) {
            Cashier curr = this.cashiers.get(c);
            this.cashierTable.setValueAt(curr.getAgentState(), c, 2);
            this.cashierTable.setValueAt(curr.getQueueSize(), c, 3);
            this.cashierTable.setValueAt(curr.getQueue().stream().map(Player::getName).collect(Collectors.joining(", ")), c, 4);
        }
    }

    private void updateCroupierTable() {
        for (int c = 0; c < this.croupiers.size(); c++) {
            Croupier curr = this.croupiers.get(c);
            this.croupierTable.setValueAt(curr.getAgentState(), c, 2);
            this.croupierTable.setValueAt(curr.numPlayers(), c, 3);
            this.croupierTable.setValueAt(curr.getPlayers().stream().map(Player::getName).collect(Collectors.joining(", ")), c, 4);
        }
    }

    private void updateSlotTable() {
        for (int s = 0; s < this.slotMachines.size(); s++) {
            SlotMachine curr = this.slotMachines.get(s);
            this.slotTable.setValueAt(curr.getAgentState(), s, 2);
            this.slotTable.setValueAt(curr.getPlayer() != null ? curr.getPlayer().getName() : "", s, 3);
        }
    }

    private void updateThreadTable() {
        threads = new ArrayList<>(Thread.getAllStackTraces().keySet());
        this.threads.sort(Comparator.comparing(Thread::getName));
        DefaultTableModel model = (DefaultTableModel) threadTable.getModel();
        model.setRowCount(0);

        for (Thread t : threads) {
            if (toggleThreads && t.getThreadGroup() == Thread.currentThread().getThreadGroup()) {
                model.addRow(new Object[]{
                        model.getRowCount(),
                        t.getName(),
                        t.getState().toString()
                });
            } else {
                model.addRow(new Object[]{
                        model.getRowCount(),
                        t.getName(),
                        t.getState().toString()
                });
            }
        }
    }

    public void startTCPServer() {
        System.out.println("Starting TCP Server... in port: " + monitorPort);
        tcpThreadAlive = true;
        tcpThread = new Thread(() -> {
            InetAddress wifiAddress = null;
            try {
                wifiAddress = NetworkUtils.getAddress(0);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try (ServerSocket serverSocket = new ServerSocket(monitorPort, 50, wifiAddress);
            ) {
                System.out.println("TCP Server started on port 5000.");
                while (tcpThreadAlive) {
                    try (Socket clientSocket = serverSocket.accept();
                         OutputStream outputStream = clientSocket.getOutputStream();
                         PrintWriter writer = new PrintWriter(outputStream, true)) {

                        System.out.println("Client connected.");
                        while (tcpThreadAlive && !Thread.currentThread().isInterrupted()) {
                            String currentState = getCurrentState();
                            writer.println(currentState);
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                System.err.println("Error starting TCP Server: " + e.getMessage());
            }
        });

        tcpThread.setName("TCP Monitor Server");
        tcpThread.start();
    }

    public void stopTCPServer() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tcpThreadAlive = false;
        if (tcpThread != null) {
            tcpThread.interrupt();
        }
    }


    private String getCurrentState() {
        StringBuilder state = new StringBuilder();
        for (Player player : players) {
            state.append(String.format("Player: %s, State: %s, Balance: %s, Chips: %s|",
                    player.getName(), player.getAgentState(), player.getBalance(), player.getChips()));
        }

        return state.toString();
    }

    private void updateCounterTable() {
        DefaultTableModel model = (DefaultTableModel) counterTable.getModel();
        model.setRowCount(0);

        // Count Players by state
        Map<String, Long> playerStateCount = players.stream()
                .collect(Collectors.groupingBy(
                        Player::getAgentState,
                        Collectors.counting()
                ));
        for (PlayerState state : PlayerState.values()) {
            model.addRow(new Object[]{"Jugadores", state, playerStateCount.getOrDefault(state.name(), 0L)});
        }

        // Count Cashiers by state
        Map<String, Long> cashierStateCount = cashiers.stream()
                .collect(Collectors.groupingBy(
                        Cashier::getAgentState,
                        Collectors.counting()
                ));
        for (CashierState state : CashierState.values()) {
            model.addRow(new Object[]{"Cajeros", state, cashierStateCount.getOrDefault(state.name(), 0L)});
        }

        // Count Croupiers by state
        Map<String, Long> croupierStateCount = croupiers.stream()
                .collect(Collectors.groupingBy(
                        Croupier::getAgentState,
                        Collectors.counting()
                ));
        for (CroupierState state : CroupierState.values()) {
            model.addRow(new Object[]{"Croupiers", state, croupierStateCount.getOrDefault(state.name(), 0L)});
        }

        // Count Slot Machines by state
        Map<String, Long> slotMachineStateCount = slotMachines.stream()
                .collect(Collectors.groupingBy(
                        SlotMachine::getAgentState,
                        Collectors.counting()
                ));
        for (SlotMachineSate state : SlotMachineSate.values()) {
            model.addRow(new Object[]{"Máquinas Slot", state, slotMachineStateCount.getOrDefault(state.name(), 0L)});
        }
    }

    public void startMonitor() {
        monitorAlive = true;
        startTCPServer();
        monitor = new Thread(() -> {
            while (isMonitorAlive() && !Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                    switch (this.filter) {
                        case ALL -> updateAgentTable();
                        case PLAYERS -> updatePlayerTable();
                        case CASHIER -> updateCashierTable();
                        case SLOT_MACHINES -> updateSlotTable();
                        case CROUPIER -> updateCroupierTable();
                        case THREADS -> updateThreadTable();
                        case CONTADOR -> updateCounterTable();
                        default -> throw new AssertionError();
                    }

                } catch (InterruptedException ie) {
                    System.out.println("Monitor Updater Interrupted");
                    break;
                }
            }
        });

        monitor.setName("Monitor");
        monitor.start();
        frame.setVisible(true);
    }

    public boolean isMonitorAlive() {
        return monitorAlive;
    }

    public void stopMonitor() {
        monitorAlive = false;
        stopTCPServer();
        monitor.interrupt();
        frame.dispose();
    }
}