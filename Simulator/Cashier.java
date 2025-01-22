package Simulator;

import Agent.Agent;
import Calc.Vector2D;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

public class Cashier extends Agent {
    //STATIC ATTRIBUTES
    public static final float CHIP_PRICE = 1.5f;

    //PRIVATE ATTRIBUTES
    private CashierState agentState;
    private boolean busy;
    public Queue<Player> queue;
    private final Semaphore semaphore = new Semaphore(1);

    protected Cashier(String name, Casino casino, Vector2D position) {
        super(name, casino, new File("Assets/cashier.png"));
        this.busy = false;
        this.queue = new LinkedList<>();
        this.pos = position;
        this.agentState = CashierState.AVAILABLE;
    }
    //========================== INTERACTIONS ==================================
    /**
     * Charge to the player n chips and pays m money. If the player does not have
     * enough chips, then it will return and do nothing.
     * At the end the cashier change its state to AVAILABLE
     * @param chips the amount of chips to be charged
     * @param player the agent the cashier is interacting with
     */
    public void exchange(int chips, Player player) throws InterruptedException{
        this.busy = true;
        agentState = CashierState.EXCHANGING;

        try {
            player.charge(chips);
        } catch (Exception e) { //not enough chips
            System.err.println(e.getMessage());
            agentState = CashierState.AVAILABLE;
            return;
        }
        Thread.sleep(3000);
        float money = chips * CHIP_PRICE;
        player.pay(money);

        removeFromQueue();
        agentState = CashierState.AVAILABLE;
    }

    /**
     * Charge to the player m money and pays n chips. If the player does not have
     * enough funds, then it will return and do nothing.
     * At the end the cashier change its state to AVAILABLE
     * @param money the amount of money to be charged
     * @param player the agent the cashier is interacting with
     */
    public void exchange(float money, Player player) throws InterruptedException, UnsupportedAudioFileException, LineUnavailableException, IOException {
        Casino.playSound("Sounds/Cashier-done.wav");
        this.busy = true;
        agentState = CashierState.EXCHANGING;

        try {
            player.charge(money);
        } catch (Exception e) { //not enough funds
            System.err.println(e.getMessage());

            agentState = CashierState.AVAILABLE;
            return;
        }

        int chips = (int) (money / CHIP_PRICE);

        Thread.sleep(3000);
        player.pay(chips);

        removeFromQueue();
        agentState = CashierState.AVAILABLE;
    }

    public synchronized void assign(Player p){
        this.busy = true;
        agentState = CashierState.EXCHANGING;
        addToQueue(p);
    }

    //GETTERS
    public boolean isBusy(){
        return this.busy;
    }


    public Semaphore getSemaphore() {
        return semaphore;
    }

    public synchronized Queue<Player> getQueue() { return queue; }
    public synchronized int getQueueSize() { return this.queue.size(); }
    public synchronized void addToQueue(Player p) { this.queue.add(p); }
    public synchronized void removeFromQueue() { this.queue.poll(); }

    //OVERRIDE METHODS
    @Override
    public String getAgentState() {
        if(!isAgentAlive()) return "CLOSED";

        return switch (agentState) {
            case AVAILABLE -> "AVAILABLE";
            case EXCHANGING -> "EXCHANGING";
            case CLOSING -> "CLOSING";
        };
    }
    @Override
    public void run(){
        while(isAgentAlive() && !isInterrupted()) {
            switch (agentState){
                case AVAILABLE:
                    this.busy = false;
                    break;

                case EXCHANGING:
                    this.busy = true;
                    break;

                case CLOSING:
                    break;

                default:
                    System.out.println("default case");
            }
//            System.out.println(getName() + " QUEUE SIZE: " + this.queue.size() + " Players: " + getQueue().stream().map(Player::getName).collect(Collectors.joining(", ")));
        }
    }

    @Override
    public void stopWork() {
        this.agentState = CashierState.CLOSING;
        this.killAgent();
    }

    @Override
    public void startWork() {
        this.agentState = CashierState.AVAILABLE;
        this.startAgent();
    }

    @Override
    public void draw(Graphics g) {
        //g.setColor(isBusy() ? Color.RED : Color.GREEN);
        if (image == null) {
            g.setColor(Color.GRAY);
            g.fillRect((int) this.pos.getX(), (int) this.pos.getY(), 25, 25);
        }
        else {
            g.drawImage(image, (int) this.pos.getX(), (int) this.pos.getY(), 32, 32, null);
        }
    }
}
