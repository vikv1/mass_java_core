package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

public class CypherAllLiteral extends CypherLiteral<String> {
    public CypherAllLiteral() {
        super("*");
    }

    @Override
    public String toString() {
        return "*";
    }
}
