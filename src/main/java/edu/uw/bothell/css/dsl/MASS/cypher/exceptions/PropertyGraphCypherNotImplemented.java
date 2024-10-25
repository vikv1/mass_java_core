package edu.uw.bothell.css.dsl.MASS.cypher.exceptions;

public class PropertyGraphCypherNotImplemented extends PropertyGraphCypherException {
    private static final long serialVersionUID = -7342371148840304840L;

    public PropertyGraphCypherNotImplemented(String message) {
        super("not implemented: " + message);
    }
}
