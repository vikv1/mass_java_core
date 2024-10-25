package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

public class MatchExecutionStep extends ExecutionStepWithChildren {
    public MatchExecutionStep(MatchPatternPartExecutionStep[] childSteps) {
        super(toChildSteps(childSteps));
    }

    private static ExecutionStep[] toChildSteps(MatchPatternPartExecutionStep[] childSteps) {
        int length = childSteps.length;
        ExecutionStep[] results = new ExecutionStep[length];
        System.arraycopy(childSteps, 0, results, 0, childSteps.length);
        return results; 
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        return super.execute(ctx, source);
    }

    @Override
    public String toString() {
        return String.format("%s: ", super.toString());
    }
}
