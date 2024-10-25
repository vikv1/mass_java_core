package edu.uw.bothell.css.dsl.mass.apps.ShortestPath;
import edu.uw.bothell.css.dsl.MASS.*;

public class Node extends Place {
    // function identifiers
    public static final int init_ = 0;

    public Object callMethod( int functionId, Object argument ) {
	switch( functionId ) {
	case init_: return init( argument );
	}
	return null;
    }

    public int[] neighbors = null;
    public int[] distances = null;
    public int footprint = -1;

    /**
     * Is the default constructor.
     */
    public Node( ) {
	super( );
    }

    public Node( Object arg ) {
    }

    public Object init( Object arg ) {
	// My network information
	int nNodes = getSize( )[0];
	int nodeId = getIndex( )[0];

	// Generate my neighbors and their distances
	Map myMap = new Map( nNodes, nodeId );
	neighbors = myMap.neighbors;
	distances = myMap.distances;

	return null;
    }
}
