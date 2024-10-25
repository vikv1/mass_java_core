package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

public enum Direction {
    OUT,
    IN,
    BOTH;

    public Direction reverse() {
        switch (this) {
            case OUT:
                return IN;
            case IN:
                return OUT;
            case BOTH:
                return BOTH;
            default:
                throw new RuntimeException("unexpected direction: " + this);
        }
    }
}