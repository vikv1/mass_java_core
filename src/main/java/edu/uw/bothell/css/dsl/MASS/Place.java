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
import java.util.Collections;
import java.util.HashSet;     // implementation for Agent bag
import java.util.Set;         // local Agent bag
import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

/**
 *	Place represents a single element from a collection of places distributed
 *	among all cluster nodes. A Place may contain a collection of Agents that
 *	perform operations on objects contained within the Place. 
 *
 */
@SuppressWarnings("serial")
public class Place implements Serializable {

	private int[] index;
	private Object outMessage = null;
	private Object[] inMessages = null;
	
	// Includes all the agents residing locally on this place. Synchronized set is NOT serializable !
	private transient Set<Agent> agents = Collections.synchronizedSet( new HashSet<Agent>( ) );
	
	private Vector< int[] > neighbors = null;

	private boolean visited;

	
	public Place() {
		
		// TODO - hack! "pulling" index is bad practice - should be supplied during init
		if ( MASS.getCurrentPlacesBase() != null ) index = MASS.getCurrentPlacesBase().getNextIndex();
		
	}
	
	/**
	 * Is called from Places.callAll( ), callSome( ), exchangeAll( ), and
	 * exchangeSome( ), and invoke the function specified with functionId as
	 * passing arguments to this function. A user-derived Place class must
	 * implement this method.
	 * @param functionId The ID number of the function to invoke
	 * @param argument An argument that will be passed to the invoked function
	 * @return Always returns NULL
	 */
	public Object callMethod( int functionId, Object argument ) {
		return null;
	}

	private Place findDstPlace( int handle, int offset[] ) {

		// compute the global linear index from offset[]
		PlacesBase places = MASSBase.getPlacesMap().get( new Integer( handle ) );
		int[] neighborCoord = new int[places.getSize().length];
		places.getGlobalNeighborArrayIndex( index, offset, places.getSize(),
				neighborCoord );
		int globalLinearIndex = MatrixUtilities.getLinearIndex( places.getSize(), neighborCoord );
		if ( globalLinearIndex == Integer.MIN_VALUE ) return null;

		// identify the destination place  
		int destinationLocalLinearIndex
		= globalLinearIndex - places.getLowerBoundary();

		Place destintationPlace = null;
		int shadowIndex;
		if ( destinationLocalLinearIndex >= 0 &&
				destinationLocalLinearIndex < places.getPlacesSize() )
			destintationPlace = places.getPlaces()[ destinationLocalLinearIndex ];
		else if ( destinationLocalLinearIndex < 0 &&
				( shadowIndex = destinationLocalLinearIndex + 
				places.getShadowSize() ) >= 0 )
			destintationPlace = places.getLeftShadow()[ shadowIndex ];
		else if ( (shadowIndex = 
				destinationLocalLinearIndex - places.getPlacesSize()) >= 0
				&& shadowIndex < places.getShadowSize() )
			destintationPlace = places.getRightShadow()[ shadowIndex ];

		return destintationPlace;
	
	}

	/**
	 * Get the collection of Agents residing locally on this place
	 * <p>
	 * Important: Synchronized set is NOT serializable! Therefore when agent is de-serialized
	 * the place field of agent must be re-assigned. Otherwise you will get an exception when you
	 * call [agent instance].getPlace().getAgents()
	 *  
	 * @return The Agents residing in this instance of Place
	 */
	public synchronized Set<Agent> getAgents() {
		return agents;
	}
	
	/**
	 * Get the number of Agents residing in this Place
	 * @return The number of Agents associated with this Place
	 */
	public int getNumAgents() {
		return agents.size();
	}

	/**
	 * To be overridden by a developer - return debug data from this Place
	 * @return Debug data contained by this Place
	 */
	public Number getDebugData()
	{
		return null;
	}

	/**
	 * To be overridden by a developer - set debug data for this Place
	 * @param argument Debug data for this Place
	 */
	public void setDebugData(Number argument) {
	}

