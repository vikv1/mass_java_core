package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.ElementType;
import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherAstBase;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;

import java.util.List;
import java.util.stream.Collectors;

public abstract class CreateElementPatternExecutionStep extends ExecutionStepWithChildren implements ExecutionStepWithResultName {
    protected final ElementType elementType;
    protected final String name;
    protected final List<String> propertyResultNames;
    protected final List<ExecutionStep> mergeActions;

    public CreateElementPatternExecutionStep(
        ElementType elementType,
        String name,
        List<ExecutionStepWithResultName> properties,
        List<ExecutionStep> mergeActions
    ) {
        super(properties.toArray(new ExecutionStepWithResultName[0]));
        this.elementType = elementType;
        this.name = name;
        this.propertyResultNames = properties.stream().map(ExecutionStepWithResultName::getResultName).collect(Collectors.toList());
        this.mergeActions = mergeActions;
    }

    @Override
    public String getResultName() {
        return name;
    }
}
