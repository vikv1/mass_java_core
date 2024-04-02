package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherLiteral;

public class LiteralExecutionStep extends DefaultExecutionStep implements ExecutionStepWithResultName {
    private final String resultName;
    private final Object value;

    public LiteralExecutionStep(String resultName, Object value) {
        this.resultName = resultName;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        if (source == null) {
            source = new SingleRowPropertyGraphCypherResult();
        }
        return source.peek(row -> row.pushScope(resultName, CypherLiteral.toJava(value)));
    }

    @Override
    public String toString() {
        return String.format("%s: {'%s', %s}", super.toString(), resultName, value);
    }

    @Override
    public String getResultName() {
        return resultName;
    }
}
