package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.*;
import edu.uw.bothell.css.dsl.MASS.cypher.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

public class MatchPatternPartExecutionStep extends ExecutionStepWithChildren implements ExecutionStepWithResultName {
    private final String resultName;
    private final List<String> pathResultNames;
    private final List<String> nodesResultNames;
    private List<Object[]> arguments;
    private int nodeNumber = 0;

    public MatchPatternPartExecutionStep(String resultName, MatchPartExecutionStep... matchPartExecutionSteps) {
        this.resultName = resultName;
        this.pathResultNames = stream(matchPartExecutionSteps)
            .map(MatchPartExecutionStep::getResultName)
            .collect(Collectors.toList());
        arguments = new ArrayList<>();
        nodesResultNames = new ArrayList<>();

        // prepare arguments send to MASSBase for Agents to fetch match clause results
        for(int i = 0; i < matchPartExecutionSteps.length; i++) {
            MatchNodePartExecutionStep nodeStep = (MatchNodePartExecutionStep) matchPartExecutionSteps[i];
            nodeNumber++;
            nodesResultNames.add(nodeStep.getResultName());
            i++;
            MatchRelationshipPartExecutionStep relStep = i < matchPartExecutionSteps.length ? (MatchRelationshipPartExecutionStep) matchPartExecutionSteps[i] : null;
            
            Object[] currArgs = new Object[5];
            currArgs[0] = (Object) nodeStep.labelNames;
            currArgs[1] = (Object) nodeStep.nodeProperties;
            currArgs[2] = relStep == null? (Object) "NULL" : (Object) relStep.direction;
            currArgs[3] = relStep == null? (Object) "NULL" : (Object) relStep.relTypes;
            currArgs[4] = relStep == null? (Object) "NULL" : (Object) relStep.relProperties;
            arguments.add(currArgs);
        }
    }

    @Override
    public String getResultName() {
        return resultName;
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult originalSource) {
        
        int thisInitPopulation = ctx.getGraph().getThisGraphPlacesSize();
        
        Agents agents = new Agents(0, PropertyGraphAgent.class.getName(), null, ctx.getGraph(), thisInitPopulation);
    
        Object[] results = (Object[]) agents.PropertyGraphDoAll(0, this.arguments, nodeNumber);
        
        MASS.getLogger().debug("At MacthPatternPartExecutionStep: Agents size " + agents.nAgents() );

        LinkedHashSet<String> columnNames = new LinkedHashSet<String>(pathResultNames);
        
        Stream<CypherResultRow> rows = Arrays.stream(results)
                                                .filter(result -> ((ArrayList<String>) result).size() == nodesResultNames.size())
                                                .map(result -> {
                                                    ArrayList<String> elements = (ArrayList<String>) result; 
                                                    Map<String, Object> elementMap = new HashMap<>();
                                                    AtomicInteger i = new AtomicInteger(0); // Using AtomicInteger for indexing within forEach
                                                    for(String x : elements) {
                                                        elementMap.put(nodesResultNames.get(i.getAndIncrement()), (Object) x);
                                                    }
                                                    return (CypherResultRow) new DefaultCypherResultRow(columnNames, elementMap);
                                                });

        PropertyGraphCypherResult source = new PropertyGraphCypherResult(rows, columnNames);

        return source;
    }

    @Override
    public String toString() {
        return String.format("%s: {%s}", super.toString(), resultName);
    }
}
