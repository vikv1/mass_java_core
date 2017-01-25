package edu.uw.bothell.css.dsl.MASS;

/**
 * Created by utku on 1/24/17.
 *
 * This class simply contains the serialized version of the new agent object
 *  and the coordinates that new agent will become active.
 */
public class AgentSpawnRequest
{
    // Serialized agent object
    // private KRYOOBJECT serializedAgent;

    // Coordinates
    private int x, y;

    // Getters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // Setters
    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}
