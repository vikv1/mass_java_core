package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

import java.util.stream.Stream;

public class CypherDeleteClause extends CypherClause {
    private final CypherListLiteral<CypherAstBase> expressions;
    private final boolean detach;

    public CypherDeleteClause(CypherListLiteral<CypherAstBase> expressions, boolean detach) {
        this.expressions = expressions;
        this.detach = detach;
    }

    public CypherListLiteral<CypherAstBase> getExpressions() {
        return expressions;
    }

    public boolean isDetach() {
        return detach;
    }

    @Override
    public String toString() {
        return "===>>>CypherDeleteClause: \\n" + //
                "" + (isDetach() ? "DETACH field true" : "") + "DELETE query content:" + CypherExpression.toString(getExpressions()) + ";";
    }

    @Override
    public Stream<? extends CypherAstBase> getChildren() {
        return expressions.stream();
    }
}
