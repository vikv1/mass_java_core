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

/**
 * Class for BinaryTreePlaces. 
 * BinaryTreePlaces manages all Binary Tree Place elements within the simulation space.
 * 
 */
package edu.uw.bothell.css.dsl.MASS;

import java.util.Arrays;
import java.util.Vector;

public class BinaryTreePlaces extends BinaryTreePlacesBase {

	//the value for data partition
	double[] pivot = null;

    // constructor of BinaryTreePlaces
	public BinaryTreePlaces(int handle, String className, String filename, Object argument) {
		super(handle, className, filename);		
		init_master_binaryTree(argument, 0); 
	}
    
    /**
	 * add a new Place to the tree
	 */
	public boolean addPlace(BinaryTreePlace newPlace) {

		// if the node is in master computing node's boundary
		if (BinaryTreeUtilities.withinBoundary(newPlace.getData().getValue(), getCurrNodeBoundary(), 0)) {
			MASS.getLogger().debug("add place " + newPlace.getData().toString() + " locally (pid = " + MASSBase.getMyPid() + ")");
			addPlaceLocally(newPlace);
			return true;
		} else {

			//if it is not in master computing node's local tree
			//search for the destination 
			int dest = getRankFromGlobalNodeMap(newPlace.getData(), pivot);
			if (dest != -1) {
				for (MNode node : MASSBase.getRemoteNodes()) {
					if (node.getPid() == dest) {
						MASS.getLogger().debug("!send message [ handle = " + getHandle() + ", new place data = " + newPlace.getData().toString() 
							+ "] to remote node pid = " + dest);
						node.sendMessage(new Message(Message.ACTION_TYPE.BINARY_TREE_PLACES_ADD_PLACE, getHandle(), (Object) newPlace));
						return true;
					}
				}
			}
			
			MASS.getLogger().debug("Cannot add Place: new Place data is out of range ( data = " + newPlace.getData().toString() + ")");
			return false;
		}
	}

	/**
	 * Given a destination coordinates, search for where the coordinates reside, return the "rank"
	 *  where the coordinates is actually located, return -1 if it is not found
	 * @param allNodesBoundaries The array to obtain boundary of each computing node, the index is node's rank
	 * @return The node/rank number associated with that Place, -1 if not found
	 */
    public int getRankFromGlobalNodeMap(Data data, double[] pivot) {

		double coordinate = data.getValue()[0];
		if (coordinate < pivot[0]) {  // master node 
			//MASS.getLogger().debug("coordinate < pivot[0], return 0");
			return 0;
		} else if (coordinate >= pivot[pivot.length - 1]) {
			//MASS.getLogger().debug("coordinate >= pivot[pivot.length - 1], return " + (pivot.length - 1));
			return pivot.length;
		} else {
			
			for (int i = 1; i < pivot.length; i++) {
				if (coordinate >= pivot[i - 1] && coordinate < pivot[i]) {
					//MASS.getLogger().debug("coordinate >= pivot[" + (i - 1) + "] coordinate < pivot[" + i + "], return " + i);
					return i;
				}
			 }
		}
        
        return -1;
    }

	/**
	 * 
     * Initializes the BinaryTreePlaces with the given arguments and boundary width.
	 * --------- added by Yuna
     * @param argument
     * @param boundaryWidth
     */
	// the current program only allows # of nodes = 2^n (e.g. 2, 4, 8, 16...), codes needs 
	// to modified for other cases, e.g. nodes = 7 then automatically 4 nodes are used for computing
    private void init_master_binaryTree( Object argument, int boundaryWidth ) {  

		// create a list of all host names;  
		// the master IP name
		Vector<String> hosts = new Vector<String>( );
		
		try {
		    hosts.add( MASS.getMasterNode().getHostName() );
		} catch ( Exception e ) {
			MASSBase.getLogger().debug( "init_master_tree: InetAddress.getLocalHost( ) ", e );
		    System.exit( -1 );
		}
		// all the slave IP names
		for ( MNode node : MASS.getRemoteNodes() ) {
		    hosts.add( node.getHostName( ) );
		}
        
        // calculate boundary for master node
        int totalNodes = hosts.size();
		MASSBase.getLogger().debug( "total computer nodes = " + totalNodes);

		// calcualte the pivot point for data partition
		// pivot point is calculated by values of first dimension by default
		// an advanced method for calculating pivot point is needed in future
			
		pivot = BinaryTreeUtilities.calculatePivot(getFilename(), totalNodes);
		
		if (pivot != null) {
			MASSBase.getLogger().debug("BinaryTreePlaces.java init_master-binaryTree() pivot: size = " + pivot.length + ", " + Arrays.toString(pivot));
		} else {
			MASSBase.getLogger().debug("BinaryTreePlaces.java init_master-binaryTree() pivot: null");
		}
		
		// boundary of master node
		Boundary boundary = BinaryTreeUtilities.calculateCurrNodeBoundary(pivot, totalNodes, MASSBase.getMyPid());
		setCurrNodeBoundary(boundary);

		// register this places in the places hash map
		MASSBase.getPlacesMap().put( getHandle(), this );
		MASSBase.getLogger().debug("getPlacesMap(), placesMap.size = " + MASSBase.getPlacesMap().size());
		
		// init tree of master node
		init_all_binaryTree(getCurrNodeBoundary(), getFilename(), argument);

		MASS.getLogger().debug("after init_all_binaryTree, root = " + getRoot().getData().toString());

		// print tree of master node
		//BinaryTreeUtilities.printBinaryTree(getRoot());

		// create messages and send them to remote nodes for TreePlaces initialization
		Message m1 = new Message( Message.ACTION_TYPE.BINARY_TREE_PLACES_INITIALIZE, getHandle(), pivot, totalNodes, getClassName(), getFilename(), argument, hosts );
		
		MASSBase.getLogger().debug( "Message = handle: " + getHandle() + ", pivot: " + Arrays.toString(pivot) + 
                ", numOfNode = " + totalNodes +", classname = " + getClassName() + ", filename = " + getFilename() + 
                ", argument" + argument);	

		// send a BINARY_TREE_PLACES_INITIALIZE message to each slave
		MASSBase.getLogger().debug( "BINARY_TREE_PLACES_INITIALIZE sent to all remote nodes" );
		MASS.getRemoteNodes().forEach( place -> place.sendMessage( m1 ) );
		
		// establish all inter-node connections within setHosts( )
		MASSBase.setHosts( hosts );

		// Synchronized with all slave processes
		MASS.barrierAllSlaves();  
		
	}
	
}