package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

import com.google.common.collect.Lists;

import java.util.stream.Collectors;

import static edu.uw.bothell.css.dsl.MASS.cypher.utils.StreamUtils.stream;

public abstract class CypherExpression extends CypherAstBase {
    public static String toString(Iterable<CypherAstBase> expressions) {
        return stream(expressions)
            .map(e -> e == null ? "null" : e)
            .map(Object::toString)
            .collect(Collectors.joining(", "));
    }

    public static Object toString(CypherAstBase[] expressions) {
        return toString(Lists.newArrayList(expressions));
    }
}
