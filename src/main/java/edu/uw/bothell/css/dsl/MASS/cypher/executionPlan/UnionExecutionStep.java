package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnionExecutionStep extends ExecutionStepWithChildren {
    private final boolean all;

    public UnionExecutionStep(boolean all, ExecutionStep left, ExecutionStep right) {
        super(left, right);
        this.all = all;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        LinkedHashSet<String> columnNames = new LinkedHashSet<>();
        Stream<CypherResultRow> rows = Stream.of();
        for (ExecutionStep step : getChildSteps().collect(Collectors.toList())) {
            PropertyGraphCypherResult result = step.execute(ctx, null);
            columnNames.addAll(result.getColumnNames());
            rows = Stream.concat(rows, result);
        }
        if (!all) {
            rows = rows.distinct();
        }
        return new PropertyGraphCypherResult(rows, columnNames);
    }

    @Override
    public String toString() {
        return String.format("%s: {%s}", super.toString(), all);
    }
}
