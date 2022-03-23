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
public class SpaceAgent extends SmartAgent {
    
    public static final int MIGRATE = 0; 
    public static final int SPAWN_AGENTS = 1;
    public static final int KILL_PARENTS =2;
    public static final int PROPAGATE_AGENTS = 3;
    public static final int KILL_DUPLICATES = 4;
    
    private double[] currentCoordinates;
    private double[] nextCoordinates;  //coordinates where agent is migrating to
    private double[] originalCoordinates;  //the original coordinates of the agent
    private int[] subIndex; //subIndex of sub-place where SpaceAgent resides
    private int generation;
    private int originalId;
    private boolean isParent = false; //flase when agent instantiated, once the agent spawns, it turns to true

    public SpaceAgent(Object args) {
        
        // initialize SpaceAgent
        super();
        SpaceAgentArgs spaceAgentArgs = (SpaceAgentArgs) args;
        this.currentCoordinates = spaceAgentArgs.getCurrentCoordinates().clone();
        this.nextCoordinates = spaceAgentArgs.getNextCoordinates().clone();
        this.originalCoordinates = spaceAgentArgs.getOriginalCoordinates().clone();
        this.subIndex = spaceAgentArgs.getSubIndex().clone();
        this.generation = spaceAgentArgs.getGeneration();
        this.originalId = spaceAgentArgs.getOriginalId();
        setIndex(spaceAgentArgs.getIndex());
    }

    

    public double[] getCurrentCoordinates() {
        return currentCoordinates;
    }

    public double[] getNextCoordinates() {
        return nextCoordinates;
    }

    public int[] getSubIndex() {
        return this.subIndex;
    }

    public void setSubIndex(int[] subIndex) {
        this.subIndex = subIndex;
    }


	public void setCurrentCoordinates(double[] coordinates) {
		currentCoordinates = coordinates;
    }

    //this method is only applicable right after agent instantiation (no migration called)
    public Object setOriginalCoordinates(double[] originalCoordinates) {
        this.originalCoordinates = originalCoordinates.clone();
        return null;
    }

    public double[] getOriginalCoordinates() {
        return originalCoordinates;
    }

    public int getGeneration() {
        return this.generation;
    }
    
    public int getOriginalId() {
        return this.originalId;
    }

    public boolean getParentStatus() {
        return isParent;
    }

    public Object migrate(double[] newCoordinates) { 

        // invalid index!
        Objects.requireNonNull(newCoordinates, "Must provide an index when migrating!" );
        
        int placesHandle = ((SpacePlace) this.getPlace()).getHandle();
        //MASS.getLogger().debug("SpaceAgent.java migrate(), placesHandle = " + placesHandle);
        SpacePlacesBase curPlaces = (SpacePlacesBase) (MASSBase.getPlacesMap().get(placesHandle));
        //MASS.getLogger().debug("SpaceAgent.java migrate(), min = [" + curPlaces.getMin()[0] + "," + curPlaces.getMin()[1] + 
        //    "].");
        int[] destIndex = SpaceUtilities.findDestIndex(newCoordinates, curPlaces);
        if (destIndex[0] == -1) {
            //out of Boundary
            kill();
            return null;
        } else {
            if (!SpaceUtilities.sameCoordinates(getCurrentCoordinates(), newCoordinates)) { 
            
                //coordinates, index and subIndex are not updated here, they are updated in manageAll
                //because manageAll_space needs current coordinates, index and subIndex for removing 
                //agent from agents map
                
                setMigrating(true);  
            } else {
                setMigrating(false);
            }
        }
        MASS.getLogger().debug("agent id = " + getAgentId() + ", isMigrating() = " + isMigrating());
        return null;
    }   

    //agent spawns in 'moore' manner or "vonNewmann" manner
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
        int[] curIndex = getIndex();
        double[] tempCoordinates = getCurrentCoordinates().clone();

        // new coordiantes where agent is migrating
        double[] subInterval = ((SpacePlace) getPlace()).getSubInterval().clone();
        Vector<double[]> neighbors = SpaceUtilities.getNeighborPatterns(neighborhoodPattern, subInterval);

