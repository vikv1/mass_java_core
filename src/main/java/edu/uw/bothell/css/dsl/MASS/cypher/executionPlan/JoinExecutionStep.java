package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.*;

import java.util.LinkedHashSet;
import java.util.stream.Stream;

public class JoinExecutionStep extends ExecutionStepWithChildren {
    private final boolean executeOnceOnEmptySource;

    public JoinExecutionStep(boolean executeOnceOnEmptySource, ExecutionStep... childSteps) {
        super(childSteps);
        this.executeOnceOnEmptySource = executeOnceOnEmptySource;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        if (source == null) {
            if (executeOnceOnEmptySource) {
                source = new SingleRowPropertyGraphCypherResult();
            } else {
                return new EmptyPropertyGraphCypherResult();
            }
        }

        LinkedHashSet<String> columnNames = source.getColumnNames();

        Stream<CypherResultRow> rows = source.flatMap(row -> super.execute(ctx, new SingleRowPropertyGraphCypherResult(row)));

        return new PropertyGraphCypherResult(rows, columnNames);
    }

    @Override
    public String toString() {
        return String.format("%s {executeOnceOnEmptySource=%s}", super.toString(), executeOnceOnEmptySource);
    }
}
