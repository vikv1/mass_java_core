package edu.uw.bothell.css.dsl.mass.apps.ShortestPath;
import java.util.*;

public class Dijkstra {
    static boolean stats = false;
    static int nSteps = 0;
    static int minFixed = 0, totalMinFixed = 0;
    static int neighbors = 0, totalNeighbors = 0;
    
    public static void main( String[] args ) {

	// Read and validate input parameters
	if ( args.length < 3 ) {
	    System.err.println( "args = " + args.length +
				" should be 3 or 4: #vertices source destination [y]" );
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
	stats = ( args.length == 4 );
	System.out.println( "Nnodes = " + nNodes + " src = " + src +
			    " dst = " + dst + " stats = " +
			    ( stats ? "yes" : "no" ) );

	// Create a map
	Map[] map = new Map[nNodes];
        for ( int i = 0; i < nNodes; i++ ) {
            // System.out.println( "Node " + i + ":" );
            map[i] = new Map( nNodes, i );

	    /*
            for ( int j = 0; j < map[i].neighbors.length; j++ ) {
                System.out.println( "to " + map[i].neighbors[j]
                                    + " with distance "
                                    + map[i].distances[j] );
            }
	    System.out.println( );
	    */
        }

	// Time measurement starts
	System.out.println( "Go!" );
	long startTime = System.currentTimeMillis( );
	
	// Start Dijkstra
	Dijkstra d = new Dijkstra( map, src, dst );
	System.out.println( "The shortest path from " + src + " to " + dst
			    + " is " + d.final_distance );

	// Time measurement ends
	long elapsedTime = System.currentTimeMillis( ) - startTime;
	System.out.println( "Elapsed time = " + elapsedTime );

	// Print out statistics for each step
	if ( stats )
	    System.out.println( "nStep = " + nSteps +
				": total minFiexed scanned = " + totalMinFixed +
				", total neighbors scanned = " + totalNeighbors );
    }

    public int final_distance = -1;

    // the code modified from http://nw.tsuda.ac.jp/lec/dijkstra/
    public Dijkstra( Map[] map, int src, int dst ) {
	boolean[] fixed = new boolean[map.length]; // show if node[i] is fixed.
	int[] distance = new int[map.length];      // distance to node[i]

	// initialize the distance to each node
	for ( int i = 0; i < map.length; i++ ) {
	    distance[i] = Integer.MAX_VALUE;
	    fixed[i] = false;
	}

	distance[src] = 0; // going to the source node is always 0.

	// System.out.println( "start from " + src );
	for ( nSteps = 0; true; nSteps++ ) {
	    // System.out.println( "+++++++++++++++++++++++++++++++++++++++" );
	    // identify the unfixed node with the minimum distance so far
	    int marked = minIndex( distance, fixed );
	    // System.out.println( "minIndex returned " + marked );
	    if ( marked < 0 ) { 
		final_distance = distance[dst];
		return; // all nodes have been fixed.
	    }
	    if ( distance[marked] == Integer.MAX_VALUE )
		return; // this node is a singleton.
	    // this node is now fixed.
	    fixed[marked] = true;
	    if ( marked == dst ) {
		// System.out.println( "reached dst " + dst );
		final_distance = distance[dst];
		return; // the destination node has been fixed.
	    }

	    // for each neighboring node from this new fixed node
	    for ( int i = 0; i < map[marked].distances.length; i++ ) {
		if ( map[marked].distances[i] > 0 
		     && !fixed[map[marked].neighbors[i]] ) { // not fixed
		    // System.out.print( "now updating the distance to " 
		    //			+ map[marked].neighbors[i] );
		    // calculate the distance there from the fixed node
		    int newDistance 
			= distance[marked] + map[marked].distances[i];
		    // if it's shorter, let's replace the old wth the newer.
		    if ( newDistance < distance[map[marked].neighbors[i]] ) 
			distance[map[marked].neighbors[i]] = newDistance;
		    // System.out.println( " ... " 
		    //		+ distance[map[marked].neighbors[i]] );
		}
	    }
	    totalNeighbors += neighbors = map[marked].distances.length;
	    if ( false )
		System.out.println( "step = " + nSteps +
				    ": minFiexed = " + minFixed +
				    ", neighbrs[" + marked + "] = " +
				    neighbors );
	}
    }

    static int minIndex( int[] distance, boolean[] fixed ) {
	int index = 0;
	// choose an unfixed node.
	for ( ; index < fixed.length; index++ )
	    if ( !fixed[index] )
		break;
	// if no unfixed node exists, return -1.
	if ( index == fixed.length )
	    return -1;

	// find the unfixed node with the minimum distance
	for ( int i = index + 1; i < fixed.length; i++ )
	    if ( ! fixed[i] && distance[i] < distance[index] )
		index = i;
	totalMinFixed += minFixed = fixed.length;
	
	return index;
    }
}
