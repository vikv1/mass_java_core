package edu.uw.bothell.css.dsl.MASS.graph.transport;

import java.io.Serializable;
import java.util.List;

public class VertexModel implements Serializable {
    public final int id;
    public final List<Integer> neighbors;

    public VertexModel(int id, List<Integer> neighbors) {
        this.id = id;
        this.neighbors = neighbors;
    }
}
