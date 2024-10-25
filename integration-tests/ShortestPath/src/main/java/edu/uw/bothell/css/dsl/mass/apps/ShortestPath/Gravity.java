package edu.uw.bothell.css.dsl.mass.apps.ShortestPath;
import edu.uw.bothell.css.dsl.MASS.*;
import java.io.*;

public class Gravity extends Agent implements Serializable {
    // function identifiers
    public static final int onArrival_ = 0;
    public static final int departure_ = 1;

    public Object callMethod( int functionId, Object argument ) {
	switch( functionId ) {
	case onArrival_: return onArrival( argument );
	case departure_: return departure( argument );
	}
	return null;
    }

    /**
     * Is the default constructor.
     */
    public Gravity( ) {
	super( );
    }

    /**
     * Is the actual constructor.
     * @param arg an integer array whose elements are:
     *            0th: the final destination node index,
     *            1st: the next node index, and
     *            2nd: the departure time from the current node
     */
    public Gravity( Object arg ) {
	Args2Agents arguments = ( Args2Agents )arg;
	source = arguments.source;
	destination = arguments.destination;
	nextNode    = arguments.nextNode;
	deptTime    = arguments.deptTime;

	MASS.getLogger( ).debug( "agent(" + getAgentId( ) + ") was born." +
				 " destination = " + destination +
				 " nextNode = " + nextNode +
				 " deptTime = " + deptTime );
    }

    /**
     * @param  arg an Integer object that indicates the current simulation time
     * @return     the nearest departure time to go to the next node
     */
    public Object onArrival( Object arg ) {
	int currTime = ( ( Integer )arg ).intValue( );
	int minDeptTime = Integer.MAX_VALUE;

	if ( ( ( Node )getPlace( ) ).footprint == -1 ) {
	    // This is the 1st arrival of the gravity.
	    ( ( Node )getPlace( ) ).footprint = prevNode;
	    if ( getPlace( ).getIndex( )[0] == destination ) {
		// Arrived at the final destination.
		MASS.getLogger( ).debug( "Shortest Path from " +  source +
			       " to " + destination +
			       " is " + deptTime );
		kill( );
		return ( Object )( new Integer( -1 ) );
	    }

	    // Check all the neighbors from the new node.
	    int[] neighbors = ( ( Node )getPlace( ) ).neighbors;
	    int[] distances = ( ( Node )getPlace( ) ).distances;
	    
	    if ( deptTime == 0 && neighbors.length == 0 ||
		 deptTime != 0 && neighbors.length == 1 ) {
		// deptTime != 0 means I've started travelling.
		// This neighbor is my previous node.
		// Thus, encountered a deadend.
		MASS.getLogger( ).debug( "agent(" + getAgentId( ) +
					 ") terminated onArrival at currTime = " +
					 currTime +
					 " deptTime = " + deptTime +
					 " at deadend node " + getPlace( ).getIndex( )[0] );
		kill( );
		return new Integer( Integer.MAX_VALUE );
	    }
	    else {
		// Set my next node before spawning children.
		nextNode = ( neighbors[0] != prevNode ) ? 
		    neighbors[0] : neighbors[1];
		int parentDeptTime = minDeptTime = 
		    currTime + ( ( neighbors[0] != prevNode ) ? 
				 distances[0] : distances[1] );

		MASS.getLogger( ).debug( "parent agent(" + getAgentId( ) + ")" +
					 " nextNode = " + nextNode +
					 " parentDeptTime = " + parentDeptTime +
					 " neighbors.length = " + neighbors.length
					 );

		if ( deptTime == 0 && neighbors.length > 1 ||
		     deptTime > 0 && neighbors.length > 2 ) {
		    // Spawn children to disseminate all the neighbors
		    // except my previous and next nodes
		    Args2Agents[] args
			= new Args2Agents[(deptTime == 0) ?
					  neighbors.length - 1:
					  neighbors.length - 2];

		    for ( int i = 0, j = 0; i < neighbors.length; i++ ) {
			if ( neighbors[i] == nextNode 
			     || neighbors[i] == prevNode )
			    // skip the parent's next node or previous node
			    continue;
			int deptTime_i = currTime + distances[i];
			minDeptTime = Math.min( minDeptTime, deptTime_i );
			args[j++] = new Args2Agents( source, destination,
						   neighbors[i], deptTime_i );
		    }
		    spawn( args.length, args );
		}
		this.deptTime = parentDeptTime;
	    }
	}
	else if ( ( ( Node )getPlace( ) ).footprint != prevNode && justMigrated ) {
	    // Another gravity agent has already visited. No more gravity
	    // dissemination
	    MASS.getLogger( ).debug( "agent(" + getAgentId( ) +
				     ") terminated onArrival at currTime = " +
				     currTime +
				     " deptTime = " + deptTime +
				     " at revisited node " + getPlace( ).getIndex( )[0] );
	    kill( );
	    return new Integer( Integer.MAX_VALUE );
	}
	else {
	    // if ( getPlace( ).footprint == prevNode )
	    //  wait for my future departure
	    justMigrated = false;
	    minDeptTime = deptTime;
	}

	// return my departure time or child's nearest departure time.
	return new Integer( minDeptTime );
    }

    /**
     * Migrates to a next node if the current time == my departure time,
     * otherwise waits until then.
     * @param  arg an Integer object that indicates the current simulation time
     * @return     nothing
     */
    public Object departure( Object arg ) {
	int currTime = ( ( Integer )arg ).intValue( );
		       
	if ( currTime == deptTime ) {
	    // Time to disseminate the gravie to my next neighbor
	    prevNode = getPlace( ).getIndex( )[0];
	    justMigrated = true;
	    migrate( nextNode );
	    MASS.getLogger( ).debug( "Time " + currTime + ": agent(" + getAgentId( ) +
				     ") will migrate from " +
				     prevNode + " to " + nextNode );
	}
	else {
	    MASS.getLogger( ).debug( "Time " + currTime + ": agent(" + getAgentId( ) +
				     " )waits for time = " + deptTime );
	}
	// else wait till currTime == deptTime

	return null;
    }

    // private data members
    private int source = -1;
    private int destination = -1;
    private int nextNode = -1;
    private int prevNode = -1;
    private int deptTime = -1;

    private boolean justMigrated = false;
}
