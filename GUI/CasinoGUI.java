package GUI;

import Agent.Agent;
import Calc.Vector2D;
import Simulator.Casino;
import java.awt.*;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;

public class CasinoGUI {
    //CASINO LOCATIONS
    private static final Rectangle LOBBY_RECT = new Rectangle(0, 0, 200, 200);
    private static final Rectangle CASHIER_RECT = new Rectangle(0, 200, 200, 200);
    private static final Rectangle GAME_RECT = new Rectangle(200, 0, 300, 400);

    private BufferedImage lobbyImage;
    private BufferedImage cashierImage;
    private BufferedImage gameImage;

    private final JFrame frame;
    private final CasinoCanvas canvas;

    private final ArrayList<Agent> agents;
    private static final Random rand = new Random();
    private Thread guiUpdater;
    private boolean guiAlive = false;

    public static Vector2D getDestination(CASINO_LOCATION location){
        float x = 0;
        float y = 0;
        switch (location){
            case ENTRANCE -> {
                x = 0;
                y = 200;
            }

            case LOBBY -> {
                x = rand.nextInt(LOBBY_RECT.x, LOBBY_RECT.x + LOBBY_RECT.width);
                y = rand.nextInt(LOBBY_RECT.y, LOBBY_RECT.y + LOBBY_RECT.height);
            }

            case CASHIER_AREA -> {
                x = rand.nextInt(CASHIER_RECT.x, CASHIER_RECT.x + CASHIER_RECT.width);
                y = rand.nextInt(CASHIER_RECT.y, CASHIER_RECT.y + CASHIER_RECT.height);
            }

            case GAME_AREA -> {
                x = rand.nextInt(GAME_RECT.x, GAME_RECT.x + GAME_RECT.width);
                y = rand.nextInt(GAME_RECT.y, GAME_RECT.y + GAME_RECT.height);
            }
        }
        return new Vector2D(x, y);
    }

    public CasinoGUI(Casino casino) {
        frame = new JFrame("CASINO SIMULATOR");
        ImageIcon logo = new ImageIcon("Assets/LogoUP-dorado.jpg");
        frame.setIconImage(logo.getImage());

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setResizable(false);

        try {
            cashierImage = ImageIO.read(new File("Assets/cashier-floor.jpg"));
        } catch (IOException e) {
            System.out.println("Couldn't load Cashier Image:" + e.getMessage());
        }

        try {
            gameImage = ImageIO.read(new File("Assets/game-area.png"));
        } catch (IOException e) {
            System.out.println("Couldn't load Game Image:" + e.getMessage());
        }

        try {
            lobbyImage = ImageIO.read(new File("Assets/lobby.png"));
        } catch (IOException e) {
            System.out.println("Couldn't load Lobby Image:" + e.getMessage());
        }

        canvas = new CasinoCanvas();
        canvas.setPreferredSize(new Dimension(500, 400));
        frame.getContentPane().add(canvas, BorderLayout.CENTER);
        frame.pack();
        frame.setLocation(
                (int) (Toolkit.getDefaultToolkit().getScreenSize().width * 0.5 + Toolkit.getDefaultToolkit().getScreenSize().width * 0.25 - frame.getWidth() * 0.5),
                (int) (Toolkit.getDefaultToolkit().getScreenSize().height * 0.5 - frame.getHeight() * 0.5)
        );
        agents = casino.getAgents();
    }

    public void start(){
        frame.setVisible(true);
        canvas.createBufferStrategy(2);
        guiAlive = true;

        guiUpdater = new Thread(() -> {
            while(isGUIAlive() && !Thread.currentThread().isInterrupted()){
                canvas.update();

                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    System.out.println("GUI Updater Interrupted");
                    break;
                }
            }
        });

        guiUpdater.setName("Casino GUI Updater");
        guiUpdater.start();
    }

    public boolean isGUIAlive() { return guiAlive; }

    public void stopGUI() {
        guiAlive = false;
        guiUpdater.interrupt();
        frame.dispose();
    }

    private class CasinoCanvas extends Canvas{

        public void update(){
            BufferStrategy bufferStrategy = getBufferStrategy();

            if (bufferStrategy == null) {
                return;
            }

            Graphics g = bufferStrategy.getDrawGraphics();

            try {
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, this.getWidth(), this.getHeight());

                //painting the locations
                //LOBBY
                if (lobbyImage == null) {
                    g.setColor(new Color(200, 200, 100));
                    g.fillRect(LOBBY_RECT.x, LOBBY_RECT.y, LOBBY_RECT.width, LOBBY_RECT.height);
                } else {
                    g.drawImage(lobbyImage, LOBBY_RECT.x, LOBBY_RECT.y, LOBBY_RECT.width, LOBBY_RECT.height, null);
                }

                //CASHIER AREA
                if (cashierImage == null) {
                    g.setColor(new Color(100, 200, 100));
                    g.fillRect(CASHIER_RECT.x, CASHIER_RECT.y, CASHIER_RECT.width, CASHIER_RECT.height);
                } else {
                    g.drawImage(cashierImage, CASHIER_RECT.x, CASHIER_RECT.y, CASHIER_RECT.width, CASHIER_RECT.height, null);
                }

                //GAME AREA
                if (gameImage == null) {
                    g.setColor(new Color(100, 100, 200));
                    g.fillRect(GAME_RECT.x, GAME_RECT.y, GAME_RECT.width, GAME_RECT.height);
                } else {
                    g.drawImage(gameImage, GAME_RECT.x, GAME_RECT.y, GAME_RECT.width, GAME_RECT.height, null);
                }

                agents.forEach(agent -> {
                    if(agent.getState() != Thread.State.TERMINATED) agent.draw(g);
                });
            } finally {
                g.dispose();
            }

            bufferStrategy.show();
        }

        @Override
        public void paint(Graphics g) {}
    }
}

