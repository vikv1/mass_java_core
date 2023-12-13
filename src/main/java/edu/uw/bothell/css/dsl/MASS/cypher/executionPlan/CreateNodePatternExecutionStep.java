package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherAstBase;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import edu.uw.bothell.css.dsl.MASS.cypher.ElementType;

public class CreateNodePatternExecutionStep extends CreateElementPatternExecutionStep {
    private final List<String> labelNames;

    @SuppressWarnings("unchecked")
    public CreateNodePatternExecutionStep(
        String name,
        List<String> labelNames,
        List<ExecutionStepWithResultName> properties,
        List<ExecutionStep> mergeActions
    ) {
        super(ElementType.NODE, name, properties, mergeActions);
        this.labelNames = labelNames;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        source = super.execute(ctx, source);

        return source.peek(row -> {
            if (row.get(getResultName()) != null) {
                for (ExecutionStep action : mergeActions) {
                    MergeActionExecutionStep mergeAction = (MergeActionExecutionStep) action;
                    if (mergeAction.getType() == MergeActionExecutionStep.Type.MATCH) {
                        mergeAction.execute(ctx, new SingleRowPropertyGraphCypherResult(row)).count();
                    }
                }
                return;
            }

            // store PropertyVertexPlace in MASS library, 
		    // vertexId is the internal reference in MASS library
		    int vertexId = ctx.getGraph().addVertex(this.name); 
		    if(vertexId == -1) {
			    System.err.println("At createNode: Failed at adding Vertex to Graph. ");
			    return;
		    }
            Map<String, String> properties = new HashMap<String, String>();
            for (String propertyResultName : propertyResultNames) {
                Object value = row.get(propertyResultName);
                if (value instanceof CypherAstBase) {
                    throw new PropertyGraphCypherNotImplemented("Unhandled type: " + value.getClass().getName());
                }
                if (value != null) {
                    properties.put(propertyResultName, (String) value);
                }
            }
		    boolean success = ctx.getGraph().setLabelProperties(name, labelNames, properties); // set node properties
		    if(!success) {
			    System.err.println("Failed at setting node property.");
			    return;
		    }
		    System.out.println("Added vertex_ID " + Integer.toString((int) vertexId) + " , labels: " + labelNames + " , properties: " + properties);

            for (ExecutionStep action : mergeActions) {
                    MergeActionExecutionStep mergeAction = (MergeActionExecutionStep) action;
                if (mergeAction.getType() == MergeActionExecutionStep.Type.CREATE) {
                    mergeAction.execute(ctx, new SingleRowPropertyGraphCypherResult(row)).count();
                }
            }
        });
    }

    @Override
    public String toString() {
        return String.format("%s {labelNames=%s}", super.toString(), String.join(", ", labelNames));
    }

}
