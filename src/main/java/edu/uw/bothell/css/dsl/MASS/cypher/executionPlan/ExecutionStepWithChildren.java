package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ExecutionStepWithChildren extends DefaultExecutionStep {
    private final List<ExecutionStep> childSteps = new ArrayList<>();

    public ExecutionStepWithChildren(ExecutionStep... children) {
        addChildSteps(Arrays.stream(children));
    }

    public Stream<ExecutionStep> getChildSteps() {
        return childSteps.stream();
    }

    void addChildStep(ExecutionStep p) {
        checkNotNull(p);
        childSteps.add(p);
    }

    void addChildSteps(Stream<ExecutionStep> steps) {
        steps.forEach(this::addChildStep);
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        for (ExecutionStep childStep : childSteps) {
            source = childStep.execute(ctx, source);
        }
        return source;
    }

    @Override
    public String toStringFull() {
        StringBuilder result = new StringBuilder();
        result.append(toString());
        getChildSteps().forEach(child -> {
            String childString = StringUtils.indent(2, child.toStringFull());
            result.append('\n').append(childString);
        });
        return result.toString();
    }

}
