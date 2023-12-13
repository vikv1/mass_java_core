package edu.uw.bothell.css.dsl.MASS.cypher;

import java.util.LinkedHashSet;
import java.util.stream.Stream;

public class EmptyPropertyGraphCypherResult extends PropertyGraphCypherResult {
    public EmptyPropertyGraphCypherResult() {
        super(Stream.empty(), new LinkedHashSet<>());
    }

    public EmptyPropertyGraphCypherResult(LinkedHashSet<String> columnNames) {
        super(Stream.empty(), columnNames);
    }
}
