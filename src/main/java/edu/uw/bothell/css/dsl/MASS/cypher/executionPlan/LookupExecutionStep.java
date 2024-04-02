package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import com.google.common.collect.Sets;
import edu.uw.bothell.css.dsl.MASS.cypher.CypherDuration;
import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherLabelName;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherLiteral;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherVariable;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherTypeErrorException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.*;

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
