package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherAstBase;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        MASS.getLogger().error("At createNodePatternExecutionStep, Item Name:" + name);
        source = super.execute(ctx, source);

        return source.peek(row -> {
            // Get Node properties
            Map<String, String> properties = new HashMap<String, String>();
            for (String propertyResultName : propertyResultNames) {
                Object value = row.get(propertyResultName);
                if (value instanceof CypherAstBase) {
                    throw new PropertyGraphCypherNotImplemented("Unhandled type: " + value.getClass().getName());
                }
                if (value != null) {
                    String sValue = (String) value;
                    properties.put(propertyResultName.trim().toLowerCase(), sValue.trim().toLowerCase());
                }
            }

            // Get Node labels
            List<String> labels = new ArrayList<>();
            for(int i = 0; i < labelNames.size(); i++) {
                labels.add(labelNames.get(i).trim().toLowerCase());
            }

            // store PropertyVertexPlace in MASS library, 
		    // vertexId is the internal reference in MASS library
		    int vertexId = ctx.getGraph().addPropertyVertex(name, labels, properties); // set node properties
		    if(vertexId == -1) {
                // MASS.getLogger().error("At createNodePatternExecutionStep: Failed at adding Vertex to Graph.  Item Name:" + name);
                return;
            }
            
        });
    }

    @Override
    public String toString() {
        return String.format("In %s: {labelNames=%s}", super.toString(), String.join(", ", labelNames));
    }

}
