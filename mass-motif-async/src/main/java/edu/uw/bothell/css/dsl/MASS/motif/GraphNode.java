package edu.uw.bothell.css.dsl.MASS.motif;

// GraphNode.java
//
// by Matt Kipps
// 12/12/14

import java.util.HashMap;
import java.util.Map;

import edu.uw.bothell.css.dsl.MASS.*;


public class GraphNode extends Place {

    public static final int collectSubgraphs_ = 0;
    public static final int initializeEdges_ = 1;
    public Object callMethod(int functionID, Object args) {
        switch(functionID) {
            case initializeEdges_:
                AdjacencyList edges = (AdjacencyList)args;
                initializeEdges(edges);
                break;
            case collectSubgraphs_:
                return (Object)collectSubgraphs();
            default:
                break;
        }
        return null;
    }

    private int networkSize;
    private AdjacencyList adjacencyList;
    private Map<String, Integer> subgraphs;

    public GraphNode(Object rawArgs) {
        super();

        Constructor constructor = (Constructor)rawArgs;
        this.networkSize   = constructor.getNetworkSize();
        this.subgraphs     = new HashMap<String, Integer>();
        this.adjacencyList = null;
    }

    public int getNetworkSize() {
        return networkSize;
    }

    public Map<String, Integer> collectSubgraphs() {
        return subgraphs;
    }

    // set all the network edges for this node
    public void initializeEdges(AdjacencyList adjacencyList) {
        this.adjacencyList = adjacencyList;
    }

    public void addToSubgraphs(Subgraph subgraph) {
        String repr = subgraph.getByteString();
        int count = 1;
        synchronized(subgraphs) {
            if (subgraphs.containsKey(repr)) {
                count += subgraphs.get(repr);
            }
            subgraphs.put(repr, count);
        }
    }

    public AdjacencyList getAdjacencyList() {
        return adjacencyList;
    }

    public int getFirstIndex() {
        return getIndex()[0];
    }

    // the nested Constructor class simplifies the instantiation of GraphNode
    // objects through MASS library calls
    public static class Constructor implements java.io.Serializable {
        private int networkSize;

        public Constructor(int networkSize) {
            this.networkSize = networkSize;
        }

        public int getNetworkSize() {
            return networkSize;
        }
    }
}
