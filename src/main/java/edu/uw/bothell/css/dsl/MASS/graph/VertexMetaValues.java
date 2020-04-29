package edu.uw.bothell.css.dsl.MASS.graph;

import java.io.Serializable;

public class VertexMetaValues implements Serializable {
    public int Id;
    public int OwnerPid;

    public VertexMetaValues(int id, int ownerPid) {
        Id = id;
        OwnerPid = ownerPid;
    }
}
