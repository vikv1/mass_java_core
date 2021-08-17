package edu.uw.bothell.css.dsl.MASS.graph.matsim;

import java.io.Serializable;

import edu.uw.bothell.css.dsl.MASS.graph.MATSimNetworkLink;

public class MatsimEdge implements Serializable {
    // serialVersionUID is the object serialization
    // version UID.
    public static final long serialVersionUID = 0L;

    // length is interpreted as the weight of the edge.
    public double length;

    public String id;
    public long from;
    public long to;
    public double freespeed;
    public float capacity;
    public int permlanes;
    public int oneway;
    public String modes;

    public MatsimEdge(double weight) {
        this.length = weight;
    }

    public MatsimEdge(MATSimNetworkLink link) {
        this.id = link.id;
        this.from = link.from;
        this.to = link.to;
        this.length = link.length;
        this.freespeed = link.freespeed;
        this.capacity = link.capacity;
        this.permlanes = (int)link.permlanes;
        this.oneway = link.oneway;
        this.modes = link.modes;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"id\": \"" + this.id + "\", ");
        sb.append("\"from\": \"" + this.from + "\", ");
        sb.append("\"to\": \"" + this.to + "\", ");
        sb.append("\"length\": \"" + this.length + "\", ");
        sb.append("\"freespeed\": \"" + this.freespeed + "\", ");
        sb.append("\"capacity\": \"" + this.capacity + "\", ");
        sb.append("\"permlanes\": \"" + this.permlanes + "\", ");
        sb.append("\"oneway\": \"" + this.oneway + "\", ");
        sb.append("\"modes\": \"" + this.modes + "\" }");

        return sb.toString();
    }
}