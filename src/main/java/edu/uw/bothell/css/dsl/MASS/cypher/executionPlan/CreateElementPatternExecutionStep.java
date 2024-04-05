package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.ElementType;

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
        this.name = name.trim();
        this.propertyResultNames = properties.stream().map(ExecutionStepWithResultName::getResultName).collect(Collectors.toList());
        this.mergeActions = mergeActions;
    }

    @Override
    public String getResultName() {
        return name;
    }
}
