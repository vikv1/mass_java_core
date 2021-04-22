package edu.uw.bothell.css.dsl.mass.apps.ShortestPath;
import java.util.*;

public class GraphGen {
    public static void main( String[] args ) {
	GraphGen g = new GraphGen( Integer.parseInt( args[0] ) );
    }

    public GraphGen( int nNodes ) {
	System.out.println( "nNodes = " + nNodes +"\n" );

	Map[] node = new Map[nNodes];
	for ( int i = 0; i < nNodes; i++ ) {
	    System.out.println( "Node " + i + ":" );
	    node[i] = new Map( nNodes, i );

	    for ( int j = 0; j < node[i].neighbors.length; j++ ) {
		System.out.println( "to " + node[i].neighbors[j]
				    + " with distance " 
				    + node[i].distances[j] );
	    }
		System.out.println( );
	}
    }
}
