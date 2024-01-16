package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

public class WhereExecutionStep extends ExecutionStepWithChildren {
    private final String whereStepResultName;

    public WhereExecutionStep(ExecutionStepWithResultName whereStep) {
        super(whereStep);
        this.whereStepResultName = whereStep.getResultName();
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        source = super.execute(ctx, source);

        return source.filter(row -> {
            Object value = row.get(whereStepResultName);
            row.popScope();
            if (value == null) {
                return false;
            } else if (value instanceof Boolean) {
                return (Boolean) value;
            } else {
                return value != null;
            }
        });
    }

    @Override
    public String toString() {
        return String.format("%s {whereStepResultName:'%s'}", super.toString(), whereStepResultName);
    }
}
