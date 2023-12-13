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

package edu.uw.bothell.css.dsl.MASS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PropertyVertexPlace extends VertexPlace {
    private Map<String,String> nodeProperties = new HashMap<>();  // to store node properties
    private Map<Object,Map<String,String>> relationProperties = new HashMap<>(); // to store relationship properties
    private Set<String> labels = new HashSet<>();

    public PropertyVertexPlace() {
        super();

        MASSBase.getLogger().debug("PropertyVertexPlace constructed.");
    }

    public void setProperties(Map<String,String> properties) {
        this.nodeProperties = properties;
    }

    public Map<String,String> getProperties() {
        return this.nodeProperties;
    }

    public void setLabels( List<String> labels) {
        this.labels = new HashSet<String>(labels);
    }

    public Set<String> getLabels() {
        return this.labels;
    }

    public Map<Object,Map<String,String>> getAllNeighborProperties() {
        return this.relationProperties;
    }

    public Map<String,String> getNeighborProperties(Object neighborId) {
        return this.relationProperties.get(neighborId);
    }

    public void setNeighborProperties(Object neighborVertexId, Map<String,String> newRelationProperty) throws IllegalArgumentException {
        if (!relationProperties.containsKey(neighborVertexId)) {
            Map<String,String> property = new HashMap<>();
            relationProperties.put(neighborVertexId, property);
        }

        Map<String,String> currentProperty = relationProperties.get(neighborVertexId);
        
        for (Map.Entry<String,String> entry : newRelationProperty.entrySet()){
            currentProperty.put(entry.getKey(), entry.getValue());
        }
        // System.out.println("Adding relationship to: " + neighborId + ", property of " + relationProperties.get(neighborId));
    }

    
}
