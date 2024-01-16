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
import java.util.Vector;

public class PropertyVertexPlace extends VertexPlace {
    private String nodeName = null;
    private Set<String> labels = new HashSet<String>();
    private Map<String,String> nodeProperties = new HashMap<>();  // to store node properties
    // public Vector<Object> neighbors = new Vector<>(); // declared in VerterPlace.java
    private Map<Object, String> relationNames = new HashMap<Object, String>();
    private Map<Object, Set<String>> relationTypes = new HashMap<Object, Set<String>>();
    private Map<Object, Map<String,String>> relationProperties = new HashMap<Object, Map<String,String>>(); // to store relationship properties

    public PropertyVertexPlace() {
        super();

        MASSBase.getLogger().debug("PropertyVertexPlace constructed.");
    }

    public void setNodeName(String name) {
        this.nodeName = name;
    }

    public String getNodeName(){
        return this.nodeName;
    }

    public void addLabel(String label){
        labels.add(label);
    }
    
    public void setLabels( List<String> labels) {
        for(String s: labels) {
            this.addLabel(s);
        }
    }

    public boolean hasLabels(String labelString){
        if(labelString == null || labelString == "") {
            return true;
        }

        String[] targetLabels = labelString.split(",");

        for(String label: targetLabels) {
            if(!this.labels.contains(label)){
                return false;
            }
        }
        return true;
    }

    public Set<String> getLabels(){
        return this.labels;
    }

    public void setNodeProperties(Map<String,String> properties) {
        this.nodeProperties = properties;
    }

    public Map<String,String> getNodeProperties() {
        return this.nodeProperties;
    }

    // nodeProperties string format {key=value, key=value}
    public Boolean hasNodeProperties(String argument) {
        if(argument == null || argument == "") {
            return true;
        }
        String[] args = argument.split(",");

        for(String arg: args){
            if(arg == "") {
                continue;
            }
            String[] s = arg.split("=");
            if(s.length != 2) {
                System.err.println("Cannot match node properties, as property argument format is invalid");
                return false;
            }
            String key = s[0];
            String value = s[1];
            if(!this.nodeProperties.containsKey(key) || this.nodeProperties.get(key) != value){
                return false;
            }
        }
        return true;
    }

    public String getRelationName(Object neighborID){
        return this.relationNames.get(neighborID);
    }

    public Map<Object,String> getRelationNames(){
        return this.relationNames;
    }

    public Vector<Object> getAllNeighbors(){
        return this.neighbors;
    }

    public boolean hasRelationType(Object neighborID, String type){
        return this.relationTypes.get(neighborID).contains(type);
    }

    public Map<Object, Set<String>> getRelationTypes(){
        return this.relationTypes;
    }

    public boolean hasRelationProperty(Object neighborID, String propertyKey) {
        return this.relationProperties.get(neighborID).containsKey(propertyKey);
    }

    public String getRelationProperty(Object neighborID, String propertyKey) {
        if(this.hasRelationProperty(neighborID, propertyKey)){
            return this.relationProperties.get(neighborID).get(propertyKey);
        } else {
            return null;
        }
    }

    public boolean addNeighbor(Object neighborItemName) {
        if (neighbors.contains(neighborItemName)) {
            return false;
        } else{
            neighbors.add(neighborItemName);
            return true;
        }
    }

    public void setNeighborRelationTypes(Object neighborItemName, List<String> relationTypes) {
        if (!this.relationTypes.containsKey(neighborItemName)) {
            Set<String> types = new HashSet<String>(relationTypes);
            this.relationTypes.put(neighborItemName, types);
        } else {
            Set<String> types = this.relationTypes.get(neighborItemName);
            for(String s: relationTypes) {
                types.add(s);
            }
        }
    }

    public void setNeighborProperties(Object neighborItemName, Map<String, String> relationProperties){
        if (!this.relationProperties.containsKey(neighborItemName)) {
            this.relationProperties.put(neighborItemName, relationProperties);
        } else {
            Map<String,String> currentProperty = this.relationProperties.get(neighborItemName);
            for(Map.Entry<String,String> entry : relationProperties.entrySet()){
                currentProperty.put(entry.getKey(), entry.getValue());
            }
        }
    }
    
    public Map<String,String> getNeighborProperties(Object neighborId) {
        return this.relationProperties.get(neighborId);
    }

    public Map<Object, Map<String,String>> getAllNeighborProperties() {
        return this.relationProperties;
    }

    /**
	 * Is called from Places.callAll( ), callSome( ), exchangeAll( ), and
	 * exchangeSome( ), and invoke the function specified with functionId as
	 * passing arguments to this function. A user-derived Place class must
	 * implement this method.
	 * @param functionId The ID number of the function to invoke
	 * @param argument An argument that will be passed to the invoked function
	 * @return Always returns NULL
	 */
    @Override
	public Object callMethod( int functionId, Object argument ) {
		Object result = this.nodeName;
        if(argument == null) return result;

        switch (functionId) {
            case 1: // Match Node Label
                result = this.hasLabels((String) argument) ? result : null;
                break;
            case 2: // Match Node Properties, proprety argument format(key,value;key;value)
                result = this.hasNodeProperties((String) argument) ? result : null;
                break;
            default:
                break;
        }

        return (Object) result;
	}
    
}
