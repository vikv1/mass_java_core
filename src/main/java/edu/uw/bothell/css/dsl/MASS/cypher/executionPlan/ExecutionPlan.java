package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;

public class ExecutionPlan {
    private final ExecutionStep root;

    public ExecutionPlan(ExecutionStep root) {
        this.root = root;
    }

    public String toStringFull() {
        return root.toStringFull();
    }

    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx) {
        ctx.setCurrentlyExecutingPlan(this);
        return root.execute(ctx, null);
    }

    public ExecutionStep getRoot() {
        return root;
    }
}