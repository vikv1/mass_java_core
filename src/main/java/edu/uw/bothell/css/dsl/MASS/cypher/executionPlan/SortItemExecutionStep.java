package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;


import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherSortItem;

import java.util.stream.Stream;

public class SortItemExecutionStep extends ExecutionStepWithChildren implements ExecutionStepWithResultName {
    private final CypherSortItem.Direction direction;
    private final String itemResultName;
    private final String expressionText;

    public SortItemExecutionStep(CypherSortItem.Direction direction, ExecutionStepWithResultName itemExpression, String expressionText) {
        super(itemExpression);
        this.direction = direction;
        this.itemResultName = itemExpression.getResultName();
        this.expressionText = expressionText;
    }

    public CypherSortItem.Direction getDirection() {
        return direction;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        return source.flatMapCypherResult(row -> {
            Object value = row.get(expressionText);
            if (value != null) {
                return Stream.of(row.pushScope(getResultName(), value));
            } else {
                return super.execute(ctx, new SingleRowPropertyGraphCypherResult(row));
            }
        });
    }

    @Override
    public String getResultName() {
        return itemResultName;
    }

    @Override
    public String toString() {
        return String.format(
            "In %s: {direction=%s, itemResultName='%s', expressionText='%s'}",
            super.toString(),
            direction,
            itemResultName,
            expressionText
        );
    }
}
