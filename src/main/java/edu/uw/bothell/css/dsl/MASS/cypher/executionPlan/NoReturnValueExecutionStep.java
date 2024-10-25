package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

import java.util.stream.Stream;

public class NoReturnValueExecutionStep extends DefaultExecutionStep {
    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        source.count();
        return new PropertyGraphCypherResult(Stream.empty(), source.getColumnNames());
    }
}
