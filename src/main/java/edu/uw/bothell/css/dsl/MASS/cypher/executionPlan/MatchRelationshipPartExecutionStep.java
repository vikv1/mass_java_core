package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.PathResultBase;
import edu.uw.bothell.css.dsl.MASS.cypher.RelationshipRangePathResult;
import edu.uw.bothell.css.dsl.MASS.cypher.SingleRowPropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherDirection;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherRangeLiteral;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherException;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import static edu.uw.bothell.css.dsl.MASS.cypher.utils.StreamUtils.stream;

public class MatchRelationshipPartExecutionStep extends MatchPartExecutionStep {
    public final String direction;
    public final Set<String> relTypes = new HashSet<>();;
    public final Map<String, String> relProperties = new HashMap<String,String>();
    public final CypherRangeLiteral range;

    public MatchRelationshipPartExecutionStep(
        String originalName,
        String resultName,
        boolean optional,
        List<String> relTypes,
        CypherDirection direction,
        CypherRangeLiteral range,
        List<ExecutionStepWithResultName> properties
    ) {
        super(originalName, resultName, optional, properties);
        this.direction = direction.toString().trim().toLowerCase();
        this.range = range;

        for(String s : relTypes){
            this.relTypes.add(s.trim().toLowerCase());
        }

        for (ExecutionStepWithResultName step : properties) {
            String key = step.getResultName();
            LiteralExecutionStep gStep = (LiteralExecutionStep) step;
            String value = (String) gStep.getValue();
            relProperties.put(key.trim().toLowerCase(), value.trim().toLowerCase());
        }
    }

    @Override
    public String toString() {
        return String.format("%s: {%s}", super.toString(), relTypes);
    }
}
