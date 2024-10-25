package edu.uw.bothell.css.dsl.MASS.cypher;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.stream.Stream;

public class SingleRowPropertyGraphCypherResult extends PropertyGraphCypherResult {
    public SingleRowPropertyGraphCypherResult() {
        this(new DefaultCypherResultRow(new LinkedHashSet<>(), new HashMap<>()));
    }

    public SingleRowPropertyGraphCypherResult(CypherResultRow row) {
        super(Stream.of(row), row.getColumnNames());
    }
}
