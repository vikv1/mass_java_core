/*

 	MASS Java Software License
	© 2012-2021 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:

	© 2012-2021 University of Washington. MASS was developed by Computing and Software Systems at University of
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

import java.util.Objects;
import java.util.Vector;

@SuppressWarnings("serial")
public class SmartAgent extends Agent {

    public static final int MIGRATE = 0;
    public static final int SPAWN_AGENTS = 1;
    public static final int KILL_PARENTS = 2;
    public static final int PROPAGATE_AGENTS = 3;
    public static final int KILL_DUPLICATES = 4;

    private double[] currentCoordinates;
    private double[] nextCoordinates;  //coordinates where agent is migrating to
    private int[] subIndex; //subIndex of sub-place where SpaceAgent resides
    private int generation;
    private int originalId;
    private boolean isParent = false; //flase when agent instantiated, once the agent spawns, it turns to true



    // private data members
    private int nextNode = -1;
    private int prevNode = -1;
    private boolean justMigrated = false;

    public SmartAgent(Object args) {

        // initialize SmartAgent
        super();
    }

    public SmartAgent( ) {

        // initialize SmartAgent
        super();
    }

    public Object migratePropagate( Object arg )
    {
        // if I'm the very first agent just moving to the source, the source node's
        // prevNode should be -2: no previous node.
        prevNode = ( getAgentId( ) == 0 && getPlace( ).getIndex( )[0] == 0 && ( ( SmartPlace )getPlace( ) ).footprint == -1 ) ? -2 : getPlace( ).getIndex( )[0];

        if (prevNode == -2)
        {
            justMigrated = true;
            migrate( nextNode );
            MASS.getLogger( ).debug( ": agent(" + getAgentId( ) + ") will migrate from " +
                    prevNode + " to " + nextNode );

            return null;
        }

        SmartPlace smartPlace = ( SmartPlace )getPlace( );

        if ( ( ( SmartPlace )getPlace( ) ).footprint == -1 ) {
            // This is the 1st arrival of the crawler.
            smartPlace.footprint = prevNode;

            // Check all the neighbors from the new node.
            int[] neighbors = smartPlace.neighbors;
            int[] distances = smartPlace.distances;

            // footprint == -2 means that I'm at the source node
            if ( smartPlace.footprint == -2 && neighbors.length == 0 ||
                    smartPlace.footprint != -2 && neighbors.length == 1 ) {
                MASS.getLogger( ).debug( "agent(" + getAgentId( ) +
                        ") terminated onArrival at a deadend "
                        + getPlace( ).getIndex( )[0] );
                kill( );
            }
            else {
                // Set my next node before spawning children.
                nextNode = ( neighbors[0] != prevNode ) ?
                        neighbors[0] : neighbors[1];

                // Spawn children to disseminate all the neighbors
                // except my previous and next nodes
                SmartArgs2Agents[] args
                        = new SmartArgs2Agents[( smartPlace.footprint == -2 ) ?
                        neighbors.length - 1:
                        neighbors.length - 2];

                for ( int i = 0, j = 0; i < neighbors.length; i++ ) {
                    if ( neighbors[i] == nextNode
                            || neighbors[i] == prevNode )
                        // skip the parent's next node or previous node
                        continue;
                    args[j++] = new SmartArgs2Agents( neighbors[i] );
                }
                spawn( args.length, args );
            }
        }
        else if ( smartPlace.footprint != prevNode && justMigrated ) {
            // Another crawler agent has already visited. No more crawler
            // dissemination
            MASS.getLogger( ).debug( "agent(" + getAgentId( ) +
                    ") terminated onArrival at the revisited node "
                    + getPlace( ).getIndex( )[0] );

            kill( );
            return null;
        }
        else {
            // if ( node.footprint == prevNode )
            justMigrated = false;
        }

        return null;
    }
}





