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

        // return new PropertyGraphCypherResult(
        //     source.peek(row -> {
        //         Element element = null;
        //         Property property = null;
        //         String elementPropertyName = null;
        //         Object value = row.get(atomStepResultName);
        //         row.popScope();

        //         if (this.property != null) {
        //             String[] propertyParts = this.property.split("\\.");
        //             for (String propertyPart : propertyParts) {
        //                 String propertyName = ctx.normalizePropertyName(propertyPart);
        //                 if (value == null) {
        //                     value = null;
        //                 } else if (value instanceof Element) {
        //                     element = ((Element) value);
        //                     property = element.getProperty(propertyName);
        //                     elementPropertyName = propertyName;
        //                     if (property == null) {
        //                         value = null;
        //                     } else {
        //                         value = property.getValue();
        //                     }
        //                 } else if (value instanceof Map) {
        //                     value = ((Map) value).get(propertyName);
        //                     if (value instanceof CypherVariable) {
        //                         value = row.get(((CypherVariable) value).getName());
        //                     }
        //                 } else if (value instanceof CypherDuration) {
        //                     CypherDuration dur = (CypherDuration) value;
        //                     value = dur.getProperty(propertyName);
        //                 } else {
        //                     throw new PropertyGraphCypherTypeErrorException("Cannot access properties on a non-map/non-element value");
        //                 }
        //             }
        //         }

        //         if (labels.size() > 0) {
        //             Set<String> labelsSet = labels.stream()
        //                 .map(CypherLiteral::getValue)
        //                 .collect(Collectors.toSet());
        //             if (value instanceof Element) {
        //                 Set<String> elementLabels;
        //                 if (value instanceof Vertex) {
        //                     elementLabels = ctx.getVertexLabels((Vertex) value);
        //                 } else if (value instanceof Edge) {
        //                     throw new PropertyGraphCypherNotImplemented("lookup with labels on edge: " + this);
        //                 } else {
        //                     throw new PropertyGraphCypherNotImplemented("unhandled element type: " + value.getClass().getName());
        //                 }

        //                 if (Sets.difference(labelsSet, elementLabels).size() > 0) {
        //                     row.pushScope(resultName, null);
        //                     return;
        //                 }
        //             } else {
        //                 throw new PropertyGraphCypherNotImplemented("lookup with labels on non-element: " + this);
        //             }
        //         }

        //         if (element != null) {
        //             Map<String, Object> scope = new HashMap<>();
        //             scope.put(resultName, value);
        //             scope.put(resultName + SCOPE_ELEMENT_SUFFIX, element);
        //             scope.put(resultName + SCOPE_PROPERTY_SUFFIX, property);
        //             scope.put(resultName + SCOPE_PROPERTY_NAME_SUFFIX, elementPropertyName);
        //             row.pushScope(scope);
        //         } else {
        //             row.pushScope(resultName, value);
        //         }
        //     }),
        //     source.getColumnNames()
        // );

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
