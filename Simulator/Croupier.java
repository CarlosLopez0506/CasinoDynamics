package Simulator;

import Agent.Agent;
import Calc.Vector2D;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Semaphore;

public class Croupier extends Agent {
    //STATIC ATTRIBUTES
    public static final int COST = 1;
    public static int SPACES = 5;

    public enum GameType { ROULETTE, Blackjack, Poker}

    //FINAL ATTRIBUTES
    private final List<Player> players = new ArrayList<>(SPACES);
    private final GameType gameType;
    private final Random rand = new Random();

    private final long gameStartWaitTime = 2000;
    private final long gameDuration = 3000;

    private Semaphore available = new Semaphore(SPACES);
    private CroupierState agentState;

    protected Croupier(String name, Casino casino, Vector2D position, int bufferSize, GameType game) {
        super(name, casino, new File("Assets/croupier.png"));
        SPACES = bufferSize;
        pos = position;
        gameType = game;
        agentState = CroupierState.AVAILABLE;
    }

    public synchronized List<Player> getPlayers() {
        return players;
    }

    private void close() throws InterruptedException {
        this.killAgent();
    }

    //Changes STATE and carries out an action depending on the STATE
    private void changeState(CroupierState newState) {
        agentState = newState;
        switch (agentState) {
            case ENDING_GAME -> {
                players.clear();
                changeState(CroupierState.AVAILABLE);
            }

        }
    }

    public synchronized boolean isAvailable() {
        return agentState == CroupierState.AVAILABLE || agentState == CroupierState.WAITING_PLAYERS;
    }

    public boolean addPlayer(Player player) {
        try {
            available.acquire(); // Limit access with semaphore
            synchronized (players) {
                if (players.size() == SPACES) {
                    available.release(); // Release semaphore if game is full
                    System.out.println(getName() + " game is already full");
                    return false;
                }

                // Deduct cost and add player
                player.setChips(player.getChips() - COST);
                players.add(player);
                System.out.println(player.getName() + " has joined. Total players: " + players.size());

                // Check if game can start
                if(players.size() >= SPACES){
                    changeState(CroupierState.STARTING_GAME);
                    startGame();
                }

                if(players.size() >= 2){
                    changeState(CroupierState.WAITING_PLAYERS);
                }

                // else if (agentState == CroupierState.WAITING_PLAYERS) {
                //     startGame();
                //     changeState(CroupierState.STARTING_GAME);
                // }
                
            }
        } catch (InterruptedException | UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            Thread.currentThread().interrupt();
            System.out.println(getName() + " was interrupted");
            return false;
        } finally {
            available.release(); // Ensure semaphore is always released
        }
        return true;
    }

    public void startGame() throws UnsupportedAudioFileException, LineUnavailableException, IOException {
        changeState(CroupierState.GAME_STARTED);

        for (Player player : players) {
            player.setState(PlayerState.IN_GAME_CROUPIER);
        }

        try {
            Thread.sleep(gameDuration);
        } catch (InterruptedException e) {
            for (Player player : players) {
                player.setState(PlayerState.WAITING_GAME_END);
            }
            return;
        }
        
        
        for (Player player : players) {
            player.setState(PlayerState.WAITING_GAME_END);
        }
        
        changeState(CroupierState.ENDING_GAME);
        Casino.playSound("Sounds/Croupier-done.wav");
    }

    private float processResult(float odds) {
        switch (gameType) {
            case ROULETTE -> {
                if (odds <= 0.1f)
                    return 1.5f;
                else if (odds <= 0.5f)
                    return 1.25f;
                else if (odds <= 0.8f)
                    return 1;
                else
                    return 0;
            }
            case Blackjack -> {
                if (odds <= 0.1f)
                    return 1.75f;
                else if (odds <= 0.3f)
                    return 1.5f;
                else if (odds <= 0.6f)
                    return 1.25f;
                else
                    return 0;
            }
            case Poker -> {
                if (odds <= 0.03f)
                    return 2.5f;
                else if (odds <= 0.1f)
                    return 2.0f;
                else if (odds <= 0.4f)
                    return 1.5f;
                else
                    return 0;
            }
        }
        return 0;
    }

    public void getResults(Player player) {
        float odds = rand.nextFloat();
        player.pay((int) (Croupier.COST * processResult(odds)));
    }

    public int numPlayers() {
        return this.players.size();
    }

    @Override
    public void run() {
        while (isAgentAlive() && !isInterrupted()) {
            try {
                switch (agentState) {
                    case WAITING_PLAYERS -> {
                        Thread.sleep(gameStartWaitTime);
                        agentState = CroupierState.STARTING_GAME;
                    }
                    case STARTING_GAME -> startGame();
                    case CLOSING -> close();
                }
            } catch (InterruptedException | UnsupportedAudioFileException | LineUnavailableException | IOException ie) {
                System.out.println("Error Al Cambiar de Estado en Croupier: " + ie.getMessage());
                break;
            }
        }
    }

    @Override
    public String getAgentState() {
        if (!isAgentAlive()) return "CLOSED";
        return agentState.toString();
    }

    @Override
    public void stopWork() {
        this.agentState = CroupierState.CLOSING;
    }

    @Override
    public void startWork() {
        changeState(CroupierState.WAITING_PLAYERS);
        this.startAgent();
    }

    @Override
    public void draw(Graphics g) {
        if (image == null) {
            g.setColor(new Color(113, 255, 1));
            g.fillArc((int) pos.getX(), (int) pos.getY(), 10, 10, 0, 360);
        }
        else {
            g.drawImage(image, (int) pos.getX(), (int) pos.getY(), 96, 48, null);
        }
    }
}
