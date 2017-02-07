package edu.uw.bothell.css.dsl.MASS;

/**
 * Created by utku on 1/24/17.
 *
 * This class simply contains the serialized version of the new agent object
 *  and the coordinates that new agent will become active.
 */
public class AgentSpawnRequest
{
    // Serialized agent identifier
    private String serializedAgentIdentifier;

    // index[0], index[1], and index[2] correspond to coordinates of x, y, and z, or those of i, j, and k.
    //private int[] index;

    // Getters
    public String getSerializedAgentIdentifier() { return serializedAgentIdentifier; }

    //public int[] getIndex() { return index; }

    // Setters
    public void setSerializedAgentIdentifier(String serializedAgentIdentifier) { this.serializedAgentIdentifier = serializedAgentIdentifier; }

    //public void setIndex(int[] index) { this.index = index; }
}