        for (int i = 0; i < neighbors.size(); i++ ) {
            for (int j = 0; j < subInterval.length; j++) {
                neighbors.get(i)[j] += tempCoordinates[j];
            }
        }
        generation++;
        for(int i = 0; i < numOfAgents; i++){
            SpacePlace sp = (SpacePlace)getPlace();
            
            Object[] finalArgs = new Object[2];

            //Commented By Vishnu as a part of Auto-Agent Migration
            //SpaceAgentArgs spaceAgentArgs = new SpaceAgentArgs(getCurrentCoordinates(), neighbors.get(i),
            //                        getIndex(), getSubIndex(), generation, getOriginalId());
            SpaceAgentArgs spaceAgentArgs = new SpaceAgentArgs(getCurrentCoordinates(), neighbors.get(i), getOriginalCoordinates(),
                                    getIndex(), getSubIndex(), generation, getOriginalId());

            finalArgs[0] = (Object) spaceAgentArgs;  //argument for SpaceAgent
            finalArgs[1] = args;  //argument for customer agent class
            arguments[i] = (Object) finalArgs;
           
        }
        spawn(numOfAgents, arguments);
        isParent = true;
        return null;
    }

    //agent propagates which spawns in moore and vonNeumann manner alternatively
    public Object propagateAgent(Object argument) {
        
        if (generation % 2 == 0) { 
            // if generation is even, spawn to N, W, E, S
            spawnAgent("vonNeumann", argument);
            
        } else {
            // if generation is odd, spawn to N, W, E, S, NW, SW, NE, SE
            spawnAgent("moore", argument);
        }
        return null;
    }   

    public Object killParent(Object args){
        if(isParent){
            kill();
            MASS.getLogger().debug("agent id = " + getAgentId() + " isParent = " + isParent + " is killed.");
        }
        return null;
    }



    //Spawns Child Agents in Moore and Von-Neumann Neighborhood and then kill the parent agent
    //Propagate Ripple Auto-Agent Migration
    public Object propagateRipple(Object argument) {

        if (getPlace() != null && !(getPlace() instanceof SpacePlace)) {

            MASSBase.getLogger().error("Requested PropagateRipple but places is {"
                    + getPlace() .getClass().getName() + "} and not SpacePlace.");

            return null;
        }

        if (generation % 2 == 0) {
            // if generation is even, spawn to N, W, E, S
            spawnAgentinNeighborhood("vonNeumann", argument, currentCoordinates);

        } else {
            // if generation is odd, spawn to N, W, E, S, NW, SW, NE, SE
            spawnAgentinNeighborhood("moore", argument, currentCoordinates);
        }

        isParent = true;
        kill(); //Kill Parent Agent;
        MASS.getLogger().debug("agent id = " + getAgentId() + " isParent = " + isParent + " will be killed.");

        return null;
    }

    //agent spawns in 'moore' manner or "vonNewmann" manner
    private Object spawnAgentinNeighborhood(String neighborhoodPattern, Object args, double[] currentCoordinates) {
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
        int[] curIndex = getIndex();

        // new coordiantes where agent is migrating
        double[] subInterval = ((SpacePlace) getPlace()).getSubInterval().clone();
        Vector<double[]> neighbors = SpaceUtilities.getNeighborPatterns(neighborhoodPattern, subInterval);

        for (int i = 0; i < neighbors.size(); i++ ) {
            for (int j = 0; j < subInterval.length; j++) {
                neighbors.get(i)[j] += currentCoordinates[j];
            }
        }

        generation++;
        for(int i = 0; i < numOfAgents; i++){
            SpacePlace sp = (SpacePlace)getPlace();

            Object[] finalArgs = new Object[2];
            SpaceAgentArgs spaceAgentArgs = new SpaceAgentArgs(currentCoordinates, neighbors.get(i),originalCoordinates,
                    getIndex(), subIndex, generation, originalId);

            finalArgs[0] = (Object) spaceAgentArgs;  //argument for SpaceAgent
            finalArgs[1] = args;  //argument for customer agent class
            arguments[i] = (Object) finalArgs;

        }
        spawn(numOfAgents, arguments);

        return null;
    }
    
}
