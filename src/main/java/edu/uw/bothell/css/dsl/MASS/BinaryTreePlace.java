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
 * Class for BinaryTreePlace.
 * 
 */
package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;
import java.util.Arrays;

@SuppressWarnings("serial")
public class BinaryTreePlace extends Place implements Serializable { 
  

    private Agent agent = null;
    private Data data = null;
    private int handle = -1;
    private BinaryTreePlace leftNode = null;
    private BinaryTreePlace rightNode = null;

    public BinaryTreePlace() {
        super();
    }

    // constructor for Place without pre-assigned index 
    public BinaryTreePlace(Data data, int handle) {
        MASSBase.getLogger().debug("create BinaryTreePlace, data = " + data.toString() + ", handle = " + handle);
        this.data = data;
        this.handle = handle;
    }
    
    public BinaryTreePlace(Object args) {
        
        super();
        
        // initialize SpacePlace
        BinaryTreePlaceArgs binaryTreePlaceArgs = (BinaryTreePlaceArgs) args;
        this.data = binaryTreePlaceArgs.getData();
        this.handle = binaryTreePlaceArgs.getHandle();
    
    }

    public Data getData() {
        return data;
    }

    public BinaryTreePlace getLeftNode() { 
        return leftNode;
    }

    public BinaryTreePlace getRightNode() {
        return rightNode;
    }

    /**
     * print all agents in a Place
     */
    public void printAgentsMap() {
        StringBuilder sb = new StringBuilder();
        sb.append("BinaryPlace = " + Arrays.toString(getIndex()));
        sb.append("\n");
  
        for (Agent agent : getAgents()) {
            sb.append(" agent id = " + agent.getAgentId() + ", index = [" + Arrays.toString(agent.getIndex()) + "]");
        }
        sb.append("\n");

        MASSBase.getLogger().debug(sb.toString());
    }

    /**
     * set the dimension that is used for data comparison
     */
    public void setCompareDim(int dim) {
        if (dim == -1) {
            MASSBase.getLogger().error("parent place's compare dimension has not been set yet.");
        }
        if (dim == 0) {
            this.getData().setCompareDim(1);
        } else {
            this.getData().setCompareDim(0);
        }
    }

    public boolean setLeftNode(BinaryTreePlace node) {
        if (leftNode == null) {
            leftNode = node;
            return true;
        } else {
            MASSBase.getLogger().error("BinaryTreePlace.java setLeftNode() failed. Left node already existed.");
            return false;
        }
    }

    /*
    // set tree boundary for root
    public void setBoundary(int dim) {
        double[] min = new double[dim];
        double[] max = new double[dim];
        Arrays.fill(min, Double.MIN_VALUE);
        Arrays.fill(min, Double.MAX_VALUE);
    }
    */


   /*
    // find the destination tree of a given point
    public TreePlace getDestinationTree(Point p) {
        // if a child can't contain an object, it will live in this quad
        TreePlace destTree = this;
        double[] coordinates = p.getCoordinates();
        if (TreeUtilities.withinBoundary(coordinates, topLeft.getBoundary())) {
            destTree = topLeft;
        }
        if (TreeUtilities.withinBoundary(coordinates, topRight.getBoundary())) {
            destTree = topRight;
        }
        if (TreeUtilities.withinBoundary(coordinates, bottomLeft.getBoundary())) {
            destTree = bottomLeft;
        }
        if (TreeUtilities.withinBoundary(coordinates, bottomRight.getBoundary())) {
            destTree = bottomRight;
        }
        return destTree;
    }
    */

    
    /*
    public int calculateNumOfLeaf() { 
        
        //MASSBase.getLogger().debug(this.getTreeIndex().toString());
        if (isLeaf) {
            
            return 1;
        } else {
            int leaf = topLeft.calculateNumOfLeaf() + topRight.calculateNumOfLeaf() + 
            bottomLeft.calculateNumOfLeaf() + bottomRight.calculateNumOfLeaf();
            return leaf;
        }
    }
    */
    
    /*
    // traverse all tree nodes for callMethod()
    public Object callMethodHelper(int functionId, Object argument) {
        MASSBase.getLogger().debug("~~callMethodHelper functionId = " + functionId);
        MASSBase.getLogger().debug("isLeaf = " + isLeaf);
        if (isLeaf) {
            MASSBase.getLogger().debug("before callMethod, functionId = " + functionId + "args = " + argument);
            Object retValue = callMethod( functionId, argument);
            MASSBase.getCurrentReturnsTemp().add(retValue);
        }
        if (topLeft != null) {
            topLeft.callMethodHelper(functionId, argument);
        }
        if (topRight != null) {
            topRight.callMethodHelper(functionId, argument);
        }
        if (bottomLeft != null) {
            bottomLeft.callMethodHelper(functionId, argument);
        }
        if (bottomRight != null) {
            bottomRight.callMethodHelper(functionId, argument);
        }
        return null;
    }
    */
   
    public boolean setRightNode(BinaryTreePlace node) {
        if (rightNode == null) {
            rightNode = node;
            return true;
        } else {
            MASSBase.getLogger().error("BinaryTreePlace.java setRightNode() failed. Right node already existed.");
            return false;
        }
    }
    
}

