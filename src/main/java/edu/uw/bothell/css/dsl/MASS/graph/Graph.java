package edu.uw.bothell.css.dsl.MASS.graph;

import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;

public interface Graph {
    GraphModel getGraph();
    GraphModel getGraph(boolean all);

    // Graph Maintenance
    boolean addEdge(int vertexId, int neighborId, double weight);
    boolean removeEdge(int vertexId, int neighborId);

    int addVertex(int vertexId);
    boolean removeVertex(int vertexId);
}
