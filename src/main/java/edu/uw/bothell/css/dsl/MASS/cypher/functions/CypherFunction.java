package edu.uw.bothell.css.dsl.MASS.cypher.functions;

import edu.uw.bothell.css.dsl.MASS.cypher.executionPlan.ExecutionStepWithResultName;

public interface CypherFunction {
    ExecutionStepWithResultName create(String resultName, boolean distinct, ExecutionStepWithResultName[] argumentsExecutionStep);
}
