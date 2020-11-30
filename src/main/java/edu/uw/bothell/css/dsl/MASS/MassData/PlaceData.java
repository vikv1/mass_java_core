/*

 	MASS Java Software License
	© 2012-2020 University of Washington

	Permission is hereby granted, free of charge, to any person obtaining a copy
	of this software and associated documentation files (the "Software"), to deal
	in the Software without restriction, including without limitation the rights
	to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
	copies of the Software, and to permit persons to whom the Software is
	furnished to do so, subject to the following conditions:

	The above copyright notice and this permission notice shall be included in
	all copies or substantial portions of the Software.

	The following acknowledgment shall be used where appropriate in publications, presentations, etc.:      

	© 2012-2020 University of Washington. MASS was developed by Computing and Software Systems at University of 
	Washington Bothell.

	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
	THE SOFTWARE.

*/

package edu.uw.bothell.css.dsl.MASS.MassData;

import java.io.Serializable;

/**
 * Created by Nicolas on 8/10/2015.
 */
@SuppressWarnings("serial")
public class PlaceData extends MASSPacket implements Serializable
{
    //this places data
    private Number thisPlaceData;

    //this places index
    private int index;

    //if this place contains agents
    private boolean hasAgents = false;

    //an array of agent data residing on this place
    private AgentData[] agentDataOnThisPlace;

    public Number getThisPlaceData() {
        return thisPlaceData;
    }

    public void setThisPlaceData(Number thisPlaceData) {
        this.thisPlaceData = thisPlaceData;
    }

    public boolean isHasAgents() {
        return hasAgents;
    }

    public void setHasAgents(boolean hasAgents) {
        this.hasAgents = hasAgents;
    }

    public AgentData[] getAgentDataOnThisPlace() {
        return agentDataOnThisPlace;
    }

    public void setAgentDataOnThisPlace(AgentData[] agentDataOnThisPlace) {
        this.agentDataOnThisPlace = agentDataOnThisPlace;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toJSONString() {
        String jsonString =  "{\"Number\":0,\"index\":0,\"hasAgents\":false,\"AgentArray\":[";
        
        jsonString += agentDataOnThisPlace[0].toJSONString();
        
        for(int i = 1; i < agentDataOnThisPlace.length; i++)
        {
            jsonString += "," + agentDataOnThisPlace[i].toJSONString();
        }
        
        jsonString += "]}";
        
        return jsonString;
        
    }
}
