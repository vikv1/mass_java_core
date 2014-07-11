package MASS;

import java.util.Vector;

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

    private Place findDstPlace( int handle, int index[] ) {
	// compute the global linear index from offset[]
	// identify the destination place  

	return null;
    }

    public int[] size; // TODO: should be final
    public int[] index;// TODO: should be final
    public Object outMessage = null;
    public Object[] inMessages = null;
    Vector<Agent> agents = new Vector<Agent>( );


}