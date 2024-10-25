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
 * Class for BinaryTreePlacesBase.
 * 
 */

package edu.uw.bothell.css.dsl.MASS;

import java.util.Arrays;
import java.util.Vector; 

public class BinaryTreePlacesBase extends PlacesBase{

    private String filename;  //input file
    private int dimensions;
    private int totalNodes;  //total number of computing nodes
    private BinaryTreePlace root = null;
    private int[] currMaxIndex;
    private Boundary currNodeBoundary = null;

    private Vector<BinaryTreePlace> binaryTreePlaces = new Vector<>();

    //constructor of BinaryTreePlaces for remote nodes. 
    public BinaryTreePlacesBase( int handle, String classname, double[] pivot, int totalNodes, String filename, Object initArgs) {
        
        super(handle, classname);

        this.totalNodes = totalNodes;
        this.filename = filename;
        this.currMaxIndex = new int[2]; 
        this.currMaxIndex[0] = MASSBase.getMyPid();
        this.currMaxIndex[1] = -1;

        this.currNodeBoundary = BinaryTreeUtilities.calculateCurrNodeBoundary(pivot, totalNodes, MASSBase.getMyPid());
        MASSBase.getLogger().debug("currNodeBoundary = " + currNodeBoundary.toString());
		MASSBase.getLogger().debug( "Places_base handle = " + handle + ", class = " + classname
            + ",  MyPid = " + MASSBase.getMyPid() + ", totalNodes = " + this.totalNodes
            + "rangeCurrNode =" + currNodeBoundary.toString() + ", filename = " + filename
            + "pivot = " + Arrays.toString(pivot));
    
        // register this places in the places hash map
		MASSBase.getPlacesMap().put(handle, this );
        MASSBase.getLogger().debug("getPlacesMap(), placesMap.size = " + MASSBase.getPlacesMap().size());
        
        init_all_binaryTree(currNodeBoundary, filename, initArgs);
       
    }

    // constructor of treePlaces for master node
	public BinaryTreePlacesBase(int handle, String className, String filename) {
        super(handle, className);	
        this.filename = filename;
        this.currMaxIndex = new int[2]; 
        this.currMaxIndex[0] = MASSBase.getMyPid();
        this.currMaxIndex[1] = -1;
    }
    
    /**
     * add new place to local binary tree
     */
    public boolean addPlaceLocally(BinaryTreePlace newPlace) {
        boolean success = addPlaceLocally(root, newPlace);
        if (success) {
            //BinaryTreeUtilities.printBinaryTree(root);
            return true;
        }
        return false;
    }

    /**
     * add a new Place to a specific tree node (a BinaryTreePlace object)
     */
    public boolean addPlaceLocally(BinaryTreePlace place, BinaryTreePlace newPlace) {

        if (place == null) MASSBase.getLogger().debug("addPlaceLocally: place = null");

        if (place.getData() == null) MASSBase.getLogger().debug("addPlaceLocally: place.getData() = null");
        
//        Data dataToAdd = newPlace.getData();
        //MASSBase.getLogger().debug("newPlace data = " + dataToAdd.toString());
        int compareDim = place.getData().getCompareDim();
        
        DataComparator dataComparator = new DataComparator(compareDim);
        if (dataComparator.compare(place.getData(), newPlace.getData()) == 0 ) {
            MASSBase.getLogger().debug("data already exist in the tree. No duplicates allowed. Add data failed.");
            return false;
        }

        int handle = getHandle();
        if (dataComparator.compare(place.getData(), newPlace.getData()) > 0) {
            
            //if new data is greater than the data in Place, add it to the left child node
            if (place.getLeftNode() != null) {
                // if left node is not null, keep searching place in left node
                addPlaceLocally(place.getLeftNode(), newPlace);  
            } else {
                // otherwise add node to the left
                place.setLeftNode(newPlace);
                newPlace.setIndex(new int[]{currMaxIndex[0], currMaxIndex[1] + 1});

                // add newPlace to Vector<BinaryTreePlace>
                ((BinaryTreePlacesBase) (MASSBase.getPlacesMap().get(handle))).getBinaryTreePlaces().add(newPlace);
                
                MASSBase.getLogger().debug("Place " + newPlace.getData().toString() + " was added to node, index = [" + 
                    newPlace.getIndex()[0] + ", " + newPlace.getIndex()[1] + "].");

                // update maxIndex in BinaryTreePlacesBase
                currMaxIndex[1]++;

                // set compare dimension of newPlace
                newPlace.setCompareDim(place.getData().getCompareDim());
            }
            
        } else {
            //if the new data is >= data in Place, add it to the right child node
            if (place.getRightNode() != null) {

                addPlaceLocally(place.getRightNode(), newPlace);
            } else {
                // otherwise add node to the right
                place.setRightNode(newPlace);
                newPlace.setIndex(new int[]{currMaxIndex[0], currMaxIndex[1] + 1});

                // add newPlace to Vector<BinaryTreePlace>
                ((BinaryTreePlacesBase) (MASSBase.getPlacesMap().get(handle))).getBinaryTreePlaces().add(newPlace);

                MASSBase.getLogger().debug("Place " + newPlace.getData().toString() + " was added to node, index = [" + 
                    newPlace.getIndex()[0] + ", " + newPlace.getIndex()[1] + "].");

                // update maxIndex in BinaryTreePlacesBase
                currMaxIndex[1]++;

                // set compare dimension of newPlace
                newPlace.setCompareDim(place.getData().getCompareDim());
            }
        }

        return true;
    }
    
