package Agent;

import Calc.Vector2D;
import Simulator.Casino;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * This class represents all the agents involved in the simulation
 *
 */
public abstract class Agent extends Thread{
    //PRIVATE METHODS
    private boolean agentAlive = true;

    //PROTECTED ATTRIBUTES
    protected Casino casino;
    //graphics variables
    protected Vector2D pos;
    protected BufferedImage image;
    //PUBLIC ATTRIBUTES
    public abstract String getAgentState();

    protected Agent(String name, Casino casino){
        super(name);
        this.casino = casino;
    }

    protected Agent(String name, Casino casino, File image){
        super(name);
        this.casino = casino;
        try {
            this.image = ImageIO.read(image);
        } catch (IOException e) {
            System.out.println("Couldn't load Agent Image Asset");
        }
    }

    //PROTECTED METHODS
    protected void startAgent(){
        this.start();
    }

    protected void killAgent(){
        this.agentAlive = false;
        this.interrupt();
    }

    //GETTERS
    public Thread getWorker(){
        return this;
    }

    public Vector2D getPos() { return this.pos; }

    public boolean isAgentAlive(){
        return this.agentAlive;
    }

    //ABSTRACT METHODS
    public abstract void stopWork();
    public abstract void startWork();
    public abstract void draw(Graphics g);
}
