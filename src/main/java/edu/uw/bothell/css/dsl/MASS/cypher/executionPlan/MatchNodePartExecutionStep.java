package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class MatchNodePartExecutionStep extends MatchPartExecutionStep {
    public final Set<String> labelNames = new HashSet<>();
    public final Map<String, String> nodeProperties = new HashMap<String,String>();

    public MatchNodePartExecutionStep(
        String originalName,
        String resultName,
        boolean optional,
        Set<String> labelNames,
        List<ExecutionStepWithResultName> properties
    ) {
        super(originalName, resultName, optional, properties);
        
        for(String s : labelNames){
            this.labelNames.add(s.trim().toLowerCase());
        }

        for (ExecutionStepWithResultName step : properties) {
            String key = step.getResultName();
            LiteralExecutionStep gStep = (LiteralExecutionStep) step;
            String value = (String) gStep.getValue();
            nodeProperties.put(key.trim().toLowerCase(), value.trim().toLowerCase());
        }
    }

    @Override
    public String toString() {
        return String.format("%s: {%s}", super.toString(), labelNames);
    }
}
