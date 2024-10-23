package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

import com.google.common.collect.ImmutableList;

import java.util.stream.Stream;

public class CypherQuery extends CypherAstBase {
    private final ImmutableList<CypherClause> clauses;

    public CypherQuery(ImmutableList<CypherClause> clauses) {
        this.clauses = clauses;
    }

    public ImmutableList<CypherClause> getClauses() {
        return clauses;
    }

    @Override
    public Stream<? extends CypherAstBase> getChildren() {
        return clauses.stream();
    }

    @Override
    public String toString() {
        StringBuilder results = new StringBuilder();
        results.append("==>>CypherQueries: \n");
        long i = 1;
        for (CypherClause clause : getClauses()) {
            results.append("CypherClause " + i + ": ");
            i++;
            results.append(clause.toString());
            results.append("\n");
        }
        return results.toString();
    }
}
