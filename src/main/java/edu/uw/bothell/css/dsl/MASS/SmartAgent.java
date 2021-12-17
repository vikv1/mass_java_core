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

        super();
        // initialize SmartAgent
        SmartArgs2Agents arguments = ( SmartArgs2Agents )args;
        nextNode = arguments.nextNode;
        prevNode = arguments.prevNode;
        MASS.getLogger( ).debug( "SmartAgent(" + getAgentId( ) + ") was born, going to " +
                " nextNode = " + nextNode );

        setNextIndex(nextNode); //Setting the Next Index on the Agent for Migration.
    }

    public SmartAgent( ) {

        // initialize SmartAgent
        super();
    }

    public int getNextNode( )
    {
        return nextNode;
    }

    public Object migratePropagate( Object arg )
    {
        SmartPlace smartPlace = ( SmartPlace )getPlace( ); //Get the place where the agent is

        if ( smartPlace.footprint == -1 ) {
            // Check all the neighbors from the new node.
            int[] neighbors = smartPlace.neighbors;
            int[] distances = smartPlace.distances;

            MASS.getLogger( ).debug("Number of Neighbors are "+neighbors.length);

            if (neighbors.length == 0 || (neighbors.length == 1 && prevNode == neighbors[0])) //If no Neighbors or if the only neighbor is previous node, Kill the Agent
            {
                MASS.getLogger( ).debug( "agent(" + getAgentId( ) +
                        ") terminated onArrival at a deadend "
                        + getPlace( ).getIndex( )[0] );

                smartPlace.footprint = 1; //This place has been visited
                kill( );
                return null;
            }

            // Set my next node before spawning children.
            nextNode = ( neighbors[0] != prevNode ) ? neighbors[0] : neighbors[1];
            MASS.getLogger( ).debug( "Migration: Agent(" + getAgentId( ) + ") will migrate from " + smartPlace.getIndex( )[0] + " to " + nextNode );
            migrate( nextNode ); //Migarte to the next Node

            // Spawn children to disseminate all the neighbors
            // except my previous and next nodes
            SmartArgs2Agents[] args
                    = new SmartArgs2Agents[( getAgentId( ) == 0 && getPlace( ).getIndex( )[0] == 0 && ( ( SmartPlace )getPlace( ) ).footprint == -1 ) ?
                    neighbors.length - 1:
                    neighbors.length - 2];

            MASS.getLogger( ).debug( "Number of Neighbor Argument is : " +args.length);

            if (args.length == 0) {
                //Before the Agent moves set the prevNode
                prevNode = getPlace( ).getIndex( )[0];
                smartPlace.footprint = 1; //This place has been visited
                return null; //if there are no neighbours to spawn, just return
            }

            for ( int i = 0, j = 0; i < neighbors.length; i++ ) {
                if ( neighbors[i] == nextNode || neighbors[i] == prevNode ) // skip the parent's next node or previous node
                    continue;
                MASS.getLogger( ).debug( "Neighbor is "+neighbors[i]+" i is:"+i+" Next Node is: "+nextNode+" Prev Node is: "+prevNode);
                args[j++] = new SmartArgs2Agents( neighbors[i],getPlace( ).getIndex( )[0]);
            }

            spawn( args.length, args );

            //Before the Agent moves or Spawns set the prevNode
            prevNode = getPlace( ).getIndex( )[0];
            smartPlace.footprint = 1; //This place has been visited

        }else {
            MASS.getLogger( ).debug("Place"+smartPlace.getIndex( )[0]+"Has been Visited before"+"agent(" + getAgentId( ) + ") terminated onArrival");
            kill( );
        }

        return null;
    }

}