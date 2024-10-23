package edu.uw.bothell.css.dsl.MASS.cypher.exceptions;

public class PropertyGraphCypherException extends RuntimeException {
    private static final long serialVersionUID = 6952596657973758922L;

    public PropertyGraphCypherException(Exception e) {
        super(e);
    }

    public PropertyGraphCypherException(String msg, Throwable e) {
        super(msg, e);
    }

    public PropertyGraphCypherException(String msg) {
        super(msg);
    }
}
