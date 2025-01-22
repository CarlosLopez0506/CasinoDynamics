package GUI;

import javax.swing.*;
import Client.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Random;
import javax.swing.border.Border;

/**
 * A GUI application for configuring a casino simulation.
 * Users can specify the number of players, cashiers, slots, croupiers, and simulation duration.
 */
public class ClientGUI extends JFrame {
    private JSpinner playersSpinner;
    private JSpinner cashiersSpinner;
    private JSpinner slotsSpinner;
    private JSpinner croupiersSpinner;

    private JSpinner croupierCapacity;
    private JSpinner durationSpinner;
    private JButton sendButton;

    /**
     * Constructs the ClientGUI with spinners for casino configuration and a send button.
     */
    public ClientGUI() {
        initializeGUI();
    }

    /**
     * Initializes the GUI components and layout.
     */
    private void initializeGUI() {
        ImageIcon tree = new ImageIcon("Assets/LogoUP-dorado.jpg");
        this.setIconImage(tree.getImage());
        this.setSize(700, 550);
        this.setResizable(false);

        this.setTitle("Configuración del Casino");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setLocation(
                (int) ((Toolkit.getDefaultToolkit().getScreenSize().width - getWidth()) * 0.5),
                (int) ((Toolkit.getDefaultToolkit().getScreenSize().height - getHeight()) * 0.5)
        );
        this.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.gridx = 0;
        gbc.weightx = 1;

        JPanel topSection = new JPanel();
        topSection.setLayout(new GridBagLayout());

        GridBagConstraints topSectionConstraints = new GridBagConstraints();
        topSectionConstraints.fill = GridBagConstraints.BOTH;
        topSectionConstraints.insets = new Insets(10, 0, 10, 0);
        topSectionConstraints.gridy = 0;
        topSectionConstraints.weighty = 1.0;


        ImageIcon logoImage = new ImageIcon("Assets/LogoUP-Titnto.png");
        JLabel logo = new JLabel(new ImageIcon(
            logoImage.getImage().getScaledInstance(130, 150, Image.SCALE_REPLICATE)
        ));

        topSectionConstraints.gridx = 0;
        topSectionConstraints.weightx = 1.0;
        topSection.add(logo, topSectionConstraints);


        JPanel titleSection = new JPanel();
        titleSection.setLayout(new GridLayout(0, 1, 0, 0));

        JLabel title = new JLabel("Casino Dynamics");
        title.setFont(Fonts.HEADING_FONT);
        title.setAlignmentX(SwingConstants.LEFT);
        titleSection.add(title);

        JLabel subject = new JLabel("Fundamentos de Programación en Paralelo");
        subject.setFont(Fonts.SUBHEADING_FONT);
        subject.setAlignmentX(SwingConstants.LEFT);
        titleSection.add(subject);

        JLabel major = new JLabel("Ing. Sistemas y Gráficas Computacionales");
        major.setAlignmentX(SwingConstants.LEFT);
        titleSection.add(major);

        JLabel teacher = new JLabel("Prof: Doc. Juan Carlos López Pimentel");
        teacher.setAlignmentX(SwingConstants.LEFT);
        titleSection.add(teacher);

        JLabel team = new JLabel("Equipo: Braulio Solorio, Esteban Sánchez, Daniel Esparza, Omar Vidaña");
        team.setAlignmentX(SwingConstants.LEFT);
        titleSection.add(team);

        JLabel date = new JLabel("Fecha: 27 de noviembre de 2024");
        date.setAlignmentX(SwingConstants.LEFT);
        titleSection.add(date);

        topSectionConstraints.gridx = 1;
        topSectionConstraints.weightx = 2.0;
        topSection.add(titleSection, topSectionConstraints);

        add(topSection, gbc);

        JPanel form = new JPanel();
        form.setLayout(new GridLayout(6, 2, 10, 10));

        playersSpinner = createSpinner(8, 1, 30, 1);
        cashiersSpinner = createSpinner(4, 1, 4, 1);
        slotsSpinner = createSpinner(1, 1, 4, 1);
        croupiersSpinner = createSpinner(1, 1, 4, 1);
        croupierCapacity = createSpinner(5,2, 5, 1);
        durationSpinner = createSpinner(1, 1, 60, 1);

        Random random = new Random();

        JLabel lbl1 = new JLabel("Jugadores: ");
        lbl1.setFont(Fonts.TEXT_FONT);
        JLabel lbl2 = new JLabel("Cajeros: ");
        lbl2.setFont(Fonts.TEXT_FONT);
        JLabel lbl3 = new JLabel("SlotMachines: ");
        lbl3.setFont(Fonts.TEXT_FONT);
        JLabel lbl4 = new JLabel("Croupiers: ");
        lbl4.setFont(Fonts.TEXT_FONT);
        JLabel lbl5 = new JLabel("Croupier Capacity");
        lbl5.setFont(Fonts.TEXT_FONT);
        JLabel lbl6 = new JLabel("Duración (min): ");
        lbl5.setFont(Fonts.TEXT_FONT);


        form.add(lbl1);
        form.add(playersSpinner);
        form.add(lbl2);
        form.add(cashiersSpinner);
        form.add(lbl3);
        form.add(slotsSpinner);
        form.add(lbl4);
        form.add(croupiersSpinner);
        form.add(lbl5);
        form.add(croupierCapacity);
        form.add(lbl6);
        form.add(durationSpinner);

        this.getContentPane().add(form, gbc);

        sendButton = new JButton("Empezar");
        sendButton.setBackground(Palette.GOLD);
        sendButton.setForeground(Palette.WHITE);
        this.getContentPane().add(sendButton, gbc);
        add(new JLabel());

        sendButton.addActionListener((ActionEvent e) -> {
            sendData();
        });

        setLocationRelativeTo(null);
        setVisible(true);
    }

    /**
     * Creates a configured JSpinner.
     *
     * @param value Initial value.
     * @param min   Minimum value.
     * @param max   Maximum value.
     * @param step  Step size for incrementing/decrementing the value.
     * @return The configured JSpinner.
     */
    private JSpinner createSpinner(int value, int min, int max, int step) {
        SpinnerNumberModel model = new SpinnerNumberModel(value, min, max, step);
        return new JSpinner(model);
    }

    /**
     * Sends the casino configuration data to the client.
     * Displays a success or error message based on the outcome.
     */
    private void sendData() {
        int players = (int) playersSpinner.getValue();
        int cashiers = (int) cashiersSpinner.getValue();
        int slots = (int) slotsSpinner.getValue();
        int croupiers = (int) croupiersSpinner.getValue();
        int croupierSize = (int) croupierCapacity.getValue();
        int duration = (int) durationSpinner.getValue();

        try {
            dispose();
            Client.sendCasinoConfiguration(players, cashiers, slots, croupiers, croupierSize, duration);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error sending data: " + ex.getMessage());
        }
    }

    /**
     * Entry point of the application.
     * Launches the ClientGUI.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
