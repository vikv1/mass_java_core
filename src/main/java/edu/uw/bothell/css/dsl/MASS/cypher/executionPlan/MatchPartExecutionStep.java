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

public abstract class MatchPartExecutionStep
    extends ExecutionStepWithChildren
    implements ExecutionStepWithResultName {
    protected final String resultName;
    protected final boolean optional;
    protected final List<String> propertyResultNames;
    protected final String originalName;

    public MatchPartExecutionStep(
        String originalName,
        String resultName,
        boolean optional,
        List<ExecutionStepWithResultName> properties
    ) {
        super(properties.toArray(new ExecutionStepWithResultName[0]));
        this.originalName = originalName == null ? null: originalName.trim();
        this.resultName = resultName == null ? null: resultName.trim();
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

    @Override
    public String toString() {
        return String.format(
            super.toString(),
            String.join(", ", propertyResultNames),
            getResultName()
        );
    }
}
