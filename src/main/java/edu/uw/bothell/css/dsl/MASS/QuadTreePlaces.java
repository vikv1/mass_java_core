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
 * Class for QuadTreePlaces which manages all QuadTreePlace elements within the simulation space.
 * 
 */
package edu.uw.bothell.css.dsl.MASS;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class QuadTreePlaces extends QuadTreePlacesBase {

	private int[] maxLevelOfNodes;
	private int[] numOfLeaf;  // number of leaf node of the quad tree
	private int granularityEnhancer; 

    // constructor of treePlaces
	public QuadTreePlaces(int handle, String className, int dimensions, String fileName, int granularityEnhancer, Object argument) {
		super(handle, className, dimensions, fileName);		
		maxLevelOfNodes = new int[MASSBase.getSystemSize()];
		numOfLeaf = new int[MASSBase.getSystemSize()];
		this.granularityEnhancer = granularityEnhancer;
		//MASSBase.getLogger().debug("~~~ before init_master_tree()");
		init_master_tree(argument); 
	}
	
    /**
	 * callAll set up in master node for TreePlaces
	 * @param functionId	
	 *
	 */
	private Object[] ca_setup( int functionId, Object argument, Message.ACTION_TYPE type ) {
    	
		// calculate the total argument size for return-objects
		int total = 0; // the total number of treePlace elements
		for (int n : numOfLeaf) {
			total += n; 
		}
		MASSBase.getLogger().debug("TreePlaces: ca_setup: total = " + total);
		// send a TREE_PLACES_CALLALL message to each slave
		Message m = null;
		int srcPos = numOfLeaf[0];  // source position of arguments
		for ( int i = 0; i <  MASS.getRemoteNodes().size( ); i++ ) {
			
		    // create a message
		    if ( type == Message.ACTION_TYPE.QUADTREE_PLACES_CALL_ALL_VOID_OBJECT ) {
		    	m = new Message( type, this.getHandle(), functionId, argument );
			} else {  
		    	
		    	// TREE_PLACES_CALL_ALL_RETURN_OBJECT
		    	
				int arg_size = numOfLeaf[i + 1];
				
				Object[] partialArguments = new Object[arg_size];
				
				// This is a band-aid If you just pass null into this function this function will throw a null pointer exception. 
				try {
					System.arraycopy( (Object[]) argument, srcPos, partialArguments, 0, arg_size );
				} catch ( NullPointerException e ) {
					
				}
				
				m = new Message( type, this.getHandle(), functionId, partialArguments );
				
				MASSBase.getLogger().debug( "TreePlaces.callAll: arg_size = " + (partialArguments == null ? 0 : partialArguments.length) +
						   " srcPos = " + srcPos + " node = " + (i + 1) );
				srcPos += arg_size;
		    }
		    
		    // send it
		    MASS.getRemoteNodes().get(i).sendMessage( m );
		    
		    MASSBase.getLogger().debug( "TREE_PLACES_CALL_ALL " + m.getAction( ) + " sent to {}", MASS.getRemoteNodes().get(i).getHostName() );
		
		}
	
		// retrieve the corresponding places
		MASSBase.setCurrentPlacesBase(this);
		MASSBase.setCurrentFunctionId(functionId);
		MASSBase.setCurrentArgument(argument);
		MASSBase.setCurrentMsgType(type);
		
		
		if (type == Message.ACTION_TYPE.QUADTREE_PLACES_CALL_ALL_RETURN_OBJECT) {
			MASSBase.setCurrentReturns(new Object[total]);  // prepare an entire return space
			MASSBase.setCurrentReturnsTemp(new ArrayList<Object>());
		} else {
			MASSBase.setCurrentReturns(null);
		}
		
		// resume threads
		MThread.resumeThreads( MThread.STATUS_TYPE.STATUS_CALLALL );
		
		// callAll implementation
		if ( type == Message.ACTION_TYPE.QUADTREE_PLACES_CALL_ALL_VOID_OBJECT )
		    super.callAll( functionId, argument, 0 ); // 0 = the main tid
		else {
			int argumentLength = argument == null ? 0 : ((Object[])argument).length;
		    super.callAll( functionId, (Object[])argument, argumentLength, 0 );
		}
		// copy return values (master node) from List to Object[] currentReturns
		int arrSize = MASSBase.getCurrentReturnsTemp().size();
		MASSBase.getLogger().debug("return values size = " + arrSize);
		MASSBase.setCurrentReturns(MASSBase.getCurrentReturnsTemp().toArray(new Object[arrSize]), 0);
		
		// confirm all threads are done with callAll.
		MThread.barrierThreads( 0 );
		
		// Synchronized with all slave processes
		MASSBase.getLogger().debug("Attempting to barrierAllSlaves...");
		MASS.barrierAllSlaves_treePlaces( MASSBase.getCurrentReturns(), numOfLeaf );
		MASSBase.getLogger().debug("barrierAllSlaves completed!");

		return MASSBase.getCurrentReturns();
    
    }

	/**
	 * calculate the boundary of all data from an input file. 
	 */
    public Boundary calculateBoundary(String filename) {
        double[] min = new double[getDimensions()];
        double[] max = new double[getDimensions()];
        Arrays.fill(min, Integer.MAX_VALUE);
        Arrays.fill(max, Integer.MIN_VALUE);
        try{
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String line = null;
       
            while((line = in.readLine()) != null) {
               
                String[] items = line.split(" ");
                double[] coordinates = new double[items.length];
                for (int i = 0; i < items.length; i++) {
                    coordinates[i] = Double.parseDouble(items[i]);
                    min[i] = Math.min(min[i], coordinates[i]);
                    max[i] = Math.max(max[i], coordinates[i]);
                }

            }

            // round down min and round up max
            double[] min_final = new double[getDimensions()];
            double[] max_final = new double[getDimensions()];
            for (int i = 0; i < getDimensions(); i++) {
				//boundary is 1 less/greater than min[] or max[] to avoid data points fall on the edge
				//because the range of tree is [min, max);
                min_final[i] = Math.floor(min[i] - 1.0);
				max_final[i] = Math.ceil(max[i] + 1.0);
				//min_final[i] = Math.floor(min[i] - (max[i] - min[i]) / 10.0);
				//max_final[i] = Math.floor(max[i] + (max[i] - min[i]) / 10.0);
            }
            Boundary boundary = new Boundary(min_final, max_final);
            MASS.getLogger().debug("QuadTreePlaces calculateBoundary(): boundary = " + boundary.toString());
            return boundary;
        } catch(FileNotFoundException e) {
            MASS.getLogger().debug("QuadTreePlaces --> calculateBoundary(): FileNotFoundException:" + e);
        } catch(IOException e) {
            MASS.getLogger().debug("QuadTreePlaces --> calculateBoundary(): IO Exception: " + e);  
		}
		return null;
	}
	
	/**
	 * Calls the method specified with functionId of all array elements. Done
	 * in parallel among multi-processes/threads.
	 * @param functionId The ID of the function to call
	 */
	
	public void callAll( int functionId ) {
		MASSBase.getLogger().debug( "callAll void object" );
		ca_setup( functionId, null, Message.ACTION_TYPE.QUADTREE_PLACES_CALL_ALL_VOID_OBJECT );
    }
	
	/**
	 * Calls the method specified with functionId of all tree elements as
	 * passing arguments[i] to element[i]’s method, and receives a return
	 * value from it into (void *)[i] whose element’s size is return_size. Done 
	 * in parallel among multi-processes/threads. In case of a multi-
	 * dimensional array, "i" is considered as the index when the array is
	 * flattened to a single dimension.
	 * @param functionId The ID of the function to call
	 * @param argument An argument to supply to the function being called in each Place
	 * @return An Object (actually, an Object[]) with each element set to the return value
	 * 			supplied by each Place in the cluster
	 */
	public Object[] callAll( int functionId, Object[] argument ) {
	
		MASSBase.getLogger().debug( "callAll return object" );
		
		return ca_setup( functionId, ( Object )argument, Message.ACTION_TYPE.QUADTREE_PLACES_CALL_ALL_RETURN_OBJECT );
    
	}
    

	public int getTotalLeafPlaces() {
		int ret = 0;
		for (int n : numOfLeaf) {
			ret += n;
		}
		return ret;
	}
	
	/**
	 * 
     * Initializes the QuadTreePlaces with the given arguments.
	 * This method only allows # of nodes = 2^n (e.g. 2, 4, 8, 16...), 
	 * e.g. nodes = 7 then automatically 4 nodes are used for computing
     * @param argument
     * @param boundaryWidth
     */
    private void init_master_tree( Object argument) {  

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

		// calculate boundary for all input data
		Boundary boundaryAll = calculateBoundary(getFilename());
		MASSBase.getLogger().debug("all min = [" + boundaryAll.getMin()[0] + "," + boundaryAll.getMin()[1] + "], max = [" + boundaryAll.getMax()[0] 
			+ "," + boundaryAll.getMax()[1] + "]");
		Boundary boundaryCurrNode = QuadTreeUtilities.calculateBoundaryOfCurrNode(boundaryAll, totalNodes, MASSBase.getMyPid());
		setBoundaryAll(boundaryAll);
		setBoundaryCurrNode(boundaryCurrNode);

		// init tree place in master node
		init_all_quadtree(argument);

		// create messages and send them to remote nodes for QuadTreePlaces initialization
		Message m1 = new Message( Message.ACTION_TYPE.QUADTREE_PLACES_INITIALIZE, getHandle(), getDimensions(),
					 boundaryAll, totalNodes, getClassName(), getFilename(), argument, hosts );

		MASSBase.getLogger().debug( "Message = "+getHandle() + ", " + getDimensions() + ", "+ boundaryAll.toString() + 
                ", numOfNode = " + totalNodes +", classname = " + getClassName() + ", filename = " + getFilename() + 
                ", argument" + argument);	

		// send a QUADTREE_PLACES_INITIALIZE message to each slave
		MASSBase.getLogger().debug( "QUADTREE_PLACES_INITIALIZE sent to all remote nodes" );
		MASS.getRemoteNodes().forEach( place -> place.sendMessage( m1 ) );
		
		// establish all inter-node connections within setHosts( )
		MASSBase.setHosts( hosts );
	
		// register this places in the places hash map
		MASSBase.getPlacesMap().put( getHandle(), this );
		MASSBase.getLogger().debug("getPlacesMap(), placesMap.size = " + MASSBase.getPlacesMap().size());

		// Synchronized with all slave processes
		MASS.barrierAllSlaves(maxLevelOfNodes, numOfLeaf);  
		int maxLevel = 0;
		maxLevelOfNodes[0] = getLocalMaxLevelOfTree();

		// calculate # of leaf node in current quadTreePlaces
		numOfLeaf[0] = getLocalNumOfLeaf();
		MASSBase.getLogger().debug( "maxLevelOfNodes[0] = " +  maxLevelOfNodes[0] + ", numOfLeaf[0] = " +  numOfLeaf[0]);

		for (int level : maxLevelOfNodes) {
			maxLevel = Math.max(maxLevel, level);
		}
		maxLevel += granularityEnhancer;
		setMaxLevelOfAllTrees(maxLevel); 
		
		
		// create messages and send the max level of all trees to remote nodes
		Message m2 = new Message( Message.ACTION_TYPE.SEND_MAX_LEVEL, getHandle(), maxLevel, "send max level");
		// send a SEND_MAX_LEVEL message to each slave
		MASSBase.getLogger().debug( "SEND_MAX_LEVEL sent to all remote nodes" );
		MASS.getRemoteNodes().forEach( place -> place.sendMessage( m2 ) );
		MASS.barrierAllSlaves(); 

    }

	
}