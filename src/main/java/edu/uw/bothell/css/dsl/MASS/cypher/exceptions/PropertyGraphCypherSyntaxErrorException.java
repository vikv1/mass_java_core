package edu.uw.bothell.css.dsl.MASS.cypher.exceptions;

public class PropertyGraphCypherSyntaxErrorException extends PropertyGraphCypherException {
    private static final long serialVersionUID = 3262990761198793941L;

    public PropertyGraphCypherSyntaxErrorException(String message) {
        super(message);
    }

    public PropertyGraphCypherSyntaxErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
