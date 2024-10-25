package edu.uw.bothell.css.dsl.MASS.graph.hippie;

import java.io.Serializable;
import java.util.HashMap;

// HippieEdge represents a HIPPIE edge. It contains
// the edge weight and all the attribute data.
public class HippieEdge implements Serializable {
    // serialVersionUID is the object serialization
    // version UID.
    public static final long serialVersionUID = 0L;

    // weight is the edge weight.
    public double weight;

    // attributes is the edge's attributes.
    public HashMap<String, String[]> attributes;

    public HippieEdge(double weight) {
        this.weight = weight;
        this.attributes = new HashMap<String, String[]>();
    }

    // addAttribute adds the provided attributes to the
    // edge.
    public void addAttribute(String ID, String[] vals) {
        this.attributes.put(ID, vals);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("weight=");
        sb.append(this.weight);
        sb.append(", attributes={");

        for (String Key : this.attributes.keySet()) {
            sb.append("\"");
            sb.append(Key);
            sb.append("\": [");

            // Add first value
            sb.append("\"");
            sb.append(this.attributes.get(Key)[0]);
            sb.append("\"");
            for (int i = 1; i < this.attributes.get(Key).length; i++) {
                sb.append(", \"");
                sb.append(this.attributes.get(Key)[i]);
                sb.append("\"");
            }
            sb.append("], ");
        }

        sb.deleteCharAt(sb.length()-1);
        sb.deleteCharAt(sb.length()-1);
        sb.append("}");

        return sb.toString();
    }
}
