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
 * Class for QuadTreePlacesBase. 
 * 
 */
package edu.uw.bothell.css.dsl.MASS;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

public class QuadTreePlacesBase extends PlacesBase{

    private String filename;
    private int dimensions;
    private Boundary boundaryAll;  // boundary of all data (all computing nodes)
    private Boundary boundaryCurrNode;  // boundary of current node
    private int totalNodes;  // total number of computing node
    private QuadTreePlace treePlaceRoot; // the root of the quad tree in the current computing node
    private int localMaxLevelOfTree;  // the maximum level (deepest tree node) of the local quad tree
    private int maxLevelOfAllTrees;  // the maximum level (deepest tree node) of all quad trees
    private int localNumOfLeaf;  // the number of leaf node in local quad tree

    // constructor of QuadTreePlaces for remote node
    public QuadTreePlacesBase( int handle, String classname, int dimensions, Boundary boundary, int totalNodes, String filename, Object argument) {
        super(handle, classname);
        this.filename = filename;
        this.dimensions = dimensions;
        this.boundaryAll = boundary;
        this.totalNodes = totalNodes;
        this.localMaxLevelOfTree = 0;

        this.boundaryCurrNode = QuadTreeUtilities.calculateBoundaryOfCurrNode(boundaryAll, totalNodes, MASSBase.getMyPid());
        
		MASSBase.getLogger().debug( "Places_base handle = " + handle + ", class = " + classname
            + ", dimensions = " + dimensions + ", boundary_all = " + this.boundaryAll.toString() 
            + "boundaryCurrNode =" + boundaryCurrNode.toString() + ", filename = " + filename);
	
        init_all_quadtree(argument);
       
    }

    // constructor of QuadTreePlaces for master node
	public QuadTreePlacesBase(int handle, String className, int dimensions, String fileName) {
		super(handle, className);		
        this.dimensions = dimensions;
        this.filename = fileName;
    }
    
    /**
     * Execute a function (specified by ID) on each QuadTreePlace, with a single argument
     * @param functionId The function (method) to execute
     * @param argument Optional argument to be supplied to the method
     * @param tid The ID of the thread that should execute the method
     */
    public void callAll( int functionId, Object argument, int tid ) {
    	
    	// debugging 
    	if ( MASSBase.getLogger().isDebugEnabled() )
    		MASSBase.getLogger().debug( "thread[" + tid + "] callAll functionId = " + functionId);

        treePlaceRoot.callMethodHelper(functionId, argument);
    
    }
    
    /**
     * Execute a function (specified by ID) on each QuadTreePlace, with multiple arguments
     * @param functionId The function (method) to execute
     * @param arguments Optional arguments to be supplied to the method
     * @param length The number of arguments supplied
     * @param tid The ID of the thread that should execute the method
     */
    public Object callAll( int functionId, Object[] arguments, int length, int tid ) {

        // single thread only

    	// debugging
    	if ( MASSBase.getLogger().isDebugEnabled() )
    		MASSBase.getLogger().debug( "thread[" + tid + "] callAll_return object functionId = " + 
                functionId + ", arguments.length = " + length );
        
        treePlaceRoot.callMethodHelper(functionId, arguments);
    	
    	return null;
    
    }

    public Boundary getBoundaryAll() {
        return boundaryAll;
    }

    public Boundary getBoundaryCurrNode() {
        return boundaryCurrNode;
    }

    public int getDimensions() {
        return this.dimensions;
    }

    public String getFilename() {
        return this.filename;
    }


    public int getLocalMaxLevelOfTree() {
        return localMaxLevelOfTree;
    }

    public int getLocalNumOfLeaf() {
        return localNumOfLeaf;
    }

    public int getMaxLevelOfAllTrees() {
        return maxLevelOfAllTrees;
    }
    /**
	 * Given a destination coordinates, search for where the coordinates reside, return the "rank"
	 *  where the coordinates is actually located
	 * @param allNodesBoundaries The array to obtain boundary of each computing node, the index is node's rank
	 * @return The node/rank number associated with that Place, -1 if not found
	 */
    public int getRankFromGlobalNodeMap(double[] coordinates, Boundary[] allNodesBoundaries) {
        for (int i = 0; i < allNodesBoundaries.length; i++) {
            if (QuadTreeUtilities.withinBoundary(coordinates, allNodesBoundaries[i])) {
                return i;
            }
        }
        return -1;
    }

