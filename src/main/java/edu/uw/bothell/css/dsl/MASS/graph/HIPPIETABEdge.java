package edu.uw.bothell.css.dsl.MASS.graph;

import java.net.InetAddress;
import java.util.function.BiFunction;
import java.util.function.Function;

public class HIPPIETABEdge {
    private String proteinKey;
    private int proteinId;
    private String interactionKey;
    private int interactionId;
    private double interactionAttribute;

    private String extendedAttribute;

    public String getProteinKey() {
        return proteinKey;
    }

    public void setProteinKey(String proteinKey) {
        this.proteinKey = proteinKey;
    }

    public int getProteinId() {
        return proteinId;
    }

    public void setProteinId(int proteinId) {
        this.proteinId = proteinId;
    }

    public String getInteractionKey() {
        return interactionKey;
    }

    public void setInteractionKey(String interactionKey) {
        this.interactionKey = interactionKey;
    }

    public int getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(int interactionId) {
        this.interactionId = interactionId;
    }

    public double getInteractionAttribute() {
        return interactionAttribute;
    }

    public void setInteractionAttribute(double interactionAttribute) {
        this.interactionAttribute = interactionAttribute;
    }

    public String getExtendedAttribute() {
        return extendedAttribute;
    }

    public void setExtendedAttribute(String extendedAttribute) {
        this.extendedAttribute = extendedAttribute;
    }

    public static HIPPIETABEdge fromParts(String[] parts) {
        HIPPIETABEdge edge = new HIPPIETABEdge();

        Function<HIPPIETABFormatLineParts, String> getPart = (HIPPIETABFormatLineParts part) -> HIPPIETABFormatLineParts.getPart(parts, part);

        edge.proteinKey = getPart.apply(HIPPIETABFormatLineParts.PROTEIN_KEY);
        edge.proteinId = Integer.parseInt(getPart.apply(HIPPIETABFormatLineParts.PROTEIN_ID));

        edge.interactionKey = getPart.apply(HIPPIETABFormatLineParts.INTERACTION_KEY);
        edge.interactionId = Integer.parseInt(getPart.apply(HIPPIETABFormatLineParts.INTERACTION_ID));

        edge.interactionAttribute = Double.parseDouble(getPart.apply(HIPPIETABFormatLineParts.INTERACTION_ATTRIBUTE));

        edge.extendedAttribute = getPart.apply(HIPPIETABFormatLineParts.EXTENDED_ATTRIBUTES);

        return edge;
    }
}
