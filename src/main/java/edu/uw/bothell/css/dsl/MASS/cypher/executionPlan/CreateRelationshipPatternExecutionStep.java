package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;


import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherAstBase;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherDirection;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;
import edu.uw.bothell.css.dsl.MASS.cypher.ElementType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateRelationshipPatternExecutionStep extends CreateElementPatternExecutionStep {
    private final List<String> relTypeNames;
    private final CypherDirection direction;
    private final String leftNodeName;
    private final String rightNodeName;

    @SuppressWarnings("unchecked")
    public CreateRelationshipPatternExecutionStep(
        String name,
        List<String> relTypeNames,
        CypherDirection direction,
        String leftNodeName,
        String rightNodeName,
        List<ExecutionStepWithResultName> properties,
        List<ExecutionStep> mergeActions
    ) {
        super(ElementType.EDGE, name, properties, mergeActions);
        this.relTypeNames = relTypeNames;
        this.direction = direction;
        this.leftNodeName = leftNodeName;
        this.rightNodeName = rightNodeName;
    }

    public List<String> getRelTypeNames() {
        return relTypeNames;
    }

    public CypherDirection getDirection() {
        return direction;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        MASS.getLogger().error("At createNodePatternExecutionStep, Item Name:" + name);
        source = super.execute(ctx, source);

        return source.peek(row -> {
        
            // use Map to store relationship type and value
			Map<String, String> relationProperties = new HashMap<String, String>();
            for (String propertyResultName : propertyResultNames) {
                Object value = row.get(propertyResultName);
                if (value instanceof CypherAstBase) {
                    throw new PropertyGraphCypherNotImplemented("Unhandled type: " + value.getClass().getName());
                }
                if (value != null) {
                    String sValue = (String) value;
                    relationProperties.put(propertyResultName.trim().toLowerCase(), sValue.trim().toLowerCase());
                }
            }
			// From ID and To ID for edge
            Object outVertex = direction.hasOut() ? leftNodeName.trim() : rightNodeName.trim();
            Object inVertex = direction.hasOut() ? rightNodeName.trim() : leftNodeName.trim();

            List<String> types = new ArrayList<>();
            for(int i = 0; i < this.relTypeNames.size(); i++) {
                types.add(this.relTypeNames.get(i).trim().toLowerCase());
            }

			boolean success = ctx.getGraph().setRelationEdge(outVertex, inVertex, types, relationProperties);

            if(!success) {
                return;
            }

        });
    }
    
    @Override
    public String toString() {
        return String.format(
            "%s: {%s, %s, '%s', '%s'}",
            super.toString(),
            String.join(", ", relTypeNames),
            direction,
            leftNodeName,
            rightNodeName
        );
    }
}
