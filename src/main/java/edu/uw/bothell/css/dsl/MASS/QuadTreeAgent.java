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
import java.util.Objects;
import java.util.Vector;

import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

@SuppressWarnings("serial")
public class QuadTreeAgent extends Agent {

    public static final int MAX_AGENTS_PER_NODE = AgentSerializer.getInstance().getMaxNumberOfAgents(); 

    private int generation;
    private double[] originalCoordinates;  // the original point of the agent
    
    private double[] newCoordinates; // new coordinates where agent is migrating
    private int originalAgentId;  // the source's of agent spawned from
    private boolean isParent; 
    private Vector<Integer> treeAgentIndex;
    private int[] treeAgentSubIndex;
    private int subIndexLinear;

    //default constructor
    public QuadTreeAgent() { super();}
    
    
    public QuadTreeAgent(Object args) {
        super();
        this.isParent = false;
        QuadTreeAgentArgs treeAgentArgs = (QuadTreeAgentArgs)args;
        
        originalCoordinates = treeAgentArgs.getOriginalCoordinates();
        setCurrentCoordinates(treeAgentArgs.getCurrentCoordinates());
        newCoordinates = treeAgentArgs.getNewCoordinates();
        Vector<Integer> index = treeAgentArgs.getAgentIndex();
        treeAgentSubIndex = treeAgentArgs.getAgentSubIndex();
        setAgentIndex(index);
        this.generation = treeAgentArgs.getGeneration();
  
    }
    
    public String displaySubIndex() {
        return "[" + treeAgentSubIndex[0] + "," + treeAgentSubIndex[1] + "]";
    }
    
    // get the agent index which is the index of the QuadTreePlace it resides
    public Vector<Integer> getAgentIndex() {
        return treeAgentIndex;
    }   
    

    public int getGeneration() {
        return generation;
    }
    

    
    public double[] getNewCoordinates() {
        return newCoordinates;
    }
    

    public int getOriginalAgentId() {
        return originalAgentId;
    }
    
    public double[] getOriginalCoordinates() {
        return originalCoordinates;
    }

    // get the subIndex of the sub-place where agent resides
    public int[] getSubIndex() {
        return treeAgentSubIndex;
    }

    public Object killParent(Object args){
        if(isParent){
            kill();
        }
        return null;
    }

    //migrate agent to a specific coordinates
    public Object migrate(double[] newCoordinates) {

        MASS.getLogger().debug("QuadTreeAgent.java migrate(), newCoordinates = " + newCoordinates[0] + "," + newCoordinates[1]);

        // invalid coordinates!
        Objects.requireNonNull( "QuadTreeAgent.java migrate(), newCoordinates = " + newCoordinates, "Must provide an coordinates when migrating!" );
        
        MASSBase.getLogger().debug("call migration on agent : " + getAgentId() + "(" + getOriginalAgentId() + ")" + 
                 ", new coordinates: [" + newCoordinates[0] + "," + newCoordinates[1] + "]");
        
        //boundary of all quad trees
        Boundary boundary = ((QuadTreePlacesBase) MASSBase.getCurrentPlacesBase()).getBoundaryAll();
        MASSBase.getLogger().debug("QuadTreeAgent migrate() : " + boundary.toString());

        
        if (!QuadTreeUtilities.withinBoundary(newCoordinates, boundary)) {

            //if agent migrates out of boundary, kill the agent
            MASSBase.getLogger().debug("QuadTreeAgent " + getAgentId() + "(" + getOriginalAgentId() + ")" + " migrates out of boundary"); 
            kill();
        } else {

            //else migrate the agent if it is not the same as current coordinates
            if (!QuadTreeUtilities.sameCoordinates(getCurrentCoordinates(), newCoordinates)) {
                setCurrentCoordinates(newCoordinates); 
                setMigrating(true);  
            } else {
                setMigrating(false);
            }
         }
         return null;
    }

