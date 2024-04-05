package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherLabelName;

import java.util.List;

public class LookupExecutionStep extends ExecutionStepWithChildren implements ExecutionStepWithResultName {
    public static final String SCOPE_PROPERTY_SUFFIX = "_property";
    public static final String SCOPE_PROPERTY_NAME_SUFFIX = "_propertyName";
    public static final String SCOPE_ELEMENT_SUFFIX = "_element";
    private final String resultName;
    private final String property;
    private final List<CypherLabelName> labels;
    private final String atomStepResultName;

    public LookupExecutionStep(String resultName, ExecutionStepWithResultName atomStep, String property, List<CypherLabelName> labels) {
        super(atomStep);
        this.atomStepResultName = atomStep.getResultName();
        this.resultName = resultName;
        this.property = property;
        this.labels = labels;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        source = super.execute(ctx, source);
        // to do
        return new PropertyGraphCypherResult(null, source.getColumnNames());
    }

    @Override
    public String toString() {
        return String.format(
            "In %s: {resultName='%s', property='%s', labels=%s, atomStepResultName=%s}",
            super.toString(),
            resultName,
            property,
            labels,
            atomStepResultName
        );
    }

    @Override
    public String getResultName() {
        return resultName;
    }
}
