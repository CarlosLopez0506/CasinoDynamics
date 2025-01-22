package Simulator;

import Agent.Agent;
import Calc.Vector2D;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class SlotMachine extends Agent {
    //STATIC ATTRIBUTES
    public static final float PRICE = 1.05f;

    //PRIVATE ATTRIBUTES
    private SlotMachineSate agentState;
    private Player activePlayer;
    private final Random rand = new Random();
    private boolean available = true;
    private ReentrantLock lock = new ReentrantLock();


    protected SlotMachine(String name, Casino casino, Vector2D position) {
        super(name, casino, new File("Assets/slotmachine.png"));
        this.pos = position;
        agentState = SlotMachineSate.AVAILABLE;
    }


    //Interaction
    public synchronized void startGame(Player player) throws InterruptedException, UnsupportedAudioFileException, LineUnavailableException, IOException {
        try{
            player.charge(SlotMachine.PRICE);
        }catch (Exception e){ //not enough funds
            System.out.println("Not enough funds: " + e.getMessage());
            return;
        }
        agentState = SlotMachineSate.IN_GAME;
        float odds = rand.nextFloat();
        float result = 0.0f;

        Thread.sleep(500);

        if(odds <= 0.01){
            result = 2.0f;
            agentState = SlotMachineSate.JACKPOT_3;
        }
        else if(odds <= 0.15){
            result = 1.5f;
            agentState = SlotMachineSate.JACKPOT_2;
        }
        else if(odds <= 0.4){
            result = 1.25f;
            agentState = SlotMachineSate.JACKPOT_1;
        }
        else{
            agentState = SlotMachineSate.LOSE;
        }

        Thread.sleep(500);
        player.pay(SlotMachine.PRICE * result);
        Casino.playSound("Sounds/SlotMachine-Done.wav");
    }

    //GETTERS

    public Player getPlayer() {
        return activePlayer;
    }

    public boolean isAvailable(){
        return this.available;
    }

    public void takeMachine(Player p){
        lock.lock();
        activePlayer = p;
        this.available = false;
        agentState = SlotMachineSate.TAKEN;
        lock.unlock();
    }

    public void leaveMachine(){
        lock.lock();
        activePlayer = null;
        this.available = true;
        agentState = SlotMachineSate.AVAILABLE;
        lock.unlock();
    }

    @Override
    public String getAgentState() {
        if(!isAgentAlive()) return "CLOSED";
        return agentState.toString();
    }

    @Override
    public void run(){
        while (isAgentAlive() && !isInterrupted()) {
            switch (agentState){
                case CLOSING -> {
                }

                case AVAILABLE -> this.available = true;

                default -> this.available = false;
            }
        }
    }

    @Override
    public void stopWork() {
        this.agentState = SlotMachineSate.CLOSING;
        this.killAgent();
    }

    @Override
    public void startWork() {
        this.agentState = SlotMachineSate.AVAILABLE;
        this.startAgent();
    }

    @Override
    public void draw(Graphics g) {
        if (image == null) {
            g.setColor(new Color(255, 150, 250));
            g.fillArc((int) pos.getX(), (int) pos.getY(), 10, 10, 0, 360);
        }
        else {
            g.drawImage(image, (int) pos.getX(), (int) pos.getY(), 64, 64, null);
        }
    }
}
