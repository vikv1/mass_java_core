package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;


import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherAstBase;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherDirection;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;
import edu.uw.bothell.css.dsl.MASS.cypher.ElementType;

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

          

            // use Map to store relationship type and value
			Map<String, String> relationProperties = new HashMap<String, String>();
            for (String propertyResultName : propertyResultNames) {
                Object value = row.get(propertyResultName);
                if (value instanceof CypherAstBase) {
                    throw new PropertyGraphCypherNotImplemented("Unhandled type: " + value.getClass().getName());
                }
                if (value != null) {
                    relationProperties.put(propertyResultName, (String) value);
                }
            }
			// From ID and To ID for edge
            Object outVertex = direction.hasOut() ? leftNodeName : rightNodeName;
            Object inVertex = direction.hasOut() ? rightNodeName : leftNodeName;

			System.out.println("Adding Edge from " + outVertex + " to " + inVertex);
			boolean success = ctx.getGraph().setRelationEdge(outVertex, inVertex, relationProperties);

            if(!success) {
                System.err.println("Failed at adding Edge to Graph, from " + outVertex + " to " + inVertex + ".");
                return;
            }
            System.out.println("Successfully added Edge to Graph, from " + outVertex + " to " + inVertex + ".");


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
        return String.format(
            "%s {relTypeNames=%s, direction=%s, leftNodeName='%s', rightNodeName='%s'}",
            super.toString(),
            String.join(", ", relTypeNames),
            direction,
            leftNodeName,
            rightNodeName
        );
    }
}
