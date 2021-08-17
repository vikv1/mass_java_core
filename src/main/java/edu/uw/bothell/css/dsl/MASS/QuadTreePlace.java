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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.factory.ObjectFactory;
import edu.uw.bothell.css.dsl.MASS.factory.SimpleObjectFactory;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

@SuppressWarnings("serial")
public class QuadTreePlace extends Place {

    private static final int BOTTOM_LEFT = 0;
    private static final int BOTTOM_RIGHT = 1;
    private static final int TOP_LEFT = 2;
    private static final int TOP_RIGHT = 3;
//    private Agent agent = null;
    private Point point = null;
    private int dimensions;
    private int level;
    private Boundary boundary;

    //index of Quad Tree Place
    private Vector<Integer> treeIndex;
    private int granularity = 1;  // default granularity
    private double[] interval;  //the interval of sub-place

    private QuadTreePlace topLeft = null;
    private QuadTreePlace topRight = null;
    private QuadTreePlace bottomLeft = null;
    private QuadTreePlace bottomRight = null;
    private QuadTreePlace parent = null;   // parent QuadTreePlace of current treePlace
    private boolean isLeaf = true;  //whether the place is a leaf of not

    public boolean isDisplayed = false;

    protected ObjectFactory objectFactory = SimpleObjectFactory.getInstance();

    // hashtable to keep all agents in the treePlace, key is agent's location(sublinear index in place), value is a set of agents
    private Hashtable<Integer, Set<Agent>> agentsMap = new Hashtable<>();

    public QuadTreePlace(Object args) {
        
        super();

        // initialize QuadTreePlace
        QuadTreePlaceArgs quadTreePlaceArgs = (QuadTreePlaceArgs) args;
        this.dimensions = quadTreePlaceArgs.getDimensions();
        this.level = quadTreePlaceArgs.getLevel();
        this.boundary = quadTreePlaceArgs.getBoundary();
        this.treeIndex = quadTreePlaceArgs.getTreeIndex();
        this.point = quadTreePlaceArgs.getPoint();
        this.parent = quadTreePlaceArgs.getParent();
        this.interval = new double[dimensions];

    }

    // add agent to sub-place in treePlace
    public void addAgentToSubPlace(QuadTreeAgent agent, int[] subIndex) {
        
        // get the linear subindex
        int[] subSize = new int[dimensions];
        Arrays.fill(subSize, granularity);
        int subIndexLinear = MatrixUtilities.getLinearIndex(subSize, subIndex);  // initialize linear subIndex
        Set<Agent> agent_list = agentsMap.getOrDefault(subIndexLinear, new HashSet<Agent>());
        agent_list.add(agent);
        agentsMap.put(subIndexLinear, agent_list);

    }

