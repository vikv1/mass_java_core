package edu.uw.bothell.css.dsl.MASS;

import java.util.Collections; // for synchronized set
import java.util.HashSet;     // implementation for Agent bag
import java.util.Set;         // local Agent bag

public class Place {
    public Object callMethod( int functionId, Object argument ) {
	return null;
    }

    protected Object getOutMessage( int handle, int[] offset_index ) {
	Place dstPlace = findDstPlace( handle, offset_index );

	// return the destination outMessage
	return ( dstPlace != null ) ? dstPlace.outMessage : null;
    }

    protected void putInMessage( int handle, int[] offset_index, int position,
				 Object value ) {
	Place dstPlace = findDstPlace( handle, offset_index );

	// write to the destination inMessage[position]
	if ( dstPlace != null && position < dstPlace.inMessages.length )
	    dstPlace.inMessages[position] = value;
    }

    private Place findDstPlace( int handle, int offset[] ) {
	// compute the global linear index from offset[]
	Places_base places = MASS_base.placesMap.get( new Integer( handle ) );
	int[] neighborCoord = new int[places.size.length];
	places.getGlobalNeighborArrayIndex( index, offset, places.size,
					    neighborCoord );
	int globalLinearIndex
	    = places.getGlobalLinearIndexFromGlobalArrayIndex( neighborCoord,
							       places.size );

	if ( globalLinearIndex == Integer.MIN_VALUE )
	    return null;

	// identify the destination place  
	int destinationLocalLinearIndex
	    = globalLinearIndex - places.lower_boundary;
	
	Place dstPlace = null;
	int shadow_index;
	if ( destinationLocalLinearIndex >= 0 &&
	     destinationLocalLinearIndex < places.places_size )
	    dstPlace = places.places[ destinationLocalLinearIndex ];
	else if ( destinationLocalLinearIndex < 0 &&
		  ( shadow_index = destinationLocalLinearIndex + 
		    places.shadow_size ) >= 0 )
	    dstPlace = places.left_shadow[ shadow_index ];
	else if ( (shadow_index = 
		   destinationLocalLinearIndex - places.places_size) >= 0
		  && shadow_index < places.shadow_size )
	    dstPlace = places.right_shadow[ shadow_index ];
	
	return dstPlace;
    }

    public final int[] size;
    public final int[] index;
    public Object outMessage = null;
    public Object[] inMessages = null;
    public Set<Agent> agents = Collections.synchronizedSet(
        new HashSet<Agent>( ) );

    public Place( ) {
	size = Places.placeInitSize.clone( );
	index = Places.placeInitIndex.clone( );
    }
}
