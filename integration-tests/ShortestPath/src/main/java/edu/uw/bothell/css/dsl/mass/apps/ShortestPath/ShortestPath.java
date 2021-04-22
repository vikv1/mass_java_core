package edu.uw.bothell.css.dsl.mass.apps.ShortestPath;
import edu.uw.bothell.css.dsl.MASS.*;
import edu.uw.bothell.css.dsl.MASS.logging.LogLevel;
import java.util.*;

/**
 * ShortestPath.java - a strings-N'-pins based shortest path program
 * @author  Munehiro Fukuda <mfukuda@uw.edu>
 * @version 1.0
 * @since   Sept. 26, 2017
 */
public class ShortestPath {
    /**
     * Computes the shortest path by dispatching gravity agents along all
     * edges from the start node.
     * @param args TBD
     */
    public static void main( String[] args ) {

	int statSteps = 0;
	int statAgents = 0;

	// Read and validate input parameters
	if ( args.length != 5 ) {
            System.err.println( "args = " + args.length + " should be 4:" +
				" nNodes, source, dest, nProcs, nThrs" );
            System.exit( -1 );
        }
        int nNodes = Integer.parseInt( args[0] );
        int src = Integer.parseInt( args[1] );
        int dst = Integer.parseInt( args[2] );
        if ( nNodes < 1 || src >= nNodes || dst >= nNodes ) {
            System.err.println( "Nodes(" + nNodes + "), src(" + src
                                + "), and dst(" + dst + ") should be "
                                + "> 0, < nNodes, and < nNodes respectively.");
            System.exit( -1 );
        }

	// Set up arguments to MASS.init( );
	String[] arguments = new String[4];
	arguments[0] = "dslab";
	arguments[1] = "ignored";
	arguments[2] = "nodes.xml";
	arguments[3] = "12345";
	int nProc = Integer.parseInt( args[3] );
	int nThr = Integer.parseInt( args[4] );
	System.out.println( "nNodes = " + nNodes + " src = " + src +
			    " dst = " + dst + " nProc = " + nProc +
			    " nThr = " + nThr );
	MASS.setLoggingLevel( LogLevel.DEBUG );
	MASS.init( arguments, nProc, nThr );

	// Create a map
	Places network = new Places( 1, Node.class.getName( ), null, nNodes );
	network.callAll( Node.init_ );

	// Time measurement starts
	System.out.println( "Go!" );
	long startTime = System.currentTimeMillis( );
	
	// Instantiate the very first gravity agent.
	Args2Agents args2agents = new Args2Agents( src, dst, src, 0 );
	Agents gravity = new Agents( 2, Gravity.class.getName( ), ( Object )args2agents,
				     network, 1 );

	int nextEvent = -1;
	for ( int time = 0; ; time = nextEvent ) {
	    Object currTime = ( Object )(new Integer( time ) );
	    statSteps++;
	    // System.out.println( "*** time = " + currTime + "************" );

	    // STEP 1: have each agent migrate along a different edge 
	    // emanating from the current node
	    // at time == 0, the very first agent hops to the origin
	    // System.out.println( "... step 1: migration .................." );
	    gravity.callAll( Gravity.departure_, currTime );
	    gravity.manageAll( );
	    
	    // STEP2: have each agent spawn its children if a new node 
	    // hasn't been visited yet.
	    // System.out.println( "... step 2: spawn ......................" );
	    Object[] args4callAll = new Object[gravity.nAgents( )];
	    statAgents += gravity.nAgents( ); // for statistics
	    for ( int i = 0; i < args4callAll.length; i++ )
		args4callAll[i] = currTime;
	    Object[] allEvents
		= ( Object[] )gravity.callAll( Gravity.onArrival_,
					       args4callAll );
	    
	    if ( ( nextEvent = minInt( allEvents ) ) == -1 ) {
			System.out.println( "the shortest path = " + time );
			break; // Simulation finished
	    }
	    gravity.manageAll( );
	}

	// Stats
	System.out.println( "Statistics: #steps = " + statSteps +
			    " #agents = " + statAgents );
	
	MASS.finish( );
    }

    /**
     * Returns the mininum value of a given integer array.
     *
     * @param  array an array of integers
     * @return       the minimum value of array elements
     */
    private static int minInt( Object[] array ) {
	int min = Integer.MAX_VALUE;
	for ( Object o : array )
	    if ( ( Integer )o < min ) min = ( Integer )o;
	return min;
    }
}