    /**
     * Helper method for building tree. This method is called recursively. 
     * @param dataCurrNode all data items under the current computingn node
     * @param start low index of data for the current place
     * @param end high index of data for the current place
     * @param parentCompareDim the comparison dimension of the parent place 
     * @param initArgs argument for place initialization
     * @return BinaryTreePlace
     */
    public BinaryTreePlace buildTreeHelper(Data[] dataCurrNode, int start, int end, int parentCompareDim, Object initArgs) {
        if (start >= end) return null;

        //the data is compared in dimension 0 or 1 alternatively which only works for 2D
        //method needs to be modified for multi-dimensional data
        int compareDim = -1;
        if (parentCompareDim == 0) {
            compareDim = 1;
        } else {
            compareDim = 0; 
        }     

        Arrays.sort(dataCurrNode, start, end, new DataComparator(compareDim));
        int median = start + (end - start) / 2;
        Data d = new Data(dataCurrNode[median], compareDim);

        Object[] finalArgs = new Object[2];
        BinaryTreePlaceArgs binaryTreeArgs =  new BinaryTreePlaceArgs(d, getHandle());
        finalArgs[0] = (Object) binaryTreeArgs;
        finalArgs[1] = initArgs;

        BinaryTreePlace place = null;

        try {
            place = objectFactory.getInstance(getClassName(), (Object) finalArgs);
            
        } catch (Exception e) {
            MASSBase.getLogger().error("BinaryTreePlaces.buildTreeHelper: {} not loaded and/or instantiated", getClassName(), e);
        }
        
        place.setIndex(new int[]{currMaxIndex[0], (currMaxIndex[1] + 1)});
        MASSBase.getLogger().debug("create BinaryTreePlace, data = " + d.toString() + "index = " + 
                place.getIndex()[1] + ", total = " + dataCurrNode.length);

        // increment local maximum index
        currMaxIndex[1]++;
        binaryTreePlaces.add(place);

        //set child node
        place.setLeftNode(buildTreeHelper(dataCurrNode, start, median, compareDim, initArgs));

        place.setRightNode(buildTreeHelper(dataCurrNode, median + 1, end, compareDim, initArgs)); 

        return place;
    }

    public Vector<BinaryTreePlace> getBinaryTreePlaces() {
        return binaryTreePlaces;
    }


    /**
     * get the current maximum index of the computing node
     */
    public int[] getCurrMaxIndex() {
        return currMaxIndex;
    }

    public Boundary getCurrNodeBoundary() {
        return currNodeBoundary;
    }
    
    public int getDimensions( ) {
    	return dimensions;
    }

    public String getFilename() {
        return filename;
    }
        
    public BinaryTreePlace getRoot() {
        return root;
    }

