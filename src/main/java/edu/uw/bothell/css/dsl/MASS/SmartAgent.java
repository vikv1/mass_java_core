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

    // private data members
    private int nextNode = -1;   //Next Destination Node for the Smart Agent
    private int prevNode = -1;   //Previous Node from which the Smart Agent migrated from
    private boolean justMigrated = false;
    public int[] itinerary = null; //Array that holds the navigation
    private int OriginalSourceNode = -1;
    public static final int BothBranch_ = 1;
    public static final int LeftBranch_ = 2;
    public static final int RightBranch_ = 3;

    public SmartAgent(Object args) {
        super();

        // initialize SmartAgent
        if (args != null)
        {
            SmartArgs2Agents arguments = (SmartArgs2Agents) args;

            this.itinerary = (arguments.itinerary != null) ? arguments.itinerary.clone() : null;
            this.nextNode = (arguments.nextNode != -1) ? arguments.nextNode : -1;
            this.prevNode = (arguments.prevNode != -1) ? arguments.prevNode : -1;

            MASS.getLogger().debug("SmartAgent(" + getAgentId() + ") was born, going to " +
                    " nextNode = " + nextNode);

            if (nextNode != -1) { //Setting the Next Index on the Agent for Migration.
                setNextIndex(nextNode);
            }
        }
    }

    public SmartAgent( ) {
        // initialize SmartAgent
        super();
    }

    //Method for initializing Smart Agent's source node and itinerary
    public void init( int place )
    {
        this.OriginalSourceNode = place; //Setting the original source where the Agent is first instantiated
        this.itinerary[0] = place;  //setting the starting place on the itinerary array
    }

    public int getNextNode( )
    {
        return nextNode;
    }

    public void setNextNode( int nextNode )
    {
        this.nextNode = nextNode;
    }

    public void setitinerary( int[] itinerary )
    {
        this.itinerary = itinerary.clone();
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


    public Object propagateDown( Object arg )
    {
        int currStep = ((Integer) arg).intValue();

        if (getPlace() != null && !(getPlace() instanceof VertexPlace)) {

            MASSBase.getLogger().error("Requested PropagateDown but places is {"
                    + getPlace() .getClass().getName() + "} and not VertexPlace.");

            return null;
        }

        // Retrieve the current node's information
        Object [] neighbors = ((VertexPlace) getPlace()).getNeighbors();

        // Retrieve the current node's information
        int currNodeGlobalIndex = getPlace().getIndex()[0];

        // Count the number of edges available to visit
        int availableEdges = 0;
        for (int i = 0; i < neighbors.length; i++) {
            int neighborGlobalIndex = (Integer)neighbors[i];

            if (neighborGlobalIndex < currNodeGlobalIndex)
                // Going to a neighbor with a lower id
                availableEdges++;
        }

        if (availableEdges == 0) {
            // No more edges to explore. I'm done
            MASS.getLogger().debug("Step " + currStep + ": agent(" + getAgentId() +
                    ") gets terminated at " + currNodeGlobalIndex);
            kill();
        } else {
            // Some edges to explore

            // Prepare arguments to be passed to children
            SmartArgs2Agents[] args = new SmartArgs2Agents[availableEdges -1];
            int argsCount = 0; // eventually reaches # children

            // Scan all neighbors of the current place.
            for (int i = 0; i < neighbors.length; i++) {
                int neighborGlobalIndex = (Integer)neighbors[i];

                if (neighborGlobalIndex < currNodeGlobalIndex) {
                    // Going to a neighbor with a lower id
                    if (--availableEdges == 0) {
                        // Parent takes the last available edge and also immediately migrates
                        itinerary[currStep + 1] = neighborGlobalIndex;
                        migrate(itinerary[currStep + 1]);
                    } else {
                        // Children take the first availableEdges - 1.
                        int[] childItinerary = itinerary.clone();
                        childItinerary[currStep + 1] = neighborGlobalIndex;
                        args[argsCount++] = new SmartArgs2Agents( childItinerary,neighborGlobalIndex);
                    }
                }
            }

            // Finally, spawn all my children
            if (args != null)
                spawn(args.length, args);
        }

        return null;
    }



    public Object propagateTree( int path, Object arg )
    {
        if (getPlace() != null && !(getPlace() instanceof VertexPlace)) {

            MASSBase.getLogger().error("Requested PropagateDown but places is {"
                    + getPlace() .getClass().getName() + "} and not VertexPlace.");

            return null;
        }

        // Retrieve the current node's information
        int currNodeGlobalIndex = getPlace().getIndex()[0];
        int left = ((VertexPlace)getPlace()).left;
        int right = ((VertexPlace)getPlace()).right;

        MASS.getLogger().debug("SmartAgent(" + getAgentId() + ") is at place "+getPlace().getIndex()[0] + " Left Node is " +left + "" +
                " and Right Node is " + right);

        //if there are no Left or Right Branches kill the Agent
        if (left == -1 && right  == -1) {
            // No more nodes to explore.
            MASS.getLogger().debug(" agent(" + getAgentId() +
                    ") has reached the leaf node and is terminated at " + currNodeGlobalIndex);
            kill();
        } else if(((VertexPlace)getPlace()).footprint == 1){
            MASS.getLogger().debug(" agent(" + getAgentId() +
                    ") has already visited " + currNodeGlobalIndex);
            kill();
        }
        else {
            switch (path){
                case BothBranch_:
                    if (left != -1 && right  != -1){
                        migrateAndSpawn(arg, left, right);
                    }
                    else if (left != -1){
                        migrate(left);
                        MASS.getLogger().debug("SmartAgent(" + getAgentId() + ") is at place " + getPlace().getIndex()[0]  +
                                "and is migrating to the Left Branch" + left);
                    }
                    else if (right != -1){
                        migrate(right);
                        MASS.getLogger().debug("SmartAgent(" + getAgentId() + ") is at place "+getPlace().getIndex()[0]+
                                "and is migrating to the Right Branch" + right);
                    }

                    break;
                case LeftBranch_:
                    if (left != -1){
                        migrate(left);   //Migrate the Agent to Left Branch
                        MASS.getLogger().debug("SmartAgent(" + getAgentId() + ") is at place " + getPlace().getIndex()[0]  +
                                "and is migrating to the Left Branch" + left);
                    }
                    break;
                case RightBranch_:
                    if (right != -1){
                        migrate(right);   //Migrate the Agent to Left Branch
                        MASS.getLogger().debug("SmartAgent(" + getAgentId() + ") is at place "+getPlace().getIndex()[0]+
                                "and is migrating to the Right Branch" + right);
                    }
            }

            ((VertexPlace)getPlace()).footprint = 1; //Setting the footprint since place is visited
        }
        return null;
    }

    private Object migrateAndSpawn( Object arg, int left, int right)
    {
        migrate(left); //Migrate Parent Agent to left

        // Spawn Child Agent to the right
        SmartArgs2Agents[] args = new SmartArgs2Agents[1];
        args[0] = new SmartArgs2Agents(SmartArgs2Agents.rangeSearch_, arg, right, -1 );
        spawn(args.length, args);

        MASS.getLogger().debug("SmartAgent(" + getAgentId() + ") is at place "+getPlace().getIndex()[0]+
                "and is migrating to the Left Branch " + left + " spawning to " + right);

        return null;
    }

    public Object migrateSource( Object arg )
    {
        int currStep = ((Integer) arg).intValue();

        if (getPlace() != null && !(getPlace() instanceof VertexPlace)) {

            MASSBase.getLogger().error("Requested PropagateDown but places is {"
                    + getPlace() .getClass().getName() + "} and not VertexPlace.");

            return null;
        }

        // Retrieve the current node's information
        Object [] neighbors = ((VertexPlace) getPlace()).getNeighbors();

        // Check if the current node has my original node as a neighbor
        for (int i = 0; i < neighbors.length; i++) {
            int neighborGlobalIndex = (Integer)neighbors[i];

            if (neighborGlobalIndex == itinerary[0]) { // YES
                itinerary[currStep + 1] = neighborGlobalIndex;
                break;
            }
        }
        if (itinerary[currStep + 1] == -1) { // NO
            MASS.getLogger().debug("Step " + currStep +
                    ": agent(" + getAgentId() + ") can't go home at " +
                    itinerary[0] + " and thus gets terminated at " +
                    getPlace().getIndex()[0]);
            kill();
        }

        return null;

    }

}