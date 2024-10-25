package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

import java.util.stream.Stream;

public class CypherReturnClause extends CypherClause {
    private final boolean distinct;
    private final CypherReturnBody returnBody;

    public CypherReturnClause(boolean distinct, CypherReturnBody returnBody) {
        this.distinct = distinct;
        this.returnBody = returnBody;
    }

    public boolean isDistinct() {
        return distinct;
    }

    public CypherReturnBody getReturnBody() {
        return returnBody;
    }

    @Override
    public String toString() {
        return String.format(
            "===>>>CypherReturnClause: \n RETURN part content: %s%s",
            isDistinct() ? "DISTINCT field true" : "",
            getReturnBody().toString()
        );
    }

    @Override
    public Stream<? extends CypherAstBase> getChildren() {
        return Stream.of(returnBody);
    }
}
