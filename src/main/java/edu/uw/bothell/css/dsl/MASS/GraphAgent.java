package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

import edu.uw.bothell.css.dsl.MASS.Agent;
import edu.uw.bothell.css.dsl.MASS.MASS;

public class GraphAgent extends SmartAgent implements Serializable {

    public GraphAgent() {
        super();
    }

    public GraphAgent(Object arg) {

        //Commenting for Auto Agent Migration
        //itinerary = (int[]) arg;

        super(arg);

        MASS.getLogger().debug("Graph agent(" + getAgentId() + ") was born.");
    }

    @Override
    public int map(int initPopulation, int[] size, int[] index, int offset) {
        int rank = MASS.getMyPid();
        int systemSize = MASS.getSystemSize();

        // Calculate allotted agents for this node.
        int allottedAgents = initPopulation / systemSize;
        int remainder = initPopulation % systemSize;
        allottedAgents += rank < remainder ? 1 : 0;
        if (allottedAgents > AgentsBase.MAX_AGENTS_PER_NODE) {
            allottedAgents = AgentsBase.MAX_AGENTS_PER_NODE;
        }

        // calculate agents alloted to this index
        int numColonists = allottedAgents / size[0];
        remainder = allottedAgents % size[0];
        numColonists += index[0] < remainder ? 1 : 0;

        return numColonists;
    }
}