package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

public class Agent implements Serializable {
    public Object callMethod( int functionId, Object argument ) {
	return null;
    }

    public int map( int initPopulation, int[] size, int[] index, Place curPlace) {
	// compute the total # places
	int placeTotal = 1;
	for ( int x = 0; x < size.length; x++ )
	    placeTotal *= size[x];

	// compute the global linear index
	int linearIndex = 0;
	for ( int i = 0; i < index.length; i++ ) {
	    if ( index[i] >= 0 && size[i] > 0 && index[i] < size[i] ) {
		linearIndex = linearIndex * size[i];
		linearIndex += index[i];
	    }
	}

	// compute #agents per place a.k.a. colonists
	int colonists = initPopulation / placeTotal;
	int remainders = initPopulation % placeTotal;
	if ( linearIndex < remainders ) colonists++; // add a remainder

	return colonists;
    }

    public void kill( ) {
	alive = false;
    }

    //Set index for an Agent to migrate to
    protected boolean migrate( int... index ) { 
	int[] placesSize = place.size;
	for ( int i = 0; i < placesSize.length; i++ ) {
	    if ( index[i] >= 0 && index[i] < placesSize[i] )
		continue;
	    else
		return false;
	}
	this.index = index.clone( ); // assign the new index
	return true;
    }

    //Set number for spawning additional Agents
    protected void spawn( int numAgents, Object[] arguments ) { 
	//Only want to make changes if the number to be created is above zero
	if ( numAgents > 0 ) {
	    newChildren = numAgents;
	    this.arguments = arguments.clone( );
	}
    }

    protected final int agentsHandle;
    protected final int placesHandle;
    protected final int agentId;
    protected final int parentId;
    protected Place place = null;
    protected int[] index = null;
    protected boolean alive = true;
    protected int newChildren = 0;
    protected Object[] arguments = null;

    public Agent ( ) {
	agentsHandle = Agents.agentInitAgentsHandle;
	placesHandle = Agents.agentInitPlacesHandle;
	agentId = Agents.agentInitAgentId;
	parentId = Agents.agentInitParentId;
    }
}