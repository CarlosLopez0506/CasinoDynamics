package Simulator;

import Agent.Agent;
import Calc.Vector2D;
import GUI.CasinoGUI;
import GUI.MonitorView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import javax.sound.sampled.*;

public class Casino {
    //Constants
    private final ArrayList<Player> players = new ArrayList<>();
    private final PriorityBlockingQueue<Cashier> cashiers;
    private final ArrayList<SlotMachine> slotMachines = new ArrayList<>();
    private final ArrayList<Croupier> croupiers = new ArrayList<>();
    private final int monitorPort;
    private boolean closed = true;
    private final CasinoGUI gui;
    private final MonitorView monitorView;

    private final ReentrantLock croupierLock = new ReentrantLock();

    public Casino(int players, int cashiers, int slotMachines, int croupiers, int buffer, int monitorPort) {

        //Instantiate the players
        for (int p = 1; p <= players; p++) {
            this.players.add(new Player("Player" + p, this));
        }

        //Instantiate the cashiers
        this.cashiers = new PriorityBlockingQueue<>(cashiers, Comparator.comparingInt(Cashier::getQueueSize));
        for (int c = 1; c <= cashiers; c++) {
            this.cashiers.add(new Cashier("Cashier" + c, this, new Vector2D((c * 48) - 24, 352)));
        }

        //Intantiate the slotMachines
        for (int s = 1; s <= slotMachines; s++) {
            this.slotMachines.add(new SlotMachine("Slot Machine" + s, this, new Vector2D(192 + (s * 48), 320)));
        }

        //Instantiate the croupiers
        for (int c = 1; c <= croupiers; c++) {
            this.croupiers.add(new Croupier("Croupier" + c, this, new Vector2D(300, c * 64),
                    buffer, Croupier.GameType.ROULETTE));
        }

        this.monitorPort = monitorPort;

        gui = new CasinoGUI(this);
        monitorView = new MonitorView(this);
    }


    //METHODS
    public void open() {
        this.closed = false;
        System.out.println("=== OPENING CASINO ===");

        cashiers.forEach(Cashier::startWork);
        System.out.println("Cashiers started");

        slotMachines.forEach(SlotMachine::startWork);
        System.out.println("Slot Machines started");

        croupiers.forEach(Croupier::startWork);
        System.out.println("Croupiers started");

        players.forEach(Player::startWork);
        System.out.println("Players started");

        System.out.println("=== CASINO OPEN TO BUSINESS ===");

        monitorView.startMonitor();
        gui.start();
    }

    public void close() {
        this.closed = true;
        System.out.println("=== CLOSING CASINO ===");

        players.forEach(player -> {
//            player.stopWork();
            try {
                player.getWorker().join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("Players finished");

        cashiers.forEach(cashier -> {
            cashier.stopWork();
            try {
                cashier.getWorker().join();
                if (cashier.getWorker().isAlive()) cashier.getWorker().interrupt();
                System.out.println("Cashier died");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("Cashiers finished");

        slotMachines.forEach(slotMachine -> {
            slotMachine.stopWork();
            try {
                slotMachine.getWorker().join();
                if (slotMachine.getWorker().isAlive()) slotMachine.getWorker().interrupt();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("Slot Machines finished");

        croupiers.forEach(croupier -> {
            croupier.stopWork();
            try {
                croupier.getWorker().join();
                if (croupier.getWorker().isAlive()) croupier.getWorker().interrupt();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("Croupiers finished");

        System.out.println("=== CASINO CLOSED ===");
//        gui.stopGUI();
//        monitorView.stopTCPServer();
//        monitorView.stopMonitor();
    }

    public Cashier findCashier(Player p) {
        Cashier mostAvailable = null;

        try {
            mostAvailable = cashiers.take(); // Menos agentes --> seleccionado
            mostAvailable.assign(p); // cashier le asigno el player p a la queue
            cashiers.offer(mostAvailable); // Regreso Cashier a la Priory
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Find Cashier interrupted: " + e.getMessage());
        }

        return mostAvailable;
    }

    public Croupier findCroupier(Player p) {
        croupierLock.lock();
        Croupier availableCroupier = null;
        try {
            for (Croupier croupier : croupiers) {
                if (croupier.isAvailable()) {
                    if (croupier.addPlayer(p)) {
                        availableCroupier = croupier;
                        break;
                    }
                }
            }
        } finally {
            croupierLock.unlock();
        }
        return availableCroupier;
    }

    public SlotMachine findSlotMachine(Player p) {
        SlotMachine available = null;
        for (SlotMachine slotMachine : slotMachines) {
            if (slotMachine.isAvailable()) {
                available = slotMachine;
                slotMachine.takeMachine(p);
                break;
            }
        }
        return available;
    }

    public boolean isClosed() {
        return this.closed;
    }

    public ArrayList<Agent> getAgents() {
        ArrayList<Agent> agents = new ArrayList<>(players);
        agents.addAll(cashiers);
        agents.addAll(slotMachines);
        agents.addAll(croupiers);
        return agents;
    }

    public ArrayList<Player> getPlayers() {
        return this.players;
    }

    public PriorityBlockingQueue<Cashier> getCashiers() {
        return this.cashiers;
    }

    public ArrayList<SlotMachine> getSlotMachines() {
        return this.slotMachines;
    }

    public ArrayList<Croupier> getCroupiers() {
        return this.croupiers;
    }

    public int getMonitorPort() {
        return this.monitorPort;
    }

    public static void playSound(String soundFile) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        File file = new File(soundFile);
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);

        Clip clip = AudioSystem.getClip();
        clip.open(audioStream);

        clip.start();
    }

}
