package edu.uw.bothell.css.dsl.MASS.graph;

import edu.uw.bothell.css.dsl.MASS.graph.transport.GraphModel;

public class GraphMaintenance {
    public static GraphModel getPlaces(Graph graph) {
        return graph.getGraph(false);
    }
}
