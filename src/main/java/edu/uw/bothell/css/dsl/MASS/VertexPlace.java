package edu.uw.bothell.css.dsl.MASS;

import java.io.Serializable;

public class VertexPlace extends Place implements Serializable {


    public VertexPlace() {
        super();

        System.err.println("VertexPlace constructed");

        MASSBase.getLogger().debug("VertexPlace constructed.");
    }

    public VertexPlace(Object args) {
        super();

        System.err.println("VertexPlace constructed with args");

        MASSBase.getLogger().debug("VertexPlace constructed with args.");
    }
}
