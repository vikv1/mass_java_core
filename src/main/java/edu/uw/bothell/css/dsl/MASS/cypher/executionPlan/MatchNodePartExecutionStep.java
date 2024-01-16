package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.RelationshipRangePathResult;
import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherException;
import edu.uw.bothell.css.dsl.MASS.PropertyGraphPlaces;
import edu.uw.bothell.css.dsl.MASS.PropertyVertexPlace;
import static edu.uw.bothell.css.dsl.MASS.cypher.utils.StreamUtils.stream;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class MatchNodePartExecutionStep extends MatchPartExecutionStep<MatchRelationshipPartExecutionStep> {
    private final Set<String> labelNames;

    public MatchNodePartExecutionStep(
        String originalName,
        String resultName,
        boolean optional,
        Set<String> labelNames,
        List<ExecutionStepWithResultName> properties
    ) {
        super(originalName, resultName, optional, properties);
        this.labelNames = labelNames;
    }

    protected Stream<CypherResultRow> executeInitialQuery(PropertyGraphCypherQueryContext ctx, CypherResultRow row) {
        Map<String,String> nodeProperties = new HashMap<String,String>();

        for (String propertyName : propertyResultNames) {
            Object value = row.get(propertyName);
            nodeProperties.put(propertyName, (String) value);
        }

        List<Object> elements = ctx.getVertexByLabelandProperties(labelNames, nodeProperties);

        // Optional Match
        if (isOptional()) {
            CypherResultRow newRow = row.clone()
                .set(getResultName(), null);
            return new SingleRowPropertyGraphCypherResult(newRow);
        }

        return stream(elements)
            .map(element -> {
                CypherResultRow newRow = row.clone();
                newRow.set(getResultName(), element);
                return newRow;
            });
    }

    @Override
    protected Stream<? extends CypherResultRow> executeConnectedGetElements(PropertyGraphCypherQueryContext ctx, CypherResultRow row) {
        if (row.get(getResultName()) != null) {
            // TODO later
            return Stream.of(row);
        }

        if (getConnectedSteps().size() == 0) {
            throw new PropertyGraphCypherException("Should be using executeInitialQuery not connected elements");
        }

        if (isOptional() && isAllConnectedStepsCompletedAndNull(row)) {
            row.set(getResultName(), null);
            return Stream.of(row);
        }

        Set<String> vertexIds = getConnectedSteps().stream()
            .map(step -> step.getOtherVertexId(row, this))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (vertexIds.size() == 0) {
            if (isConnectedToZeroLengthEdge(row)) {
                return executeInitialQuery(ctx, row);
            }
            throw new PropertyGraphCypherException("Failed to find other vertex ids");
        }
        if (vertexIds.size() != 1) {
            throw new PropertyGraphCypherException("expecting only a single vertex but found: " + vertexIds.size());
        }
        String vertexId = vertexIds.iterator().next();
        PropertyVertexPlace vertex = (PropertyVertexPlace) ctx.getGraph().getVertex(vertexId); 
        if (vertex == null) {
            throw new PropertyGraphCypherException("could not find vertex " + vertexId);
        }

        if (labelNames.size() > 0) {
            Set<String> vertexLabels = ctx.getVertexLabels(vertex);
            for (String labelName : labelNames) {
                if (!vertexLabels.contains(labelName)) {
                    return Stream.empty();
                }
            }
        }

        row.set(getResultName(), vertex);
        return Stream.of(row);
    }

    private boolean isConnectedToZeroLengthEdge(CypherResultRow row) {
        return getConnectedSteps().stream()
            .anyMatch(step -> {
                Object stepValue = row.get(step.getResultName());
                // if (stepValue instanceof RelationshipRangePathResult) {
                //     RelationshipRangePathResult stepPathResult = (RelationshipRangePathResult) stepValue;
                //     if (stepPathResult.getLength() == 0) {
                //         return true;
                //     }
                // }
                return false;
            });
    }

    private boolean isAllConnectedStepsCompletedAndNull(CypherResultRow row) {
        return getConnectedSteps().stream()
            .allMatch(step -> row.get(step.getResultName()) == null);
    }

    @Override
    public String toString() {
        return String.format("In %s: {labelNames=%s}", super.toString(), labelNames);
    }
}
