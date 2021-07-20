package edu.uw.bothell.css.dsl.MASS.graph.hippie;

import java.io.Serializable;
import java.util.HashMap;

import edu.uw.bothell.css.dsl.MASS.VertexPlace;

// HippieVertex is a HIPPIE vertex. It has a HIPPIE Key
// and ID, and maintains all outgoing edge weight and
// attributes.
public class HippieVertex extends VertexPlace {
    // serialVersionUID is the object serialization 
    // version UID.
    public static final long serialVersionUID = 0L;

    // Key is the HIPPIE vertex key.
    public String Key = "";

    // ID is the HIPPIE vertex ID.
    public String ID = "";

    // edgeAttributes is a map of outgoing vertex neighbors.
    public HashMap<Object, HippieEdge> edgeAttributes = new HashMap<Object, HippieEdge>();

    // VertexInitData is initialization data that's passed to 
    // a newly constructed HIPPIE vertex.
    public static class VertexInitData implements Serializable {
        public String Key = "";
        public String ID = "";

        public VertexInitData(String Key, String ID) {
            this.Key = Key;
            this.ID = ID;
        }

        @Override
        public String toString() {
            return "Key: " + this.Key + ", ID: " + this.ID;
        }
    }

    // HippieVertex instantiates a new HippieVertex with the provided arguments.
    // 'args' is expected to be an instance of 'VertexInitData'.
    public HippieVertex(Object args) {
        super();
        if (args == null) { return; }

        VertexInitData initData = (VertexInitData)args;
        this.Key = initData.Key;
        this.ID = initData.ID;
    }

    // clone performs a deep clone of the HIPPIE vertex and is
    // necessary for retrieving verticies from remote nodes.
    @Override
    public Object clone() throws CloneNotSupportedException {
        HippieVertex vertexClone = (HippieVertex)super.clone();

        vertexClone.Key = this.Key;
        vertexClone.ID = this.ID;
        vertexClone.edgeAttributes = new HashMap<Object, HippieEdge>();
        
        for (Object key : this.edgeAttributes.keySet()) {
            HippieEdge edge = this.edgeAttributes.get(key);
            HippieEdge newEdge = new HippieEdge(edge.weight);

            for (String attribKey : edge.attributes.keySet()) {
                String[] vals = edge.attributes.get(attribKey);
                String[] newVals = new String[vals.length];
                for (int i = 0; i < vals.length; i++) {
                    newVals[i] = vals[i];
                }

                newEdge.addAttribute(attribKey, newVals);
            }

            vertexClone.edgeAttributes.put(key, newEdge);
        }

        return vertexClone;
    }

    // addNeighbor overrides the parent addNeighbor class to add
    // HippieEdge objects for each neighbor.
    @Override
    public void addNeighbor(Object neighborId, double weight) {
        super.addNeighbor(neighborId, weight);

        HippieEdge edge = new HippieEdge(weight);
        this.edgeAttributes.put(neighborId, edge);
    }

    // addEdgeAttributes adds HIPPIE attributes to the provided 
    // neighbor.
    public void addEdgeAttributes(Object neighborId, String attributes) {
        if (attributes == null || attributes.equalsIgnoreCase("")) {
            return;
        }

        // Make sure the neighbor is an existing vertex.
        HippieEdge edge = this.edgeAttributes.getOrDefault(neighborId, null);
        if (edge == null) { return; }

        // Parse attributes
        String[] attribs = attributes.split(";");
        for (String attrib : attribs) {
            String[] keyVals = attrib.split(":");
            String[] vals = keyVals[1].split(",");
            edge.addAttribute(keyVals[0], vals);
        }

        this.edgeAttributes.put(neighborId, edge);
    }

    // addEdge adds an edge to the provided neighbor with the provided
    // weight and attributes.
    public void addEdge(Object neighborId, double weight, String attributes) {
        this.addNeighbor(neighborId, weight);
        HippieEdge edge = new HippieEdge(weight);

        // if there are no attributes, add the edge without them.
        if (attributes.length() == 0) { 
            this.edgeAttributes.put(neighborId, edge);
            return; 
        }

        // Parse attributes
        String[] attribs = attributes.split(";");
        for (String attrib : attribs) {
            String[] keyVals = attrib.split(":");
            String[] vals = keyVals[1].split(",");
            edge.addAttribute(keyVals[0], vals);
        }

        this.edgeAttributes.put(neighborId, edge);
    }
    
    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public String toString() {
        return this.Key + "@@" + this.ID;
    }

    @Override
    public boolean equals(Object anObject) {
        HippieVertex obj = (HippieVertex)anObject;
        return obj.hashCode() == this.hashCode();
    }
}