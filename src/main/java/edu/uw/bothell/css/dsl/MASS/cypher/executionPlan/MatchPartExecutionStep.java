package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static edu.uw.bothell.css.dsl.MASS.cypher.utils.StreamUtils.stream;

public abstract class MatchPartExecutionStep<TC extends MatchPartExecutionStep>
    extends ExecutionStepWithChildren
    implements ExecutionStepWithResultName {
    protected final String resultName;
    protected final boolean optional;
    protected final List<String> propertyResultNames;
    protected final List<TC> connectedSteps = new ArrayList<>();
    protected final String originalName;

    public MatchPartExecutionStep(
        String originalName,
        String resultName,
        boolean optional,
        List<ExecutionStepWithResultName> properties
    ) {
        super(properties.toArray(new ExecutionStepWithResultName[0]));
        this.originalName = originalName;
        this.resultName = resultName;
        this.optional = optional;
        this.propertyResultNames = properties.stream().map(ExecutionStepWithResultName::getResultName).collect(Collectors.toList());
    }

    public String getOriginalName() {
        return originalName;
    }

    @Override
    public String getResultName() {
        return resultName;
    }

    protected List<TC> getConnectedSteps() {
        return connectedSteps;
    }

    protected boolean isOptional() {
        return optional;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult originalSource) {
        PropertyGraphCypherResult source = originalSource == null ? new SingleRowPropertyGraphCypherResult() : originalSource;
        source = super.execute(ctx, source);

        // if (originalSource == null || getConnectedSteps().size() == 0) {
            return new PropertyGraphCypherResult(
                source.flatMap(row -> executeInitialQuery(ctx, row)),
                source.getColumnNames()
            );
        // }

        // source = super.execute(ctx, source);
        // return executeConnectedQuery(ctx, source);
    }

    protected abstract Stream<CypherResultRow> executeInitialQuery(PropertyGraphCypherQueryContext ctx, CypherResultRow row);

    public PropertyGraphCypherResult executeConnectedQuery(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        return source.flatMapCypherResult(row -> executeConnectedGetElements(ctx, row));
    }

    protected abstract Stream<? extends CypherResultRow> executeConnectedGetElements(PropertyGraphCypherQueryContext ctx, CypherResultRow row);

    public void addConnectedStep(TC connectedStep) {
        connectedSteps.add(connectedStep);
    }


    @Override
    public String toString() {
        return String.format(
            "In %s: {propertyResultNames=[%s], resultName=%s, optional=%s, connectedSteps=[%s]}",
            super.toString(),
            String.join(", ", propertyResultNames),
            getResultName(),
            isOptional(),
            getConnectedSteps().stream()
                .map(MatchPartExecutionStep::getResultName)
                .collect(Collectors.joining(", "))
        );
    }
}
