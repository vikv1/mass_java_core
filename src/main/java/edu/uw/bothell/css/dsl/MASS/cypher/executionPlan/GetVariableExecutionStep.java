package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

public class GetVariableExecutionStep extends DefaultExecutionStep implements ExecutionStepWithResultName {
    private final String resultName;
    private final String name;

    public GetVariableExecutionStep(String resultName, String name) {
        this.resultName = resultName;
        this.name = name;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        return new PropertyGraphCypherResult(
            source.peek(row -> {
                row.pushScope(resultName, row.get(name));
            }),
            source.getColumnNames()
        );
    }

    @Override
    public String toString() {
        return String.format("In %s: {name=%s, resultName=%s}", super.toString(), name, resultName);
    }

    @Override
    public String getResultName() {
        return resultName;
    }
}
