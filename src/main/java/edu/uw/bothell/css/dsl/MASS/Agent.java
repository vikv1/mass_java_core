/*

 	MASS Java Software License
	© 2012-2015 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2015 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

import edu.uw.bothell.css.dsl.MASS.logging.Log4J2Logger;

@SuppressWarnings("serial")
public class Agent implements Serializable {

	/** 
	 * Is this agent’s identifier. It is calculated as: 
	 * the sequence number * the size of this agent’s belonging 
	 * matrix + the index of the current place when all
	 * places are flattened to a single dimensional array.
	 */
	private int agentId;

	/**
	 * The current place where this Agent resides
	 */
	private Place place = null;
	
	/**
	 * Is an array that maintains the coordinates of where this agent resides.
	 * Intuitively, index[0], index[1], and index[2] correspond to coordinates of
	 * x, y, and z, or those of i, j, and k.
	 */
	private int[] index = null;
	
	/** 
	 * Is true while this agent is active. Once it is set false, this agent is 
	 * killed upon a next call to Agents.manageAll( ).
	 */
	private boolean alive = true;
	
	/**
	 * Is the number of new children created by this agent upon a next call to
	 * Agents.manageAll( ).
	 */
	private int newChildren = 0;
	
	/** 
	 * Is an array of arguments, each passed to a different new child.
	 */
	private Object[] arguments = null;

	// logging
	private transient Log4J2Logger logger = Log4J2Logger.getInstance();

	public Agent ( ) {
		//agentId = Agents.getAgentInitAgentId();
	}

	/**
	 * Is called from Agents.callAll. It invokes the function specified with
	 * functionId as passing arguments to this function. A user-derived Agent
	 * class must implement this method.
	 * @param functionId
	 * @param argument
	 * @return 
	 */  	
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
	public Number getDebugData(){
		return null;
	}
	
	public void setDebugData(Number data) {}

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

	/**
	 * Terminates the calling agent upon a next call to Agents.manageAll( ).
	 * More specifically, kill( ) sets the "alive" variable false.
	 */
	public void kill( ) {
		alive = false;
	}

	 /**
	  * Returns the number of agents to initially instantiate on a place indexed
	  * with coordinates[]. The maxAgents parameter indicates the number of
	  * agents to create over the entire application. The argument size[] defines
	  * the size of the "Place" matrix to which a given "Agent" class belongs. The
	  * system-provided (thus default) map( ) method distributes agents over
	  * places uniformly as in:
	  *        maxAgents / size.length
	  * The map( ) method may be overloaded by an application-specific method.
	  * A user-provided map( ) method may ignore maxAgents when creating
	  * agents.
	  * @param initPopulation
	  * @param size
	  * @param index
	  * @return 
	  */	
	public int map( int initPopulation, int[] size, int[] index ) {

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

	/**
	 * Initiates an agent migration upon a next call to Agents.manageAll( ). More
	 * specifically, migrate( ) updates the calling agent’s index[].
	 * @param index
	 * @return 
	 */
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

	protected void setAgentId(Integer agentId) {
		this.agentId = agentId;
	}

	protected void setIndex(int[] index) {
		this.index = index;
	}

	protected void setNewChildren(int newChildren) {
		this.newChildren = newChildren;
	}

	protected void setPlace(Place place) {
		this.place = place;
	}

	/**
	 * Spawns a “numAgents’ of new agents, as passing arguments[i] (with arg_size) 
	 * to the i-th new agent upon a next call to Agents.manageAll( ).
	 * More specifically, spawn( ) changes the calling agent’s newChildren.
	 * @param numAgents
	 * @param arguments
	 */
	protected void spawn( int numAgents, Object[] arguments ) { 

		//Only want to make changes if the number to be created is above zero
		if ( numAgents > 0 ) {
			newChildren = numAgents;
			this.arguments = arguments.clone( );			
		}

	}
}