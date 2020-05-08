package edu.uw.bothell.css.dsl.MASS.graph;

import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;

public interface Graph {
    GraphModel getGraph();
    GraphModel getGraph(boolean all);

    // Graph Maintenance
    boolean addEdge(Object vertexId, Object neighborId, double weight);
    boolean removeEdge(Object vertexId, Object neighborId);

    int addVertex(Object vertexId);
    int addVertex(Object vertexId, Object vertexInitParam);
    boolean removeVertex(Object vertexId);

    void setGraph(GraphModel newGraph);
}
