package edu.uw.bothell.css.dsl.MASS.graph.transport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GraphModel implements Serializable {
    private String name;
    private List<VertexModel> vertices = new ArrayList<VertexModel>();

    public GraphModel() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<VertexModel> getVertices() {
        return vertices;
    }

    public void addVertex(int id, List<Integer> neighbors) {
        VertexModel vertex = new VertexModel(id, neighbors);

        vertices.add(vertex);
    }
}
