package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

public abstract class FunctionInvocationExecutionStep extends FunctionInvocationExecutionStepBase {
    public FunctionInvocationExecutionStep(
        String functionName,
        String resultName,
        ExecutionStepWithResultName[] argumentsExecutionStep
    ) {
        super(functionName, resultName, argumentsExecutionStep);
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        source = super.execute(ctx, source);
        return source.peek(row -> executeOnRow(ctx, row));
    }

    protected void executeOnRow(PropertyGraphCypherQueryContext ctx, CypherResultRow row) {
        Object result = executeFunction(ctx, getArguments(row));
        row.pushScope(getResultName(), result);
    }

    protected abstract Object executeFunction(PropertyGraphCypherQueryContext ctx, Object[] arguments);
}
