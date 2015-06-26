package edu.uw.bothell.css.dsl.MASS;

import java.util.Collections; // for synchronized set
import java.util.HashSet;     // implementation for Agent bag
import java.util.Set;         // local Agent bag
import java.util.Vector;

public class Place {

	private int[] size;
	private int[] index;
	private Object outMessage = null;
	private Object[] inMessages = null;
	private Set<Agent> agents = Collections.synchronizedSet( new HashSet<Agent>( ) );
	private Vector< int[] > neighbours = null;

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

	public Object getDebugData(){
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

	public Object getOutMessage() {
		return outMessage;
	}

	protected Object getOutMessage( int handle, int[] offset_index ) {

		Place dstPlace = findDstPlace( handle, offset_index );

		// return the destination outMessage
		return ( dstPlace != null ) ? dstPlace.outMessage : null;
	
	}

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