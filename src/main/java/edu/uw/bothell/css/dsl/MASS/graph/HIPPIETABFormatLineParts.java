package edu.uw.bothell.css.dsl.MASS.graph;

public enum HIPPIETABFormatLineParts {
    PROTEIN_KEY,
    PROTEIN_ID,
    INTERACTION_KEY,
    INTERACTION_ID,
    INTERACTION_ATTRIBUTE,
    EXTENDED_ATTRIBUTES,
    ;

    public static String getPart(String [] lineParts, HIPPIETABFormatLineParts part) {
        return lineParts.length < part.ordinal() ? "" : lineParts[part.ordinal()];
    }
}
