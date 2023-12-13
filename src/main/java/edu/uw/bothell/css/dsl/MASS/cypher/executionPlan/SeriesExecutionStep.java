package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

public class SeriesExecutionStep extends ExecutionStepWithChildren {
    public SeriesExecutionStep(ExecutionStep... children) {
        super(children);
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        return super.execute(ctx, source);
    }
}
