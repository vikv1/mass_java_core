package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;
import static edu.uw.bothell.css.dsl.MASS.cypher.utils.StreamUtils.distinctBy;
import edu.uw.bothell.css.dsl.MASS.cypher.utils.PredicateWithIndex;
import edu.uw.bothell.css.dsl.MASS.cypher.utils.ObjectUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class ReturnExecutionStep extends ExecutionStepWithChildren {
    private final boolean distinct;
    private final String skipExpressionResultName;
    private final String limitExpressionResultName;

    public ReturnExecutionStep(
        boolean distinct,
        List<ExecutionStep> returnItems,
        ExecutionStepWithResultName skipExpression,
        ExecutionStepWithResultName limitExpression
    ) {
        super(toChildren(returnItems, skipExpression, limitExpression));
        this.distinct = distinct;
        this.skipExpressionResultName = skipExpression == null ? null : skipExpression.getResultName();
        this.limitExpressionResultName = limitExpression == null ? null : limitExpression.getResultName();
    }

    private static ExecutionStep[] toChildren(
        List<ExecutionStep> returnItems,
        ExecutionStepWithResultName skipExpression,
        ExecutionStepWithResultName limitExpression
    ) {
        return Stream.concat(
                returnItems.stream(),
                Stream.of(skipExpression, limitExpression)
                )
                .filter(Objects::nonNull)
                .toArray(ExecutionStep[]::new);
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        if (source == null) {
            source = new SingleRowPropertyGraphCypherResult();
        }

        source.getColumnNames().clear();
        PropertyGraphCypherResult result = super.execute(ctx, source);
        if (distinct) {
            result = result.filter(distinctBy(row -> {
                HashMap<String, Object> columnValues = new HashMap<>();
                for (String columnName : row.getColumnNames()) {
                    columnValues.put(columnName, row.get(columnName));
                }
                return columnValues;
            }));
        }
        result = applySkip(result);
        result = applyLimit(result);
        return result;
    }

    private PropertyGraphCypherResult applyLimit(PropertyGraphCypherResult result) {
        if (limitExpressionResultName != null) {
            result = result.filter(new PredicateWithIndex<CypherResultRow>() {
                @Override
                protected boolean test(CypherResultRow row, long index) {
                    Number value = (Number) row.get(limitExpressionResultName);
                    long limit = value.longValue();
                    return index < limit;
                }
            });
        }
        return result;
    }

    private PropertyGraphCypherResult applySkip(PropertyGraphCypherResult result) {
        if (skipExpressionResultName != null) {
            result = result.filter(new PredicateWithIndex<CypherResultRow>() {
                @Override
                protected boolean test(CypherResultRow row, long index) {
                    Number value = (Number) row.get(skipExpressionResultName);
                    long skip = value.longValue();
                    return index >= skip;
                }
            });
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s: %s}", super.toString(), distinct);
    }
}

