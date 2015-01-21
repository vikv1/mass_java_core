package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

@SuppressWarnings("serial")
public class Agent implements Serializable {

	//@SuppressWarnings("unused")
	//private final int agentsHandle;

	//@SuppressWarnings("unused")
	//private final int placesHandle;

	private final int agentId;

	//@SuppressWarnings("unused")
	//private final int parentId;

	private Place place = null;
	private int[] index = null;
	private boolean alive = true;
	private int newChildren = 0;
	private Object[] arguments = null;

	public Agent ( ) {
		//agentsHandle = Agents.getAgentInitAgentsHandle();
		//placesHandle = Agents.getAgentInitPlacesHandle();
		agentId = Agents.getAgentInitAgentId();
		//parentId = Agents.getAgentInitParentId();
	}

	public Object callMethod( int functionId, Object argument ) {
		return null;
	}

	public int getAgentId() {
		return agentId;
	}

	public Object[] getArguments() {
		return arguments;
	}

	/**
	 * Get debug data from the agent 
	 * @return Debug data
	 */
	public Object getDebugData(){
		Integer id = new Integer(agentId);
		return (Object)id;
	}

	public int[] getIndex() {
		return index;
	}

	public int getNewChildren() {
		return newChildren;
	}

	public Place getPlace() {
		return place;
	}

	public boolean isAlive() {
		return alive;
	}

	public void kill( ) {
		alive = false;
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

	//Set index for an Agent to migrate to
	protected boolean migrate( int... index ) { 
		
		int[] placesSize = place.getSize();
		for ( int i = 0; i < placesSize.length; i++ ) {
			if ( index[i] >= 0 && index[i] < placesSize[i] ) {
				continue;
			} else {
				return false;
			}
		}
		
		this.index = index.clone( ); // assign the new index
		return true;
	
	}

	// TODO - modify debug data of the agent, overridden as necessary by the developer for now
	public void setDebugData(Object argument){

	}

	public void setIndex(int[] index) {
		this.index = index;
	}

	public void setNewChildren(int newChildren) {
		this.newChildren = newChildren;
	}

	public void setPlace(Place place) {
		this.place = place;
	}

	//Set number for spawning additional Agents
	protected void spawn( int numAgents, Object[] arguments ) { 
		
		//Only want to make changes if the number to be created is above zero
		if ( numAgents > 0 ) {
			newChildren = numAgents;
			this.arguments = arguments.clone( );
		}
	
	}

}