    //return all agent's info in the current place as a string
    public String agentMap_toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("QuadTreePlace index = " + treeIndex.toString() + "\n");
        for (Integer index : agentsMap.keySet()) {
            sb.append(index + ": (");
            Set<Agent> s = agentsMap.get(index);
            for (Agent agent : s) {
                sb.append(agent.getAgentId() + "(" + ((QuadTreeAgent)agent).getOriginalAgentId() + "), ");
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    /**
     * calculate the number of leaf node
     * @return
     */
    public int calculateNumOfLeaf() { 
        
        if (isLeaf) {
            
            return 1;
        } else {
            int leaf = topLeft.calculateNumOfLeaf() + topRight.calculateNumOfLeaf() + 
            bottomLeft.calculateNumOfLeaf() + bottomRight.calculateNumOfLeaf();
            return leaf;
        }
    }

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

    //(for visualization) all places coordinates are written to the ArrayList recordLines in MASS class
    public void displayPlace() {
        if (isDisplayed) {
            return;
        }
        double intervalX = interval[0];
        double intervalY = interval[1];

        double minX = boundary.getMin()[0];
        double minY = boundary.getMin()[1];

        double maxX = boundary.getMax()[0];
        double maxY = boundary.getMax()[1];
        
        

        double coordinateY = minY;
        while (coordinateY < maxY) {
            Double[] line = new Double[]{minX, coordinateY, maxX, coordinateY};
            MASS.getRecordLines().add( line );
            coordinateY += intervalY;
        }

        double coordinateX = minX;
        while (coordinateX < maxX) {
            Double[] line = new Double[]{coordinateX, minY, coordinateX, maxY};
            MASS.getRecordLines().add( line );
            coordinateX += intervalX;
        }

        isDisplayed = true;
        
        // TODO - what is the point of doing this?
        MASS.getRecordLines().add( null ); 

    }

    public Hashtable<Integer, Set<Agent>> getAgentsMap() {
        return agentsMap;
    }

    public QuadTreePlace getBottomLeft() {
        return bottomLeft;
    }

    public QuadTreePlace getBottomRight() {
        return bottomRight;
    }

    public Boundary getBoundary() {
        return boundary;
    }

    /**
     * Find the destination place of a given point
     * @param p
     * @return the destination QuadTreePlace
     */
    public QuadTreePlace getDestinationTree(Point p) {
        // if a child can't contain an object, it will live in this quad
        QuadTreePlace destTree = this;
        double[] coordinates = p.getCoordinates();
        if (QuadTreeUtilities.withinBoundary(coordinates, topLeft.getBoundary())) {
            destTree = topLeft;
        }
        if (QuadTreeUtilities.withinBoundary(coordinates, topRight.getBoundary())) {
            destTree = topRight;
        }
        if (QuadTreeUtilities.withinBoundary(coordinates, bottomLeft.getBoundary())) {
            destTree = bottomLeft;
        }
        if (QuadTreeUtilities.withinBoundary(coordinates, bottomRight.getBoundary())) {
            destTree = bottomRight;
        }
        return destTree;
    }

    public int getDimensions() {
        return dimensions;
    }

    public int getGranularity() {
        return granularity;
    }

    public double[] getInterval() {
        return interval;
    }

    public boolean getIsLeaf() {
        return isLeaf;
    }

    public int getLevel() {
        return level;
    }

    public QuadTreePlace getParent() {
        return parent;
    }

    public Point getPoint() {
        return point;
    }

    public QuadTreePlace getTopLeft() { 
        return topLeft;
    }

    public QuadTreePlace getTopRight() {
        return topRight;
    }

    public Vector<Integer> getTreeIndex() {
        return treeIndex;
    }

    /**
     * Insert point to the current QuadTreePlace.
     * @param p the point to insert
     * @param initArgs the argument for new place instantiation
     * @return the level of the QuadTreePlace where the point is inserted to
     */
    public int insert(Point p, Object initArgs) {

        double[] coordinates = p.getCoordinates();
        if (!QuadTreeUtilities.withinBoundary(coordinates, boundary)) {
            
            return -1;
        } 
            
        // if the current QuadTreePlace is a leaf and point is null
        if (point == null && isLeaf == true) {    
            point = p;
            MASSBase.getLogger().debug("point: " + point.toString() + " inserted." );
            return level;
        }
    
        // if the point is already in the current QuadTreePlace
        if (point != null && QuadTreeUtilities.sameCoordinates(point.getCoordinates(), p.getCoordinates())) {

            return level;
        }
    
        // exceeded the capacity, then split into four
        if (topLeft == null) {
            split(initArgs);
        }

        // get the destination place
        QuadTreePlace destTree = getDestinationTree(p);
        if (destTree == this) {
            point = p;
            MASSBase.getLogger().debug("Point " + point.toString() + " inserted.");
            return destTree.getLevel();
        } else {
            return destTree.insert(p, initArgs);
        }
        //return destTree.getLevel();
    }

    // remove agent from the map when the agent migrates to the next place
    public boolean removeAgent(QuadTreeAgent agent, int[] subIndex) {
        //System.out.print("remove agent id = " + agent.getAgentId());
        //int agentId = agent.getAgentId();
        int[] subSize = new int[dimensions];
        Arrays.fill(subSize, granularity);
        int subIndexLinear = MatrixUtilities.getLinearIndex(subSize, subIndex);  // initialize linear subIndex

        if (agentsMap.containsKey(subIndexLinear)) {
            Set<Agent> agent_list = agentsMap.get(subIndexLinear);
                       
            if (agent_list.remove(agent) ) { 
                    
                return true;
            }
        }
        return false;
    }
    
    // search for destination treePlace where the coordinates resides
    public QuadTreePlace searchDestinationTreePlace(double[] coordinates) {
        MASSBase.getLogger().debug("searchDestinationTreePlace, boundary = " + boundary.toString() + 
            "coordinates = [" + coordinates[0] + "," + coordinates[1] + "]");

        if (isLeaf) {
            return this;
        }
        if (topLeft != null && QuadTreeUtilities.withinBoundary(coordinates, topLeft.getBoundary())) {
            return topLeft.searchDestinationTreePlace(coordinates);
        }
        if (topRight != null && QuadTreeUtilities.withinBoundary(coordinates, topRight.getBoundary())) {
            return topRight.searchDestinationTreePlace(coordinates);
        }
        if (bottomLeft != null && QuadTreeUtilities.withinBoundary(coordinates, bottomLeft.getBoundary())) {
            return bottomLeft.searchDestinationTreePlace(coordinates);
        }
        if (bottomRight != null && QuadTreeUtilities.withinBoundary(coordinates, bottomRight.getBoundary())) {
            return bottomRight.searchDestinationTreePlace(coordinates);
        }
        
        return null;  // if not found, return null
    }

    // find the lowestCommonAncestor which includes the destCoordinates
    public QuadTreePlace searchLowestCommonAncestor(double[] destCoordinates) {
        // if the coordinate in the current treePlace
        if (QuadTreeUtilities.withinBoundary(destCoordinates, boundary)) {
            return this;
        }

        // search in its parent
        if (parent != null) {
            return parent.searchLowestCommonAncestor(destCoordinates);

        }

        // after looking up in all ancestors and no ancestor includes the coordinates, return null
        return null;
    }


    public void setGranularity(int n) {
        this.granularity = n;
    }

    public void setInterval(int granularity) {
        for (int i = 0; i < dimensions; i++) {
            interval[i] = (boundary.getMax()[i] - boundary.getMin()[i]) / (double) granularity;
        }
    }

    /**
     * set the parent place of current place
     * @param treePlace
     */
    public void setParent(QuadTreePlace treePlace) {
        parent = treePlace;
    }

    /**
     * Split the current QuadTreePlace into four new places. 
     * @param initArgs the arguemnt for new place instantiation
     */
    public void split(Object initArgs) {
        //MASSBase.getLogger().debug("split QuadTreePlace " + treeIndex.toString() + ", " + boundary.toString());
        double[] offSet = new double[dimensions];
        double[] min = boundary.getMin();
        double[] max = boundary.getMax();

        for (int i = 0; i < dimensions; i++) {
            offSet[i] = min[i] + (max[i] - min[i]) / 2;
        }

        try {
            // boundary of topLeft subtree
            Boundary boundary_topLeft = new Boundary(new double[]{min[0], offSet[1]}, new double[]{offSet[0], max[1]}); 

            // quadTreeIndex of topLeft subTree
            Vector<Integer> treeIndex_topLeft = (Vector) treeIndex.clone();
            treeIndex_topLeft.add(TOP_LEFT);

            Object[] finalArgs_topLeft = new Object[2];
            QuadTreePlaceArgs treePlaceArgs_topLeft = new QuadTreePlaceArgs(dimensions, level + 1, boundary_topLeft, treeIndex_topLeft, null, this);            
            finalArgs_topLeft[0] = (Object) treePlaceArgs_topLeft;
            finalArgs_topLeft[1] = initArgs;
            
            topLeft = objectFactory.getInstance(this.getClass().getName(), (Object) finalArgs_topLeft);
            MASSBase.getLogger().debug("     topLeft created:" + topLeft.getTreeIndex().toString() + ", " + 
                topLeft.getBoundary().toString() + ", level = " + topLeft.getLevel());
        

            // boundary of topRight subtree
            Boundary boundary_topRight = new Boundary(offSet, max);

            // treeIndex of topRight subTree
            Vector<Integer> treeIndex_topRight = (Vector)treeIndex.clone();
            treeIndex_topRight.add(TOP_RIGHT);

            Object[] finalArgs_topRight = new Object[2];
            QuadTreePlaceArgs treePlaceArgs_topRight = new QuadTreePlaceArgs(dimensions, level + 1, boundary_topRight, treeIndex_topRight, null, this);
            finalArgs_topRight[0] = (Object) treePlaceArgs_topRight;
            finalArgs_topRight[1] = initArgs;

            topRight = objectFactory.getInstance(this.getClass().getName(), (Object) finalArgs_topRight);
            MASSBase.getLogger().debug("     topRight created:" + topRight.getTreeIndex().toString() + ", " + 
                topRight.getBoundary().toString() + ", level = " + topRight.getLevel());


            // boundary of bottomLeft subtree
            Boundary boundary_bottomLeft = new Boundary(min,offSet);

            // treeIndex of bottomLeft subTree
            Vector<Integer> treeIndex_bottomLeft = (Vector) treeIndex.clone();
            treeIndex_bottomLeft.add(BOTTOM_LEFT);

            Object[] finalArgs_bottomLeft = new Object[2];
            QuadTreePlaceArgs treePlaceArgs_bottomLeft = new QuadTreePlaceArgs(dimensions, level + 1, boundary_bottomLeft, treeIndex_bottomLeft, null, this);
            finalArgs_bottomLeft[0] = (Object) treePlaceArgs_bottomLeft;
            finalArgs_bottomLeft[1] = initArgs;

            bottomLeft = objectFactory.getInstance(this.getClass().getName(), (Object) finalArgs_bottomLeft);
            MASSBase.getLogger().debug("     bottomLeft created:" + bottomLeft.getTreeIndex().toString() + ", " + 
                bottomLeft.getBoundary().toString() + ", level = " + bottomLeft.getLevel());


            // boundary of bottomRight subtree
            Boundary boundary_bottomRight = new Boundary(new double[]{offSet[0],min[1]}, new double[]{max[0], offSet[1]});

            // treeIndex of bottomRight subTree
            Vector<Integer> treeIndex_bottomRight = (Vector) treeIndex.clone();
            treeIndex_bottomRight.add(BOTTOM_RIGHT);

            Object[] finalArgs_bottomRight = new Object[2];
            QuadTreePlaceArgs treePlaceArgs_bottomRight = new QuadTreePlaceArgs(dimensions, level + 1, boundary_bottomRight, treeIndex_bottomRight, null, this);
            finalArgs_bottomRight[0] = (Object) treePlaceArgs_bottomRight;
            finalArgs_bottomRight[1] = initArgs;

            bottomRight = objectFactory.getInstance(this.getClass().getName(), (Object) finalArgs_bottomRight);
            MASSBase.getLogger().debug("     bottomRight created:" + bottomRight.getTreeIndex().toString() + ", " + 
                bottomRight.getBoundary().toString() + ", level = " + bottomRight.getLevel());

        } catch (Exception e) {
            MASSBase.getLogger().error("TreePlace.split(): {} not loaded and/or instantiated", this.getClass().getName(), e);
        }

        isLeaf = false;

        // replace the point in the current place to appropriate child place
        QuadTreePlace destTree = getDestinationTree(point);

        if (destTree != this) {
            destTree.insert(point, initArgs);
            point = null;  
        }
    }

    //subdivide the quadTreePlace
    public Object subdivideTreePlace() {
       
        MASSBase.getLogger().debug("subdivideTreePlace: " + getTreeIndex().toString());
       
        int maxLevelOfAllTrees = ((QuadTreePlacesBase)MASSBase.getCurrentPlacesBase()).getMaxLevelOfAllTrees();
        MASSBase.getLogger().debug("maxLevelOfAllTrees" + maxLevelOfAllTrees + ", curr tree level = " + getLevel());

        setGranularity((int) Math.pow(2, maxLevelOfAllTrees - level));
        MASSBase.getLogger().debug("tree place: " + treeIndex.toString() + "granularity = " + getGranularity());

        setInterval(granularity);
        MASSBase.getLogger().debug("tree place: " + treeIndex.toString() + "interval = [" + interval[0] + "," + interval[1] + "]");
      
        
        return null;
        
    }
}

