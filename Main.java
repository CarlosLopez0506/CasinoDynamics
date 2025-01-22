import Agent.Agent;
import GUI.ClientGUI;
import Simulator.Casino;

public class Main {
    public static void main(String[] args) {
        new ClientGUI();
        
        Casino casino = new Casino(40, 3, 3, 3, 3,5000);

        casino.open();

        try {
            Thread.sleep(60000);
            casino.close();
        } catch (InterruptedException ex) {
        }
        try {
            Thread.sleep(10000);
            System.out.println(Thread.activeCount());
            for(Agent a: casino.getAgents()) {
                System.out.println(a.getName() + ": " + a.getWorker().getState());
            }

        } catch (InterruptedException ex) {

        }

    }
}