	/**  
	 * Get the array that maintains each place’s coordinates. Intuitively,
	 * index[0], index[1], and index[2] correspond to coordinates of x, y, and
	 * z, or those of i, j, and k.
	 * @return The coordinates of this Place as an array of indices 
	 */
	public int[] getIndex() {
		return index;
	}

	/** 
	 * Get incoming Messages received by this Place
	 * @return Received incoming messages 
	 */
	public Object[] getInMessages() {
		return inMessages;
	}

	/**
	 * Get a collection of indexes representing the location of neighboring Places
	 * @return Index arrays representing neighboring Places
	 */
	public Vector<int[]> getNeighbours() {
		return neighbors;
	}

	/**
	 * Set the collection of indexes representing the location of neighboring Places
	 * @param neighbors Index arrays representing neighboring Places
	 */
	public void setNeighbors(Vector<int[]> neighbors)
	{
		this.neighbors = neighbors;
	}

	/** 
	 * Get the arguments to be passed to a set of remote-cell functions
	 * that will be invoked by exchangeAll( ) or exchangeSome( ) in the
	 * nearest future.
	 * @return The message to be passed during exchange 
	 */
	protected Object getOutMessage() {
		return outMessage;
	}

	/**
	 * Get the out Message destined for a specific Place, given by the offsetIndex
	 * @param handle The handle ID of the Place
	 * @param offsetIndex The offset index
	 * @return The message intended for the specified Place/Index
	 */
	public Object getOutMessage( int handle, int[] offsetIndex ) {

		Place dstPlace = findDstPlace( handle, offsetIndex );

		// return the destination outMessage
		return ( dstPlace != null ) ? dstPlace.outMessage : null;
	
	}

	/**
	 * Returns the size of the matrix that consists of application-specific
	 * places. Intuitively, size[0], size[1], and size[2] correspond to the size
	 * of x, y, and z, or that of i, j, and k.
	 * @return Matrix size
	 */
	public int[] getSize() {
		return MASSBase.getCurrentPlacesBase().getSize();
	}

	/**
	 * Get the visit status - i.e. if this Place has been visited by an Agent
	 * @return TRUE if visited by an Agent
	 */
	public boolean getVisited()
	{
		return visited;
	}

	protected void putInMessage( int handle, int[] offsetIndex, int position, Object value ) {

		Place dstPlace = findDstPlace( handle, offsetIndex );

		// write to the destination inMessage[position]
		if ( dstPlace != null && position < dstPlace.inMessages.length )
			dstPlace.inMessages[position] = value;
	
	}

	 /**  
	  * Set the array that maintains each place’s coordinates. Intuitively,
	  * index[0], index[1], and index[2] correspond to coordinates of x, y, and
	  * z, or those of i, j, and k.
	  * @param index The coordinates of this Place as an array of indices 
	  */
	protected void setIndex(int[] index) {
		this.index = index.clone();
	}

	/**
	 * To be overridden by a developer - set debug data for this Place
	 * @param argument Debug data for this Place
	 */
	public void setDebugData(Object argument) {
	}

	/** 
	 * Set incoming Messages received by this Place
	 * @param inMessages Collection of incoming Messages 
	 */
	public void setInMessages(Object[] inMessages) {
		this.inMessages = inMessages;
	}

	/** 
	 * Stores a set of arguments to be passed to a set of remote-cell functions
	 * that will be invoked by exchangeAll( ) or exchangeSome( ) in the
	 * nearest future.
	 * @param outMessage The message to be passed during exchange 
	 */
	public void setOutMessage(Object outMessage) {
		this.outMessage = outMessage;
	}

	/**
	 * Set the size of the matrix that consists of application-specific places.
	 * @param size Matrix size
	 */
	@Deprecated
	protected void setSize(int[] size) {
		// Cannot change matrix size here - PlacesBase is the authority for this
	}

	/**
	 * Set the visit status of this Place by an Agent
	 * @param visited Set TRUE if visited by an Agent
	 */
	public void setVisited(boolean visited)
	{
		this.visited = visited;
	}
	
}