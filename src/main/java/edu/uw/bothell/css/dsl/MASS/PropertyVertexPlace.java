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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Queue;

public class PropertyVertexPlace extends VertexPlace {
    private String ItemID = null; // to store Unique ItemID of each Node/Vertex.
    private Set<String> labels; // to store Node/Vertex labels
    private Map<String,String> nodeProperties;  // to store Node/Vertex properties
    private Map<Object, Object[]> toRelationship; // to store TO direction relationship types & properties
    private Map<Object, Object[]> fromRelationship; // <ItemID, Object[Set<String> types, Map<String, String> properties]>
    private List<Integer> nextVertex;  // Store the vertID of the vertexes that Agent will Spawn and Migrate to.

    
    public PropertyVertexPlace(Object itemID, List<String> labels, Map<String, String> properties) {
        super();
        this.setItemID(((String)itemID).trim().toLowerCase());
        this.labels = new HashSet<String>();
        this.setLabels(labels);
        this.nodeProperties = new HashMap<>();
        this.setNodeProperties(properties);
        this.nodeProperties.put("vertexid", ((String) itemID).trim().toLowerCase());
        this.toRelationship = new HashMap<>(); // <Object ID, Object[Set<String>, Map<String, String>]>
        this.fromRelationship = new HashMap<>(); 
        this.nextVertex = new ArrayList<Integer>(); // for migration, store vertID to be migrated to.
        MASSBase.getLogger().debug("PropertyVertexPlace constructed.");
    }

    public void setItemID(String name) {
        this.ItemID = name.trim();
    }


    public String getItemID(){
        return this.ItemID;
    }

    public void clearNextVertex() {
        this.nextVertex.clear();
    }

    public int getNeighborByIndex(int index) {
        if(index > this.getNextVertexSize()) return -1;
        return this.nextVertex.get(index);
    }

    public int getNextVertexSize(){
        return this.nextVertex.size();
    }

    public void setNextVertex(String direction, Set<String> relTypes, Map<String, String> relProperties) {
        if(nextVertex.size() != 0) return; // only one agent need to update nextVertex
        if(direction.equals("both") || direction.equals("unspecified")){ // ==
            setTONeighborVertex(relTypes, relProperties);
            setFROMNeighborVertex(relTypes, relProperties);
        } else if(direction.equals("out")) { // ==>
            setTONeighborVertex(relTypes, relProperties);
        } else if(direction.equals("in")) { // <==
            setFROMNeighborVertex(relTypes, relProperties);
        } else {
            this.nextVertex.clear();
        } 
        MASS.getLogger().debug("At PropertyVertexPlace, setNextVertex, nextVertex size: " + this.nextVertex.size());
    }

    protected void setTONeighborVertex(Set<String> relTypes, Map<String, String> relProperties) {
        for(Map.Entry<Object, Object[]> entry : this.toRelationship.entrySet()) {
            String neighborID = (String) entry.getKey();
            Object[] edgeLabelProperties = entry.getValue();
            if(edgeSatisfy(edgeLabelProperties, relTypes, relProperties)) {
                int vertID = MASSBase.distributed_map.getOrDefault(neighborID, -1);
                if (vertID == -1) {
                    MASS.getLogger().debug("neighbor vertex ID doesn't exist in MASS library");
                    return;
                }
                this.nextVertex.add(vertID);
            }
        }
    }

    public void setFROMNeighborVertex(Set<String> relTypes, Map<String, String> relProperties) {
        for(Map.Entry<Object, Object[]> entry : this.fromRelationship.entrySet()) {
            String neighborID = (String) entry.getKey();
            Object[] edgeLabelProperties = entry.getValue();
            if(edgeSatisfy(edgeLabelProperties, relTypes, relProperties)) {
                int vertID = MASSBase.distributed_map.getOrDefault(neighborID, -1);
                if (vertID == -1) {
                    MASS.getLogger().debug("neighbor vertex ID doesn't exist in MASS library");
                    return;
                }
                this.nextVertex.add(vertID);
            }
        }
    }