    public QuadTreePlace getRoot() {
        return treePlaceRoot;
    }

    /**
     * init_all method for quad tree
     * @param initArgs the arguemnt for quad tree initialization
     */
    public void init_all_quadtree(Object initArgs) {
        MASSBase.getLogger().debug("QuadTreePlacesBase --> init_all_quadtree()");

        // TODO - HACK! Agents and Places need to be able to "reach" this PlacesBase during instantiation;
        if ( MASS.getCurrentPlacesBase() == null ) MASS.setCurrentPlacesBase( (PlacesBase)this );
        
        try {
            // create the root of quad tree
            Vector<Integer> treeIndex = new Vector<>();
            treeIndex.add(MASSBase.getMyPid());

            Object[] finalArgs = new Object[2];
            QuadTreePlaceArgs treePlaceArgs = new QuadTreePlaceArgs(dimensions, 0, boundaryCurrNode, treeIndex, null, null);            
            finalArgs[0] = (Object) treePlaceArgs;
            finalArgs[1] = initArgs;

            treePlaceRoot = objectFactory.getInstance(getClassName(), (Object) finalArgs);
            MASSBase.getLogger().debug( "QuadTreePlace: " + " index:[" + treeIndex.toString() + "], boundary:" 
                + boundaryCurrNode.toString() + "] created.");

            // add data point to the tree
            try{
                BufferedReader in = new BufferedReader(new FileReader(filename));
                String line = null;
                
                int index = 0;
                while((line = in.readLine()) != null) {
                   
                    //get the data coordinates
                    String[] items = line.split(" ");
                    double[] coordinates = new double[items.length];

                    for (int i = 0; i < items.length; i++) {
                        coordinates[i] = Double.parseDouble(items[i]);
                    }

                    //add the data to the tree if it is in the boundary of current computing node
                    if (QuadTreeUtilities.withinBoundary(coordinates, boundaryCurrNode)) {
                        Point p = new Point(coordinates, index);
                    
                        localMaxLevelOfTree = Math.max(localMaxLevelOfTree, treePlaceRoot.insert(p, initArgs));
                    }

                    index++;
                }
                MASS.getLogger().debug("QuadTreePlaces --> init_all_quadtree(): all points inserted to QuadTreePlace " + treeIndex.toString());
                        
                MASS.getLogger().debug("All Trees: ");
                MASS.getLogger().debug(QuadTreeUtilities.display_tree(treePlaceRoot));

            } catch(FileNotFoundException e) {
                MASS.getLogger().debug("QuadTreePlaces --> init_all_quadtree(): FileNotFoundException:" + e);
            } catch(IOException e) {
                MASS.getLogger().debug("QuadTreePlaces --> init_all_quadtree(): IO Exception: " + e);  
            } 

        }
        // TODO - what to do when this exception is caught?
        catch (Exception e) {
            MASSBase.getLogger().error("QuadTreePlaces --> init_all_quadtree: {} not loaded and/or instantiated", getClassName(), e);
        }
        
        setLocalMaxLevelOfTree(localMaxLevelOfTree);
        MASS.getLogger().debug("QuadTreePlacesBase: maxLevelOfTree = " + getLocalMaxLevelOfTree());

        // calculate the number of leaf node of the local quad tree
        localNumOfLeaf = treePlaceRoot.calculateNumOfLeaf();
        setLocalNumOfLeaf(localNumOfLeaf);
        MASS.getLogger().debug("QuadTreePlacesBase: localNumOfLeaf = " + getLocalNumOfLeaf()); 

    }

    public void setBoundaryAll(Boundary boundary) {
        boundaryAll = boundary;
    }
    
    public void setBoundaryCurrNode(Boundary boundary) {
        boundaryCurrNode = boundary;
    }

    public void setLocalMaxLevelOfTree(int level) {
        localMaxLevelOfTree = level;
    }

    public void setLocalNumOfLeaf(int numOfLeaf) {
        localNumOfLeaf = numOfLeaf;
    }

    public void setMaxLevelOfAllTrees(int level) {
        maxLevelOfAllTrees = level;
    }

    //return the QuadTreePlaces' info as a string 
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("handle = " + getHandle() + ", boundary of root = " + treePlaceRoot.getBoundary().toString());
        return sb.toString();
    }
    
}