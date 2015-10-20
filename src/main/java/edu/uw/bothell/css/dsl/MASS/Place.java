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

import java.util.Collections; // for synchronized set
import java.util.HashSet;     // implementation for Agent bag
import java.util.Set;         // local Agent bag
import java.util.Vector;

/**
 *	Place represents a single element from a collection of places distributed
 *	among all cluster nodes. A Place may contain a collection of Agents that
 *	perform operations on objects contained within the Place. 
 *
 */
public class Place {

	/**
	  * Defines the size of the matrix that consists of application-specific
	  * places. Intuitively, size[0], size[1], and size[2] correspond to the size
	  * of x, y, and z, or that of i, j, and k.
	  */
	private int[] size;
	
	 /**  
	  * Is an array that maintains each place’s coordinates. Intuitively,
	  * index[0], index[1], and index[2] correspond to coordinates of x, y, and
	  * z, or those of i, j, and k. 
	  */
	private int[] index;
	
	 /** Stores a set arguments to be passed to a set of remote-cell functions
	  * that will be invoked by exchangeAll( ) or exchangeSome( ) in the
	  * nearest future. The argument size must be specified with
	  * outMessage_size. 
	  */
	private Object outMessage = null;
	
	 /** Receives a return value in inMessages[i] from a function call made to
	  * the i-th remote cell through exchangeAll( ) and exchangeSome( ).
	  * Each element size must be specified with inMessage_size. 
	  */
	private Object[] inMessages = null;
	
	/** Includes all the agents residing locally on this place. */
	private Set<Agent> agents = Collections.synchronizedSet( new HashSet<Agent>( ) );
	
	private Vector< int[] > neighbours = null;

	/**
	 * Is called from Places.callAll( ), callSome( ), exchangeAll( ), and
	 * exchangeSome( ), and invoke the function specified with functionId as
	 * passing arguments to this function. A user-derived Place class must
	 * implement this method.
	 * @param functionId
	 * @param argument
	 * @return 
	 */
	public Object callMethod( int functionId, Object argument ) {
		return null;
	}

	private Place findDstPlace( int handle, int offset[] ) {

		// compute the global linear index from offset[]
		Places_base places = MASS_base.getPlacesMap().get( new Integer( handle ) );
		int[] neighborCoord = new int[places.getSize().length];
		places.getGlobalNeighborArrayIndex( index, offset, places.getSize(),
				neighborCoord );
		int globalLinearIndex
		= places.getGlobalLinearIndexFromGlobalArrayIndex( neighborCoord,
				places.getSize() );

		if ( globalLinearIndex == Integer.MIN_VALUE )
			return null;

		// identify the destination place  
		int destinationLocalLinearIndex
		= globalLinearIndex - places.getLowerBoundary();

		Place dstPlace = null;
		int shadow_index;
		if ( destinationLocalLinearIndex >= 0 &&
				destinationLocalLinearIndex < places.getPlacesSize() )
			dstPlace = places.getPlaces()[ destinationLocalLinearIndex ];
		else if ( destinationLocalLinearIndex < 0 &&
				( shadow_index = destinationLocalLinearIndex + 
				places.getShadowSize() ) >= 0 )
			dstPlace = places.getLeftShadow()[ shadow_index ];
		else if ( (shadow_index = 
				destinationLocalLinearIndex - places.getPlacesSize()) >= 0
				&& shadow_index < places.getShadowSize() )
			dstPlace = places.getRightShadow()[ shadow_index ];

		return dstPlace;
	
	}

	public synchronized Set<Agent> getAgents() {
		return agents;
	}

	public Number getDebugData()
	{
		return null;
	}

	public int[] getIndex() {
		return index;
	}

	public Object[] getInMessages() {
		return inMessages;
	}

	public Vector<int[]> getNeighbours() {
		return neighbours;
	}

	public void setNeighbours(Vector<int[]> neighbours)
	{
		this.neighbours = neighbours;
	}

	public Object getOutMessage() {
		return outMessage;
	}

	protected Object getOutMessage( int handle, int[] offset_index ) {

		Place dstPlace = findDstPlace( handle, offset_index );

		// return the destination outMessage
		return ( dstPlace != null ) ? dstPlace.outMessage : null;
	
	}

	/**
	 * Returns the size of the matrix that consists of application-specific
	 * places. Intuitively, size[0], size[1], and size[2] correspond to the size
	 * of x, y, and z, or that of i, j, and k.
	 * @return 
	 */
	public int[] getSize() {
		return size;
	}

	protected void putInMessage( int handle, int[] offset_index, int position, Object value ) {

		Place dstPlace = findDstPlace( handle, offset_index );

		// write to the destination inMessage[position]
		if ( dstPlace != null && position < dstPlace.inMessages.length )
			dstPlace.inMessages[position] = value;
	
	}

	public void setIndex(int[] index) {
		this.index = index.clone();
	}

	// To be overridden by developer - for debugging
	public void setDebugData(Object argument) {
	}

	public void setInMessages(Object[] inMessages) {
		this.inMessages = inMessages;
	}

	public void setOutMessage(Object outMessage) {
		this.outMessage = outMessage;
	}

	public void setSize(int[] size) {
		this.size = size.clone();
	}
	
}