    protected boolean edgeSatisfy(Object[] edgeProperties, Set<String> relTypes, Map<String, String> relProperties) {
        Set<String> oriTypes = edgeProperties[0] == null ? null : ((Set<String>) edgeProperties[0]) ;
        Map<String, String> oriProperties = edgeProperties[1] == null ? null : ((Map<String, String>) edgeProperties[1]);

        return hasTypes(oriTypes, relTypes) && hasProperties(oriProperties, relProperties); 
    }
    
    public boolean hasTypes(Set<String> oriTypes, Set<String> relTypes){
        
        if(relTypes == null || relTypes.size() == 0) {
            return true;
        }
        if(oriTypes == null || oriTypes.size() == 0) {
            return false;
        }
        for(String type: relTypes) {
            if(!oriTypes.contains(type)){
                return false;
            }
        }
        return true;
    }

    public Boolean hasProperties(Map<String,String> oriProperties, Map<String,String> targetProperties) {
        if(targetProperties == null || targetProperties.size() == 0) {
            return true;
        }
        if(oriProperties == null || oriProperties.size() == 0) {
            return false;
        }
        for(Map.Entry<String,String> entry : targetProperties.entrySet()){
           if(!oriProperties.containsKey(entry.getKey()) || !oriProperties.get(entry.getKey()).equals(entry.getValue())){
                return false;
            }
        }
        return true;
    }


    public void addLabel(String label){
        labels.add(label.trim().toLowerCase());
    }
    
    public void setLabels( List<String> labels) {
        for(String s: labels) {
            this.addLabel(s.trim().toLowerCase());
        }
    }

    public boolean hasLabels(Set<String> targetLabels){
        
        if(targetLabels == null || targetLabels.size() == 0) {
            return true;
        }

        // MASS.getLogger().debug("Labels set: " + targetLabels);
        // MASS.getLogger().debug("Labels size: " + targetLabels.size());

        for(String label: targetLabels) {
            // MASS.getLogger().debug("Label: " + label);
            if(!this.labels.contains(label)){
                // MASS.getLogger().debug("No label: " + label);
                return false;
            }
        }
        return true;
    }

    public Set<String> getLabels(){
        return this.labels;
    }

    public void setNodeProperties(Map<String,String> properties) {
        for(Map.Entry<String, String> entry : properties.entrySet()) {
            this.nodeProperties.put(entry.getKey(), entry.getValue());
        }
    }

    public Map<String,String> getNodeProperties() {
        return this.nodeProperties;
    }

    // nodeProperties string format {key=value, key=value}
    public Boolean hasNodeProperties(Map<String,String> mapProperties) {
        if(mapProperties == null || mapProperties.size() == 0) {
            return true;
        }

        for(Map.Entry<String,String> entry : mapProperties.entrySet()){
           if(!this.nodeProperties.containsKey(entry.getKey()) || !this.nodeProperties.get(entry.getKey()).equals(entry.getValue())){
                return false;
            }
        }
        return true;
    }

    public boolean hasLabelsProperties(List<Object> argument) {
        Set<String> labels = (Set<String>) argument.get(0);
        Map<String, String> nodeProperties = (Map<String, String>) argument.get(1);

        return this.hasLabelsProperties(labels, nodeProperties);
    }

    public boolean hasLabelsProperties(Set<String> labels, Map<String, String> properties) {
        return this.hasLabels(labels) && this.hasNodeProperties(properties);
    }

    public void setTONeighborRelation(Object neighborID, List<String> relationTypes,  Map<String, String> relationProperties) {
        if (!this.toRelationship.containsKey(neighborID)) {
            Set<String> types = new HashSet<String>();
            for(String rs: relationTypes) {
                types.add(rs.trim().toLowerCase());
            }
            Map<String, String> mapProperties = new HashMap<>();
            for(Map.Entry<String, String> entry: relationProperties.entrySet()) {
                mapProperties.put(entry.getKey().trim().toLowerCase(), entry.getValue().trim().toLowerCase());
            }

            Object[] relation = new Object[2];
            relation[0] = (Object) types;
            relation[1] = (Object) mapProperties;

            toRelationship.put(neighborID, relation);
        } else {
            Object[] relation = this.toRelationship.get(neighborID);
            Set<String> types = (Set<String>) relation[0];
            Map<String, String> mapProperties = (Map<String, String>) relation[1];
            for(String s: relationTypes) {
                types.add(s.trim().toLowerCase());
            }
            for(Map.Entry<String, String> entry: relationProperties.entrySet()) {
                mapProperties.put(entry.getKey().trim().toLowerCase(), entry.getValue().trim().toLowerCase());
            }
            relation[0] = (Object) types;
            relation[1] = (Object) mapProperties;
            toRelationship.put(neighborID, relation);
        }
        MASS.getLogger().error("At PropertyVertexPlace, after addFROMEdge, fromRelationship size:" + toRelationship.size());
    }

