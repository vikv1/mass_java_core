package edu.uw.bothell.css.dsl.MASS.graph.matsim;

import java.io.Serializable;
import java.util.HashMap;

import edu.uw.bothell.css.dsl.MASS.VertexPlace;

public class MatsimVertex extends VertexPlace {
    // serialVersionUID is the object serialization 
    // version UID.
    public static final long serialVersionUID = 0L;

    public Long ID = -1L;
    public Double x = 0.0;
    public Double y = 0.0;
    public Integer type = -1;

    // edgeAttributes is a map of outgoing vertex neighbors
    public HashMap<Object, MatsimEdge> edgeAttributes = new HashMap<Object, MatsimEdge>();

    public static class MatsimVertexInitData implements Serializable {
        public Long ID = -1L;
        public Double x = 0.0;
        public Double y = 0.0;
        public Integer type = -1;

        public MatsimVertexInitData(Long ID, Double x, Double y, Integer type) {
            this.ID = ID;
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    public MatsimVertex(Object args) {
        super();
        if (args == null) { return; }

        MatsimVertexInitData initData = (MatsimVertexInitData)args;
        this.ID = initData.ID;
        this.x = initData.x;
        this.y = initData.y;
        this.type = initData.type;
    }

    // addNeighbor overrides the parent addNeighbor class to add
    // HippieEdge objects for each neighbor.
    @Override
    public void addNeighbor(Object neighborId, double weight) {
        super.addNeighbor(neighborId, weight);

        MatsimEdge edge = new MatsimEdge(weight);
        this.edgeAttributes.put(neighborId, edge);
    }

    public void addEdge(Object neighborId, MatsimEdge edge) {
        if (edge == null) { return; }

        super.addNeighbor(neighborId, edge.length);

        this.edgeAttributes.put(neighborId, edge);
    }

    // clone performs a deep clone of the Matsim vertex and is
    // necessary for retrieving verticies from remote nodes.
    @Override
    public Object clone() throws CloneNotSupportedException {
        MatsimVertex vertexClone = (MatsimVertex)super.clone();

        vertexClone.ID = this.ID;
        vertexClone.x = this.x;
        vertexClone.y = this.y;
        vertexClone.type = this.type;
        vertexClone.edgeAttributes = new HashMap<Object, MatsimEdge>();
        
        for (Object key : this.edgeAttributes.keySet()) {
            MatsimEdge edge = this.edgeAttributes.get(key);
            MatsimEdge newEdge = new MatsimEdge(edge.length);
            newEdge.id = edge.id;
            newEdge.from = edge.from;
            newEdge.to = edge.to;
            newEdge.freespeed = edge.freespeed;
            newEdge.capacity = edge.capacity;
            newEdge.permlanes = edge.permlanes;
            newEdge.oneway = edge.oneway;
            newEdge.modes = edge.modes;

            vertexClone.edgeAttributes.put(key, newEdge);
        }

        return vertexClone;
    }

    @Override
    public int hashCode() {
        return this.ID.toString().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"id\": \"" + this.ID + "\", ");
        sb.append("\"x\": \"" + this.x + "\", ");
        sb.append("\"y\": \"" + this.y + "\", ");
        sb.append("\"type\": \"" + this.type + "\" }");
        
        return sb.toString();
    }

    @Override
    public boolean equals(Object anObject) {
        MatsimVertex obj = (MatsimVertex)anObject;
        return obj.hashCode() == this.hashCode();
    }
}