package edu.uw.bothell.css.dsl.MASS.cypher;

import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherException;
// import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;
import edu.uw.bothell.css.dsl.MASS.cypher.executionPlan.*;
import edu.uw.bothell.css.dsl.MASS.cypher.functions.CypherFunction;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.aggregate.*;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.date.*;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.date.duration.DurationBetweenFunction;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.date.duration.DurationInDaysFunction;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.date.duration.DurationInMonthsFunction;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.date.duration.DurationInSecondsFunction;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.list.*;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.math.*;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.predicate.*;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.scalar.*;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.spatial.DistanceFunction;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.spatial.PointFunction;
// import edu.uw.bothell.css.dsl.MASS.cypher.functions.string.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import edu.uw.bothell.css.dsl.MASS.PropertyGraphPlaces;

// import static edu.uw.bothell.css.dsl.MASS.cypher.ast.model.StreamUtils.stream;

public class PropertyGraphCypherQueryContext {
    private final PropertyGraphPlaces graph;
    // private final Map<String, CypherFunction> functions = new HashMap<>();
    private ExecutionPlanBuilder executionPlanBuilder = new ExecutionPlanBuilder();
    private ExecutionPlan currentlyExecutingPlan;

    public PropertyGraphCypherQueryContext(PropertyGraphPlaces graph) {
        this.graph = graph;

        // // math
        // addFunction("abs", new AbsFunction());
        // addFunction("e", new EFunction());
        // addFunction("exp", new ExpFunction());
        // addFunction("log", new LogFunction());
        // addFunction("log10", new Log10Function());
        // addFunction("sqrt", new SquareRootFunction());

    }

    public PropertyGraphPlaces getGraph() {
        return graph;
    }

    // public Set<String> getVertexLabels(Object vertexID) {
    //     return this.graph.getVertexLabels(vertexID);
    // }

    // public Map<String, CypherFunction> getFunctions() {
    //     return functions;
    // }

    // public void addFunction(String name, CypherFunction fn) {
    //     this.functions.put(name.toLowerCase(), fn);
    // }
    
    public ExecutionPlanBuilder getExecutionPlanBuilder() {
        return executionPlanBuilder;
    }

    public void setCurrentlyExecutingPlan(ExecutionPlan currentlyExecutingPlan) {
        this.currentlyExecutingPlan = currentlyExecutingPlan;
    }

    public ExecutionPlan getCurrentlyExecutingPlan() {
        return currentlyExecutingPlan;
    }

    // public List<Object> getVertexByLabelandProperties(Set<String> labels, Map<String,String> nodeProperties) {
        
    //     List<Object> result = new ArrayList<Object>();
    //     List<Object> arguments = new ArrayList<>();
    //     arguments.add(labels);
    //     arguments.add(nodeProperties);

    //     Object[] labelVertex = this.graph.callAll(1,this.graph.getArguments(arguments));

    //     int length = labelVertex.length;
        
    //     for(int i = 0; i < length; i++){
    //         if(labelVertex[i] != null && !labelVertex[i].equals("")){
    //            result.add(labelVertex[i]);
    //         }
    //     }
        
    //     // System.out.println("At CTX, printing results: ");
    //     // for(int i = 0; i < result.size(); i++) {
    //     //     System.out.println("result at " + i + " is " + result.get(i));
    //     // }
    //     return result;
    // }
}
