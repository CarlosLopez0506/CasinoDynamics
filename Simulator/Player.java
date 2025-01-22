package Simulator;

import Agent.Agent;
import Calc.Vector2D;
import GUI.CASINO_LOCATION;
import GUI.CasinoGUI;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class Player extends Agent {
    private final Random rand = new Random();

    //States attributes
    private PlayerState agentState;
    private PlayerState nextState;

    //Movement attributes
    private Vector2D direction;
    private Vector2D destination;

    //Player attributes
    private float balance;
    private int chips;

    //Interaction attributes
    private int chipsToPay;
    private float moneyToPay;

    private Semaphore bouncer;
    private Cashier attendingCashier;
    private SlotMachine currentSlotMachine;
    private Croupier currentCroupier;

    public Player(String name, Casino casino) {
        super(name, casino);

        this.agentState = PlayerState.ENTERING;
        this.chips = 0;
        this.initBalance();

        this.pos = CasinoGUI.getDestination(CASINO_LOCATION.ENTRANCE);
    }

    //========================= SETUP ============================

    /**
     * Initialize the balance of the player with a random number
     * between $5,000.00 and $10,000.00.
     */
    private void initBalance() {
        this.balance = 10000;
    }
    //============================================================

    //======================= ACTIONS ============================

    /**
     * Set the direction to a specific point in the Casino GUI and sets
     * the direction and the destination attributes of the player;
     *
     * @param destination the destination of the agent
     * @param nextState   set the next state after reaching the destination
     */
    private void setDirection(Vector2D destination, PlayerState nextState) {
        Vector2D dir = Vector2D.getDirection(this.pos, destination);
        float speed = 6f;
        this.direction = dir.normalized();
        this.direction.scale(speed);
        this.destination = destination;
        this.nextState = nextState;
    }

    /**
     * Updates the position to the players direction, and if the player
     * reached the destination it will switch the state to the nextState
     * attribute
     */
    private void move() {
        agentState = PlayerState.WALKING;

        float distance = Vector2D.distance(this.pos, this.destination);
        float epsilon = 1f;

        Vector2D newDirection = Vector2D.sub(this.destination, this.pos).normalized();
        float dot = Vector2D.dot(newDirection, this.direction.normalized());

        if (distance < epsilon) this.agentState = this.nextState;
        else {
            if (dot <= 0) this.agentState = this.nextState;
        }

        this.pos = Vector2D.add(pos, this.direction);
    }

    /**
     * Set the destination to the lobby and assign IN_LOBBY as nextSate
     */
    private void goToLobby() {
        setDirection(CasinoGUI.getDestination(CASINO_LOCATION.LOBBY), PlayerState.IN_LOBBY);
        agentState = PlayerState.WALKING;
    }

    /**
     * Set the agent state to ENTERING and then goes to the lobby
     */
    private void enter() {
        agentState = PlayerState.ENTERING;
        goToLobby();

        while (agentState != nextState) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                killAgent();
            }
            move();
        }

        agentState = PlayerState.DECIDING;
    }

    /**
     * Echange all the chips if remaining and then leaves the casino.
     */
    private void leave() {
        this.killAgent();
    }

    /**
     * Sends the player to the attending cashier, if the attengin cashier is null
     * then send the player to the lobby
     */
    private void goToCashier() {
        if (this.attendingCashier == null) {
            agentState = PlayerState.DECIDING;
        }

        setDirection(Vector2D.add(this.attendingCashier.getPos(), new Vector2D(-10, 0)), PlayerState.EXCHANGING);
        agentState = PlayerState.WALKING;
    }

    private void exchange(int chips) {
        moneyToPay = 0;
        chipsToPay = chips;


        setDirection(CasinoGUI.getDestination(CASINO_LOCATION.CASHIER_AREA), PlayerState.WAITING_CASHIER);
        agentState = PlayerState.WALKING;
    }

    private void exchange(float money) {
        chipsToPay = 0;
        moneyToPay = money;

        setDirection(CasinoGUI.getDestination(CASINO_LOCATION.CASHIER_AREA), PlayerState.WAITING_CASHIER);
        agentState = PlayerState.WALKING;

    }

    private void waitForCashier() {
        this.attendingCashier = casino.findCashier(this);

        if (this.attendingCashier == null) return;

        bouncer = attendingCashier.getSemaphore();
        try {
            bouncer.acquire();
            goToCashier();

        } catch (InterruptedException e) {
            bouncer.release();
            this.attendingCashier.removeFromQueue();
            agentState = PlayerState.DECIDING;
        }
    }

    private void decide() throws InterruptedException {
        Thread.sleep(1000);

        if (casino.isClosed()) {
            if (this.getChips() > 0) {
                exchange(this.getChips());
            } else {
                setDirection(CasinoGUI.getDestination(CASINO_LOCATION.ENTRANCE), PlayerState.LEAVING);
                agentState = PlayerState.WALKING;
            }
            return;
        }
        float decision = rand.nextFloat();
        if (decision < 0.33) {
            setDirection(CasinoGUI.getDestination(CASINO_LOCATION.GAME_AREA), PlayerState.IN_GAME_AREA);
            agentState = PlayerState.WALKING;
            return;
        }else if(decision < 0.66){
            goToLobby();
            return;
        }
        exchange(rand.nextFloat(100) + 100);
    }

    private void findSlotMachine(){
        currentSlotMachine = casino.findSlotMachine(this);
        if(currentSlotMachine == null){
            agentState = PlayerState.DECIDING;
            return;
        }
        walkToSlotMachine();
    }

    private void findCroupier() {
        currentCroupier = casino.findCroupier(this);
        if(currentCroupier == null){
            agentState = PlayerState.DECIDING;
            return;
        }

        walkToCroupier();
    }

    private void play() throws InterruptedException {
        if(casino.isClosed()){
            agentState = PlayerState.DECIDING;
            return;
        }
        //boolean slotMachine = rand.nextBoolean();
        boolean playAlone = rand.nextBoolean();

        if (playAlone) {
            if(getBalance() >= SlotMachine.PRICE) agentState = PlayerState.FINDING_SLOT_MACHINE;
            else agentState = PlayerState.DECIDING;
        } else {
            if(getChips() >= Croupier.COST) agentState = PlayerState.FINDING_TABLE;
            else agentState = PlayerState.DECIDING;
        }
    }

    /**
     * Directs the player to the current Slot machine, if there is not
     * current slot machine, then go to the lobby
     */
    private void walkToSlotMachine()  {
        if(currentSlotMachine == null){
            agentState = PlayerState.DECIDING;
            return;
        }
        //Goes to the slot machine
        setDirection(Vector2D.add(currentSlotMachine.getPos(), new Vector2D(-10, 0)), PlayerState.IN_GAME_SLOT_MACHINE);
        agentState = PlayerState.WALKING;
    }

    /**
     * Directs the player to the current Slot machine, if there is not
     * current slot machine, then go to the lobby
     */
    private void walkToCroupier()  {
        if(currentCroupier == null){
            agentState = PlayerState.DECIDING;
            return;
        }

        int place = currentCroupier.numPlayers();

        setDirection(Vector2D.add(currentCroupier.getPos(), new Vector2D(-10, 10 * place)), PlayerState.WAITING_GAME_START);
        agentState = PlayerState.WALKING;
    }

    /**
     * Simulates a round with the current slot machine
     */
    private void playSlotMachine(){
        try {
            currentSlotMachine.startGame(this);
        } catch (InterruptedException | UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            throw new RuntimeException(e);
        } finally {
            currentSlotMachine.leaveMachine();
            currentSlotMachine = null;
            agentState = PlayerState.DECIDING;
        }
    }


    private void playCroupier() {
        currentCroupier.getResults(this);
        currentCroupier = null;
        agentState = PlayerState.DECIDING;
        
    }

    private void waitGameToStart(){
        if(casino.isClosed()) 
            agentState = PlayerState.DECIDING;

    }


    public void gameStarted() {
        //agentState = PlayerState.IN_GAME;
    }

    public void gameEnded() {
        agentState = PlayerState.WAITING_GAME_END;
    }

    public void resultsApplied() throws InterruptedException {
        agentState = PlayerState.DECIDING;
    }

    // =============================== INTERACTIONS ====================================

    /**
     * Change the state of the player to a certain state
     *
     * @param state the state to be changed
     */
    public void setState(PlayerState state) {
        this.agentState = state;
    }

    public void excahngeWithCashier() {
        try {
//            System.err.println("Time To Pay -> chips: " + chipsToPay + " or money: " + moneyToPay);
            if (moneyToPay > 0) {
                attendingCashier.exchange(moneyToPay, this);
                moneyToPay = 0;
            } else {
                attendingCashier.exchange(chipsToPay, this);
                chipsToPay = 0;
            }
        } catch (InterruptedException | UnsupportedAudioFileException | LineUnavailableException | IOException ie) {
            System.out.println(ie.getMessage());
        } finally {
            if (!casino.isClosed()) {
                agentState = PlayerState.DECIDING;

            } else {
                setDirection(CasinoGUI.getDestination(CASINO_LOCATION.ENTRANCE), PlayerState.LEAVING);
                agentState = PlayerState.WALKING;
            }
            bouncer.release();
        }

    }

    /**
     * Charge a value of chips to the player, if the player does not
     * have the enough amount then throws an exception.
     *
     * @param chips The amount of chips to be charged
     * @throws Exception
     */
    public void charge(int chips) throws Exception {
        if (chips > getChips()) throw new Exception("Not enough chips");

        this.chips -= chips;
    }

    /**
     * Charge a value of money to the player, if the player does not
     * have the enough funds then throws an exception.
     *
     * @param money The amount of money to be charged
     * @throws Exception
     */
    public void charge(float money) throws Exception {
        if (money > getBalance()) throw new Exception("Not enough funds");

        this.balance -= money;
    }

    /**
     * Increase the number of chips that the player has
     *
     * @param chips The amount of chips to be added
     */
    public void pay(int chips) {
        this.chips += chips;
    }

    /**
     * Increase the amount of funds that the player has in their balance
     *
     * @param money the amount of money to be added
     */
    public void pay(float money) {
        this.balance += money;
    }

    //=====================================================================================

    //GETTERS
    public Agent getCurrentAgent() {
        return attendingCashier != null ? attendingCashier
                : currentCroupier != null ? currentCroupier
                : currentSlotMachine;
    }
    public float getBalance() {
        return this.balance;
    }

    public int getChips() {
        return this.chips;
    }

    //SETTERS
    public void setChips(int newChips) {
        chips = newChips;
    }

    public void setInGameState() {
        //agentState = PlayerState.IN_GAME;
    }

    /**
     * Controls the states of the player
     */
    private void execute() throws InterruptedException {
//        System.err.println("@Execute " + getAgentState());
        switch (agentState) {
            case IN_LOBBY, DECIDING -> decide();

            case FINDING_SLOT_MACHINE -> findSlotMachine();
            case FINDING_TABLE -> findCroupier();

            case IN_GAME_AREA -> play();
            case WALKING -> move();

            case WAITING_CASHIER -> waitForCashier();
            case EXCHANGING -> excahngeWithCashier();
            case LEAVING -> leave();

            case IN_GAME_SLOT_MACHINE -> playSlotMachine();

            case WAITING_GAME_START -> waitGameToStart();
            case WAITING_GAME_END -> playCroupier();
        }

        if(Vector2D.distance(pos, this.destination) > 1 && agentState != PlayerState.WALKING) {
            this.pos = destination;
        }
        Thread.sleep(100);
    }

    @Override
    public void run() {
        this.enter();
        while (isAgentAlive()) {
            try {
                execute();
            } catch (InterruptedException ie) {
            }

        }
    }

    @Override
    public String getAgentState() {
        if (!isAgentAlive()) return "OUT";

        return agentState.toString();
    }

    @Override
    public void stopWork() {
        this.killAgent();
    }

    @Override
    public void startWork() {
        this.agentState = PlayerState.ENTERING;
        this.startAgent();
    }

    @Override
    public void draw(Graphics g) {
        switch (agentState) {
            case DECIDING -> g.setColor(Color.CYAN);
            case IN_GAME_CROUPIER, IN_GAME_SLOT_MACHINE, WAITING_GAME_START, WAITING_GAME_END -> g.setColor(Color.ORANGE);
            default -> g.setColor(Color.WHITE);
        }
        g.fillArc((int) pos.getX(), (int) pos.getY(), 10, 10, 0, 360);
    }
}
