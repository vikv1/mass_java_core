package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.*;
import edu.uw.bothell.css.dsl.MASS.cypher.utils.StringUtils;

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
        StringBuilder result = new StringBuilder();
        result.append(String.format("In %s:", super.toString()));
        getChildSteps().forEach(child -> {
            String childString = StringUtils.indent(2, child.toString());
            result.append('\n').append(childString);
        });
        return result.toString();
    }
}
