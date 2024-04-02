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

import java.io.Serializable;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.stream.Stream;
import edu.uw.bothell.css.dsl.MASS.clock.GlobalLogicalClock;
import edu.uw.bothell.css.dsl.MASS.matrix.MatrixUtilities;

@SuppressWarnings("serial")
public class PropertyGraphAgent extends Agent {
	protected ArrayList<String> pathResult;

	// args is the pathResult
	public PropertyGraphAgent(Object args) {
        super();
		if(args == null ) {
			this.pathResult = new ArrayList<String>();
		} else {
			this.pathResult = new ArrayList<String>((ArrayList<String>) args);
		}
	}

	public Object getPathResult() {
		return (Object) this.pathResult;
	}

	public Object executeMatch(Object matchArgs) {
		MASS.getLogger().debug("At propertyGraphAgent's executeMatch, path result before executing: " + (pathResult == null ? "null" : pathResult.toString()));
		PropertyVertexPlace place = (PropertyVertexPlace) this.getPlace();
		MASS.getLogger().debug("                                      current place: " + place.getItemID());
		MASS.getLogger().debug("                                      place's nextVertex size: " + place.getNextVertexSize());
		
		int m = (int) pathResult.size();
		MASS.getLogger().debug("                                      current pathResult size: " + m);

		Object[] thisArg = (Object[]) matchArgs;
		Set<String> nodeLabels = thisArg[0] == null ? null : (Set<String>) thisArg[0];
		Map<String, String> nodeProperties = thisArg[1] == null ? null : (Map<String, String>) thisArg[1];
		MASS.getLogger().debug("                                      thisArg: " + nodeLabels + ", " + nodeProperties);
		
		if(place.hasLabelsProperties(nodeLabels, nodeProperties)) {
			pathResult.add(place.getItemID());
			String direction = (String) thisArg[2];
			MASS.getLogger().debug("At PropertyGraphAgent executeMatch, relationship direction: " + direction);
			if(!direction.equals("NULL")) {
				Set<String> relTypes = (Set<String>) thisArg[3];
				Map<String, String> relProperties = (Map<String, String>) thisArg[4];
				place.setNextVertex(direction, relTypes, relProperties);
				this.setNewChildren(place.getNextVertexSize());
				MASS.getLogger().debug("                                    relTypes: " + relTypes);
				MASS.getLogger().debug("                                    relProperties: " + relProperties);
				MASS.getLogger().debug("                                    place's nextVertex size: " + place.getNextVertexSize());
				MASS.getLogger().debug("                                    this agent's new children: " + this.getNewChildren());
			} else {
				place.clearNextVertex();
				this.setNewChildren(0);
			}
		}

		this.setMigrating(false);
		this.kill(); // alive = false
		
		MASS.getLogger().debug("At propertyGraphAgent's executeMatch, path result after executing: " + pathResult.toString());
		return (Object) pathResult;
	}
	
	@Override
	public Object callMethod( int functionId, Object argument ) {
		switch (functionId) {	
			case 0: // match path 
				return executeMatch((Object[]) argument);
			case 1:
				break;
			default:
				break;
		} 
		return null;
	 }

	
}