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
import java.util.Hashtable;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;

import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

public class SpacePlace extends Place{


    private int handle;
    private double[] min;  //min value of the SpacePlace (not whole places)
    private double[] max;

    private double[] subInterval;  // interval of sub-place
    private int granularity;


    // hashtable to keep all agents in the place, key is agent's linearSubIndex, value is a list of agents in that subPlace
    private Hashtable<Integer, Set<Agent>> agentsMap = new Hashtable<>();

    public SpacePlace(Object args) {
        super();

        // initialize SpacePlace
        SpacePlaceArgs spacePlaceArgs = (SpacePlaceArgs) args;

        this.handle = spacePlaceArgs.getHandle();
        this.min = spacePlaceArgs.getPlaceMin();
        this.max = spacePlaceArgs.getPlaceMax();
        this.subInterval = spacePlaceArgs.getSubInterval();
        this.granularity = spacePlaceArgs.getGranularity();

    }

    public int getHandle() {
        return handle;
    }

    public double[] getMin() {
        return min;
    }
    
    public double[] getMax() {
        return max;
    }
    
    public double[] getInterval() {
        double[] interval = new double[min.length];
        for (int i = 0; i < min.length; i++) {
            interval[i] = max[i] - min[i];
        }
        return interval;
    }

    public double[] getSubInterval() {
        return subInterval;
    }
    
    public int getGranularity() {
        return granularity;
    }

    public int getLinearIndex() {

        return MatrixUtilities.getLinearIndex(getSize(), getIndex());
    }
    
    
    public void addAgent(Agent agent, int[] subIndex) {
          
        // get the linear subindex
        int[] subSize = new int[subIndex.length];
        Arrays.fill(subSize, granularity);
        int linearSubIndex = MatrixUtilities.getLinearIndex(subSize, subIndex);  // initialize linear subIndex
        Set<Agent> agent_list = agentsMap.getOrDefault(linearSubIndex, new HashSet<Agent>());
        agent_list.add(agent);
        agentsMap.put(linearSubIndex, agent_list);

    }
    
  
    
    // remove agent from the map when the agent migrates to the next place
    public boolean removeAgent(Agent agent, int[] subIndex) {
        int dimensions = subIndex.length;
        int agentId = agent.getAgentId();
        int[] subSize = new int[dimensions];
        Arrays.fill(subSize, granularity);
        int subIndexLinear = MatrixUtilities.getLinearIndex(subSize, subIndex);  // initialize linear subIndex
        
        if (agentsMap.containsKey(subIndexLinear)) {
            Set<Agent> agent_list = agentsMap.get(subIndexLinear);
                       
            if ((!agent_list.isEmpty()) && agent_list.remove(agent) ) {      
                return true;
            }
        }
        return false;
    }
    
    //update agentsMap when agent migrate from one sub-place to another within SpacePlace
    public boolean updateAgent(Agent agent, int[] oldSubIndex, int[] newSubIndex) {
        if (removeAgent(agent, oldSubIndex)) {
            
            addAgent(agent, newSubIndex);
            return true;
        } else {
            return false;
        }    
    }


    public Hashtable<Integer, Set<Agent>> getAgentsMap() {
        return agentsMap;
    }
    
    public String agentMap_toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SpacePlace index = [" + getIndex()[0] + "," + getIndex()[1] + "]\n");
        for (Integer index : agentsMap.keySet()) {
            sb.append(index + ": (");
            Set<Agent> s = agentsMap.get(index);
            for (Agent agent : s) {
                sb.append(agent.getAgentId() + "(" + ((SpaceAgent)agent).getOriginalId() + "), ");
            }
            sb.append(")\n");
        }
        return sb.toString();
    }

    public String toString() {
        return " index = [" + getIndex()[0] + ", " + getIndex()[1] + "], handle = " + handle + ", granularity = " + granularity + 
            ", min = [" + min[0] + ", " + min[1] + "], max = " + max[0] + ", " + max[1] + "], subInterval = [" + subInterval[0] + 
            ", " + subInterval[1] + "]";
    }


}
