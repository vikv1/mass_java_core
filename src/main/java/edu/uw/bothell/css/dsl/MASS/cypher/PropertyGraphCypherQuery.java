package edu.uw.bothell.css.dsl.MASS.cypher;

import edu.uw.bothell.css.dsl.MASS.MASS;
import edu.uw.bothell.css.dsl.MASS.antlr.CypherLexer;
import edu.uw.bothell.css.dsl.MASS.antlr.CypherParser;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherException;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.CypherAstParser;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.CypherCompilerContext;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherStatement;
import edu.uw.bothell.css.dsl.MASS.cypher.executionPlan.ExecutionPlan;
import org.antlr.v4.runtime.*;
import edu.uw.bothell.css.dsl.MASS.antlr.CypherLexer;
import edu.uw.bothell.css.dsl.MASS.antlr.CypherParser;

import static com.google.common.base.Preconditions.checkNotNull;

import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class PropertyGraphCypherQuery {
    private final CypherStatement statement;

    protected PropertyGraphCypherQuery(CypherStatement statement) {
        checkNotNull(statement, "statement is required");
        this.statement = statement;
    }

    public static PropertyGraphCypherQuery parse(String queryString) {
        CypherStatement statement = CypherAstParser.getInstance().parse(queryString);
        if (statement == null) {
            throw new PropertyGraphCypherException("Failed to parse query: " + queryString);
        }
        return new PropertyGraphCypherQuery(statement);
    }

    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx) {
        MASS.getLogger().debug("Executing CypherStatement (AST):\n {}", statement.toString());

        ExecutionPlan plan = ctx.getExecutionPlanBuilder().build(ctx, statement);
        MASS.getLogger().debug("Execution plan:\n {}", plan.toStringFull());
        
        return plan.execute(ctx);
    }

}