    /**
     * init_all method for binary tree.
     * if filename != null, reads input and build binary tree
     * if filename == null, only create a root in the binary tree
     * @param boundary boundary of current computing node
     * @param filename the input file
     * @param initArgs the initial arguments for binary tree places initialization
     */ 
    public void init_all_binaryTree(Boundary boundary, String filename, Object initArgs) {
    
        MASSBase.getLogger().debug("BinaryTreePlacesBase --> init_all_tree()");

        // TODO - HACK! Agents and Places need to be able to "reach" this PlacesBase during instantiation;
        if ( MASS.getCurrentPlacesBase() == null ) MASS.setCurrentPlacesBase( (PlacesBase)this );

        //the data items which are in the current computing node's boundary
        Data[] dataCurrNode = BinaryTreeUtilities.inputData(boundary, filename);
   
        // create root of the binary tree  
        // sort data by customized comparator (first dimension)
        Arrays.sort(dataCurrNode, new DataComparator(0));

        //MASS.getLogger().debug("sorted data: " + dataCurrNode[0].toString() + "," + dataCurrNode[1].toString() 
        //    + "," + dataCurrNode[2].toString() + "," + dataCurrNode[3].toString());

        int median = dataCurrNode.length / 2;

        Data d = new Data(dataCurrNode[median], 0);        
        
        Object[] finalArgs = new Object[2];
        BinaryTreePlaceArgs binaryTreeArgs =  new BinaryTreePlaceArgs(d, getHandle());
        finalArgs[0] = (Object) binaryTreeArgs;
        finalArgs[1] = initArgs;
       
        try {
            root = objectFactory.getInstance(getClassName(), (Object) finalArgs);
        } catch (Exception e) {
            MASSBase.getLogger().error("BinaryTreePlaces.init_all_binaryTree: {} not loaded and/or instantiated", getClassName(), e);
		}
        

        root.setIndex(new int[]{currMaxIndex[0], (currMaxIndex[1] + 1)});
        MASSBase.getLogger().debug("root created.");

        currMaxIndex[1]++;

        binaryTreePlaces.add(root);

        MASS.getLogger().debug("root = " + root.getData().toString() + " added.");

        // build left subtree
        root.setLeftNode(buildTreeHelper(dataCurrNode, 0, median, 0, initArgs));
        
        // build right subtree
        root.setRightNode(buildTreeHelper(dataCurrNode, median + 1, dataCurrNode.length, 0, initArgs));

        //BinaryTreeUtilities.printBinaryTree(root); 

    }

    //return the total number of places
    public int nPlaces() {
        return binaryTreePlaces.size();
    }

    /**
     * set the current maximum index of the computing node
     */
    public void setCurrMaxIndex(int[] index) {
        currMaxIndex = index;
    }

    public void setCurrNodeBoundary(Boundary boundary) {
        currNodeBoundary = boundary;
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("handle = " + getHandle() + ", boundary of root = " + currNodeBoundary.toString());
        return sb.toString();
    }

    /**
     * Execute a function (specified by ID) on each BinaryTreePlace, with a single argument
     * @param functionId The function (method) to execute
     * @param argument Optional argument to be supplied to the method
     * @param tid The ID of the thread that should execute the method
     */
    /*
    public void callAll( int functionId, Object argument, int tid ) {
    	
    	// debugging 
    	if ( MASSBase.getLogger().isDebugEnabled() )
    		MASSBase.getLogger().debug( "thread[" + tid + "] callAll functionId = " + functionId);

        root.callMethodHelper(functionId, argument);
    
    }
    */
    /**
     * Execute a function (specified by ID) on each TreePlace, with multiple arguments
     * @param functionId The function (method) to execute
     * @param arguments Optional arguments to be supplied to the method
     * @param length The number of arguments supplied
     * @param tid The ID of the thread that should execute the method
     */
    /*
    public Object callAll( int functionId, Object[] arguments, int length, int tid ) {

        // single thread only

    	// debugging
    	if ( MASSBase.getLogger().isDebugEnabled() )
    		MASSBase.getLogger().debug( "thread[" + tid + "] callAll_return object functionId = " + 
                functionId + ", arguments.length = " + length );
        
        root.callMethodHelper(functionId, arguments);
    	
    	return null;
    
    }
    */

    /**
	 * Given a destination coordinates, search for where the coordinates reside, return the "rank"
	 *  where the coordinates is actually located
	 * @param allNodesBoundaries The array to obtain boundary of each computing node, the index is node's rank
	 * @return The node/rank number associated with that Place, -1 if not found
	 */
    /*
    public int getRankFromGlobalNodeMap(double[] coordinates, Boundary[] allNodesBoundaries) {
        for (int i = 0; i < allNodesBoundaries.length; i++) {
            if (TreeUtilities.withinBoundary(coordinates, allNodesBoundaries[i])) {
                return i;
            }
        }
        return -1;
    }
    */
}