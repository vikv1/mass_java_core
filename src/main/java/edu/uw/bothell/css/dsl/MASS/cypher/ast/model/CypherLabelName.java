package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

public class CypherLabelName extends CypherLiteral<String> {
    public CypherLabelName(String value) {
        super(value);
    }

    @Override
    public String toString() {
        return ":" + getValue();
    }
}