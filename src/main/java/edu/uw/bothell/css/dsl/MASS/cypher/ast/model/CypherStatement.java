package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

import java.util.stream.Stream;

public class CypherStatement extends CypherAstBase {
    private final CypherAstBase query;

    public CypherStatement(CypherAstBase query) {
        this.query = query;
    }

    public CypherAstBase getQuery() {
        return query;
    }

    @Override
    public Stream<? extends CypherAstBase> getChildren() {
        return Stream.of(query);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("=>CypherStatement: \n");
        sb.append(this.getQuery().toString());
        return sb.toString();
    }
}