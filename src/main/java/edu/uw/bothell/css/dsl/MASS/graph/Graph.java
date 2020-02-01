package edu.uw.bothell.css.dsl.MASS.graph;

import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;

public interface Graph {
    GraphModel getGraph();

    // Graph Maintenance
    boolean addEdge(int vertexId, int neighborId, double weight);
    boolean removeEdge(int vertexId, int neighborId);

    int addVertex();
    boolean removeVertex(int vertexId);
}
