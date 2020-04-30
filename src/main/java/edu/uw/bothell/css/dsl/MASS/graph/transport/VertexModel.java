package edu.uw.bothell.css.dsl.MASS.graph.transport;

import java.io.Serializable;
import java.util.List;

public class VertexModel implements Serializable {
    public final Object id;
    public final List<Object> neighbors;

    public VertexModel(Object id, List<Object> neighbors) {
        this.id = id;
        this.neighbors = neighbors;
    }
}
