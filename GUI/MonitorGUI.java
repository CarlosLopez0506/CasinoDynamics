package GUI;

import javax.swing.*;
import java.awt.*;
import javax.swing.table.DefaultTableModel;

/**
 * MonitorGUI is a Singleton JFrame used to display player information and final balance.
 * It features a table for player data and a label for the total balance.
 */
public class MonitorGUI extends JFrame {

    private static MonitorGUI instance;
    private final JTable playerTable;
    private final JLabel finalBalanceLabel;
    private JPanel mainContent;
    private JScrollPane agentSP; // Placeholder for the agent's content
    private JLabel tableTitle;

    /**
     * Private constructor to initialize the MonitorGUI frame.
     */
    private MonitorGUI() {
        setTitle("Monitor Update");
        setSize(840, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocation(
                (int) ((Toolkit.getDefaultToolkit().getScreenSize().width - getWidth()) * 0.5),
                (int) ((Toolkit.getDefaultToolkit().getScreenSize().height - getHeight()) * 0.5)
        );
        setResizable(false);

        // Initialize playerTable and finalBalanceLabel
        playerTable = new JTable(new DefaultTableModel(new Object[]{"Player", "State", "Balance", "Chips"}, 0));
        playerTable.setDefaultEditor(Object.class, null);
        finalBalanceLabel = new JLabel("Ganaste: $0.00");
        finalBalanceLabel.setFont(new Font("Arial", Font.BOLD, 24));

        // SideBar Initialization
        JPanel sideBar = new JPanel();
        sideBar.setPreferredSize(new Dimension(200, 0));
        sideBar.setLayout(new GridBagLayout());

        GridBagConstraints sidebarConstraints = new GridBagConstraints();
        sidebarConstraints.fill = GridBagConstraints.HORIZONTAL;
        sidebarConstraints.insets = new Insets(10, 0, 10, 10);
        sidebarConstraints.gridx = 0;
        sidebarConstraints.weightx = 1.0;

        // Title Section
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
        sidebarTitle.setFont(new Font("Arial", Font.BOLD, 24));
        titlePanel.add(sidebarTitle);

        JLabel sidebarSubtitle = new JLabel("Ganancias");
        sidebarSubtitle.setAlignmentX(SwingConstants.LEFT);
        sidebarSubtitle.setFont(new Font("Arial", Font.ITALIC, 20));
        titlePanel.add(sidebarSubtitle);

        titleConstraints.gridx = 0;
        titleConstraints.weightx = 2.0;
        titleSection.add(titlePanel, titleConstraints);

        ImageIcon tahaliImage = new ImageIcon("Assets/tahali-dorado.png");
        JLabel tahali = new JLabel(new ImageIcon(tahaliImage.getImage().getScaledInstance(28, 50, Image.SCALE_SMOOTH)));
        tahali.setBounds(0, 0, 100, 100);
        titleConstraints.gridx = 2;
        titleConstraints.weightx = 1.0;
        tahali.setAlignmentX(SwingConstants.RIGHT);
        titleSection.add(tahali, titleConstraints);

        sidebarConstraints.gridy = 0;
        sidebarConstraints.weighty = 1.0;
        sideBar.add(titleSection, sidebarConstraints);

        // Main Content Panel (without filter logic)
        mainContent = new JPanel();
        mainContent.setLayout(new BorderLayout());

        tableTitle = new JLabel("TABLA DE JUGADORES");
        tableTitle.setFont(new Font("Arial", Font.ITALIC, 16));

        mainContent.add(tableTitle, BorderLayout.NORTH);
        agentSP = new JScrollPane(playerTable);  // Use playerTable in JScrollPane
        mainContent.add(agentSP, BorderLayout.CENTER);
        mainContent.add(finalBalanceLabel, BorderLayout.SOUTH);

        // Set up the frame layout
        setLayout(new BorderLayout());
        add(sideBar, BorderLayout.WEST);
        add(mainContent, BorderLayout.CENTER);
    }

    /**
     * Gets the Singleton instance of the MonitorGUI.
     *
     * @return the instance of MonitorGUI
     */
    public static MonitorGUI getInstance() {
        if (instance == null) {
            instance = new MonitorGUI();
            instance.setVisible(true);
        }
        return instance;
    }

    /**
     * Updates the table and final balance label with the given player updates.
     *
     * @param playerUpdates an array of strings containing player data
     */
    public void update(String[] playerUpdates) {
        DefaultTableModel tableModel = (DefaultTableModel) playerTable.getModel();
        tableModel.setRowCount(0);

        double totalBalance = 0.0;

        for (String playerUpdate : playerUpdates) {
            String[] playerData = playerUpdate.split(", ");
            String player = playerData[0].split(": ")[1];
            String state = playerData[1].split(": ")[1];
            double balance = Double.parseDouble(playerData[2].split(": ")[1]);
            double chips = Double.parseDouble(playerData[3].split(": ")[1]);

            tableModel.addRow(new Object[]{player, state, balance, chips});
            totalBalance += balance;
            totalBalance  = totalBalance - 10000;
        }

        finalBalanceLabel.setText("Ganaste: $" + String.format("%.2f", totalBalance));

        revalidate();
        repaint();
    }
}