    public Map<Object, Object[]> getTONeighbors() {
        return this.toRelationship;
    }
    
    public Set<String> getTONeighborTypes(Object neighborId) {
        return (Set<String>) this.toRelationship.get(neighborId)[0];
    }

    public Map<String,String> getTONeighborProperties(Object neighborId) {
        return (Map<String,String>) this.toRelationship.get(neighborId)[1];
    }

    public void setFROMNeighborRelation(Object neighborID, List<String> relationTypes,  Map<String, String> relationProperties) {
        if (!this.fromRelationship.containsKey(neighborID)) {
            Set<String> types = new HashSet<String>();
            for(String rs: relationTypes) {
                types.add(rs.trim().toLowerCase());
            }
            Map<String, String> mapProperties = new HashMap<>();
            for(Map.Entry<String, String> entry: relationProperties.entrySet()) {
                mapProperties.put(entry.getKey().trim().toLowerCase(), entry.getValue().trim().toLowerCase());
            }

            Object[] relation = new Object[2];
            relation[0] = (Object) types;
            relation[1] = (Object) mapProperties;

            fromRelationship.put(neighborID, relation);
        } else {
            Object[] relation = this.fromRelationship.get(neighborID);
            Set<String> types = (Set<String>) relation[0];
            Map<String, String> mapProperties = (Map<String, String>) relation[1];
            for(String s: relationTypes) {
                types.add(s.trim().toLowerCase());
            }
            for(Map.Entry<String, String> entry: relationProperties.entrySet()) {
                mapProperties.put(entry.getKey().trim().toLowerCase(), entry.getValue().trim().toLowerCase());
            }
            relation[0] = (Object) types;
            relation[1] = (Object) mapProperties;
            fromRelationship.put(neighborID, relation);
        }
        MASS.getLogger().error("At PropertyVertexPlace, after addFROMEdge, fromRelationship size:" + fromRelationship.size());
    }
    
    public Map<Object, Object[]> getFROMNeighbors() {
        return this.fromRelationship;
    }

    public Set<String> getFROMNeighborTypes(Object neighborId) {
        return (Set<String>) this.fromRelationship.get(neighborId)[0];
    }

    public Map<String,String> getfromNeighborProperties(Object neighborId) {
        return (Map<String,String>) this.toRelationship.get(neighborId)[1];
    }

    /**
	 * Is called from PropertyGraphPlaces.callAll( ) and 
     * invoke the function specified with functionId as
	 * passing arguments to this function. 
	 * @param functionId The ID number of the function to invoke
	 * @param argument An argument that will be passed to the invoked function
	 * @return if this is right Vertex, return ItemID; otherwise, return empty string
	 */
    @Override
	public Object callMethod( int functionId, Object argument ) {
		Object result = new String();
        
        MASS.getLogger().debug("functionID: " + functionId + ", argument: " + argument);
        MASS.getLogger().debug("At vertex with ItemID: " + this.ItemID + ", labels:" + this.labels + ", properties: " + this.nodeProperties);
        switch (functionId) {
            case 1: // Match Node Labels & properties
                if(argument == null) return result;
                result = this.hasLabelsProperties((List<Object>) argument) ? ((Object) new String(this.ItemID)) : result;
                // MASS.getLogger().debug("At PropertyVertexPlace callMethod, function 01 result: " + result);
                break;
            default:
                break;
        }

        return result;
	}
    
}
