package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.utils.StringUtils;

public class SeriesExecutionStep extends ExecutionStepWithChildren {
    public SeriesExecutionStep(ExecutionStep... children) {
        super(children);
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        return super.execute(ctx, source);
    }

    @Override
    public String toStringFull() {
        StringBuilder result = new StringBuilder();
        getChildSteps().forEach(child -> {
            String childString = StringUtils.indent(2, child.toString());
            result.append('\n').append(childString);
        });
        return result.toString();
    }
}
