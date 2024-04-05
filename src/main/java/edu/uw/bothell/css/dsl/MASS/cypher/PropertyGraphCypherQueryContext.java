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

public class PropertyGraphCypherQueryContext {
    private final PropertyGraphPlaces graph;
    private ExecutionPlanBuilder executionPlanBuilder = new ExecutionPlanBuilder();
    private ExecutionPlan currentlyExecutingPlan;

    public PropertyGraphCypherQueryContext(PropertyGraphPlaces graph) {
        this.graph = graph;
    }

    public PropertyGraphPlaces getGraph() {
        return graph;
    }
    
    public ExecutionPlanBuilder getExecutionPlanBuilder() {
        return executionPlanBuilder;
    }

    public void setCurrentlyExecutingPlan(ExecutionPlan currentlyExecutingPlan) {
        this.currentlyExecutingPlan = currentlyExecutingPlan;
    }

    public ExecutionPlan getCurrentlyExecutingPlan() {
        return currentlyExecutingPlan;
    }
}
