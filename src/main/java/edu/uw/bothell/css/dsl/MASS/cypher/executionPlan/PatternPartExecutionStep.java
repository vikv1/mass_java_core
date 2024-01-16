package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PathResult;
import edu.uw.bothell.css.dsl.MASS.cypher.PathResultBase;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

public class PatternPartExecutionStep extends ExecutionStepWithChildren implements ExecutionStepWithResultName {
    private final String resultName;
    private final List<String> pathResultNames;

    public PatternPartExecutionStep(String resultName, MatchPartExecutionStep... matchPartExecutionSteps) {
        super(matchPartExecutionSteps);
        this.resultName = resultName;
        this.pathResultNames = stream(matchPartExecutionSteps)
            .map(MatchPartExecutionStep::getResultName)
            .collect(Collectors.toList());
    }

    @Override
    public String getResultName() {
        return resultName;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        source = super.execute(ctx, source);
        if (resultName != null) {
            source = source.peek(row -> {
                PathResultBase pathResult = new PathResult(
                    pathResultNames.stream()
                        .flatMap(name -> {
                            Object o = row.get(name);
                            if (o == null) {
                                return Stream.of((String) null);
                            // } else if (o instanceof String) {
                            //     return Stream.of((String) o);
                            // } else if (o instanceof PathResultBase) {
                            //     return ((PathResultBase) o).getElements();
                            } else {
                                throw new PropertyGraphCypherNotImplemented("Unhandled value: " + o.getClass().getName());
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                );
                row.set(resultName, pathResult);
            });
        }
        return source;
    }

    @Override
    public String toString() {
        return String.format("In %s: {resultName=%s}", super.toString(), resultName);
    }
}
