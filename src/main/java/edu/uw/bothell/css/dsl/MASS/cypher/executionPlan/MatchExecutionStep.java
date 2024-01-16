package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

public class MatchExecutionStep extends ExecutionStepWithChildren {
    public MatchExecutionStep(PatternPartExecutionStep[] childSteps, WhereExecutionStep whereStep) {
        super(toChildSteps(childSteps, whereStep));
    }

    private static ExecutionStep[] toChildSteps(PatternPartExecutionStep[] childSteps, WhereExecutionStep whereStep) {
        int length = childSteps.length + (whereStep == null ? 0 : 1);
        ExecutionStep[] results = new ExecutionStep[length];
        System.arraycopy(childSteps, 0, results, 0, childSteps.length);
        if (whereStep != null) {
            results[results.length - 1] = whereStep;
        }
        System.out.println("Total number of match execution steps:" + results.length);
        return results; 
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        return super.execute(ctx, source);
    }

    @Override
    public String toString() {
        return String.format("In %s: ", super.toString());
    }
}