	/**
     * propagate agent
     * @param argument
     * @return
     */
     public Object propagateAgent(Object argument) {
        
        if (generation % 2 != 0) { 
            // if generation is even, spawn to N, W, E, S
            spawnAgent("moore", argument);
            
        } else {
            // if generation is odd, spawn to N, W, E, S, NW, SW, NE, SE
            spawnAgent("vonNeumann", argument);
        }
        
        return null;
    }
    
    public void setAgentIndex(Vector<Integer> index) {
        treeAgentIndex = index;
    }

    // set originalAgentId
	public void setOriginalAgentId(int id) {
		this.originalAgentId = id;
	}


    public void setSubIndex(int[] subIndex) {
        treeAgentSubIndex = (int[]) subIndex.clone();
        setMigrating(false);
    }
    
    
    /**
	 * Set the reference to the QuadTreePlace (both index and sub-index) 
     * where this Agent is located  ------- modified by Yuna
	 * @param place The current Place where this Agent resides
	 */
	public void setTreePlace(QuadTreePlace place, int[] subIndex, double[] destCoordinates) {
		
		// set the Place
		setPlace( place );
		
		if ( place != null ) {
			
			// set this Agent's index (index is not transient...)
			this.treeAgentIndex = place.getTreeIndex();
            this.treeAgentSubIndex = subIndex;
            setCurrentCoordinates(destCoordinates);

			// reset migration flag (have arrived at a Place and no longer migrating)
			setMigrating(false);

		}
    }
    

    //spawn agent
    public Object spawnAgent(String neighborhoodPattern, Object args) {

        int numOfAgents = 0;
        switch(neighborhoodPattern){
            case "moore": 
                numOfAgents = 8;
                break;
            
            case "vonNeumann": 
                numOfAgents = 4;
                break;
                
        }
        
        Object[] arguments = new Object[numOfAgents];
        
        double[] tempCoordinates = getCurrentCoordinates().clone();

        // new coordiantes where agent is migrating
        double[] interval = ((QuadTreePlace) getPlace()).getInterval();
        Vector<double[]> neighbors = QuadTreeUtilities.getNeighborPatterns(neighborhoodPattern, interval);

        for (int i = 0; i < neighbors.size(); i++ ) {
            for (int j = 0; j < interval.length; j++) {
                neighbors.get(i)[j] += tempCoordinates[j];
            }
        }

        int newGeneration = generation + 1;

        for(int i = 0; i < numOfAgents; i++){

            Object[] finalArgs = new Object[2];
            QuadTreeAgentArgs treeAgentArgs = new QuadTreeAgentArgs(originalCoordinates, getCurrentCoordinates(), neighbors.get(i), 
                                    treeAgentIndex, treeAgentSubIndex, newGeneration);

            finalArgs[0] = (Object) treeAgentArgs;  //argument for SpaceAgent
            finalArgs[1] = args;  //argument for customer agent class
            arguments[i] = (Object) finalArgs;

        }

        spawn(numOfAgents, arguments);
        isParent = true;
        return null;
    }

    // return the agent's info as a string
    public String toString() {
        
        // get the linear subindex
        int[] subSize = new int[this.originalCoordinates.length];
        Arrays.fill(subSize, 2);
        this.subIndexLinear = MatrixUtilities.getLinearIndex(subSize, treeAgentSubIndex);  // initialize linear subIndex
        
        StringBuilder sb = new StringBuilder();
        sb.append("------------------Agent ID = " + getAgentId() + "(" + originalAgentId + ") --------------------\n");
        sb.append("isMigrating: " + isMigrate_toString() + "\n"); 
        sb.append("Agent original coordinates = [" + getOriginalCoordinates()[0] + ", " + getOriginalCoordinates()[1] + "]\n");
        sb.append("Agent current coordinates = [" + getCurrentCoordinates()[0] + ", " + getCurrentCoordinates()[1] + "]\n");
        sb.append("Agent new coordinates = [" + getNewCoordinates()[0] + ", " + getNewCoordinates()[1] + "]\n");
        sb.append("Agent index = " + getAgentIndex().toString());
        sb.append("Agent sub index linear = " + subIndexLinear + "\n");
        return sb.toString();
        
    }
    

}
