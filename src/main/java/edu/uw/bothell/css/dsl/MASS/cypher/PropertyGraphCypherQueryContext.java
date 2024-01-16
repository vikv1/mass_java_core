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
    private final Map<String, CypherFunction> functions = new HashMap<>();
    // private final CypherResultWriter resultWriter;
    private ExecutionPlanBuilder executionPlanBuilder = new ExecutionPlanBuilder();
    private ExecutionPlan currentlyExecutingPlan;

    public PropertyGraphCypherQueryContext(PropertyGraphPlaces graph) {
        this.graph = graph;

        // // aggregate
        // addFunction("avg", new AverageFunction());
        // addFunction("collect", new CollectFunction());
        // addFunction("count", new CountFunction());
        // addFunction("max", new MaxFunction());
        // addFunction("min", new MinFunction());
        // addFunction("percentileCont", new PercentileContFunction());
        // addFunction("percentileDisc", new PercentileDiscFunction());
        // addFunction("stDev", new StandardDeviationFunction());
        // addFunction("stDevP", new StandardDeviationPopulationFunction());
        // addFunction("sum", new SumFunction());

        // // list
        // addFunction("extract", new ExtractFunction());
        // addFunction("filter", new FilterFunction());
        // addFunction("keys", new KeysFunction());
        // addFunction("labels", new LabelsFunction());
        // addFunction("nodes", new NodesFunction());
        // addFunction("range", new RangeFunction());
        // addFunction("reduce", new ReduceFunction());
        // addFunction("relationships", new RelationshipsFunction());
        // addFunction("tail", new TailFunction());

        // // math
        // addFunction("abs", new AbsFunction());
        // addFunction("ceil", new CeilFunction());
        // addFunction("floor", new FloorFunction());
        // addFunction("rand", new RandFunction());
        // addFunction("round", new RoundFunction());
        // addFunction("sign", new SignFunction());
        // addFunction("e", new EFunction());
        // addFunction("exp", new ExpFunction());
        // addFunction("log", new LogFunction());
        // addFunction("log10", new Log10Function());
        // addFunction("sqrt", new SquareRootFunction());
        // addFunction("acos", new ACosFunction());
        // addFunction("asin", new ASinFunction());
        // addFunction("atan", new ATanFunction());
        // addFunction("atan2", new ATan2Function());
        // addFunction("cos", new CosFunction());
        // addFunction("cot", new CotFunction());
        // addFunction("degrees", new DegreesFunction());
        // addFunction("haversin", new HaversinFunction());
        // addFunction("pi", new PiFunction());
        // addFunction("radians", new RadiansFunction());
        // addFunction("sin", new SinFunction());
        // addFunction("tan", new TanFunction());

        // addFunction("negate", new NegateFunction());

        // // predicate
        // addFunction("all", new AllFunction());
        // addFunction("any", new AnyFunction());
        // addFunction("exists", new ExistsFunction());
        // addFunction("none", new NoneFunction());
        // addFunction("single", new SingleFunction());

        // // scalar
        // addFunction("coalesce", new CoalesceFunction());
        // addFunction("endNode", new EndNodeFunction());
        // addFunction("head", new HeadFunction());
        // addFunction("id", new IdFunction());
        // addFunction("last", new LastFunction());
        // addFunction("length", new LengthFunction());
        // addFunction("properties", new PropertiesFunction());
        // addFunction("size", new SizeFunction());
        // addFunction("startNode", new StartNodeFunction());
        // addFunction("timestamp", new TimestampFunction());
        // addFunction("toBoolean", new ToBooleanFunction());
        // addFunction("toFloat", new ToFloatFunction());
        // addFunction("toInteger", new ToIntegerFunction());
        // addFunction("type", new TypeFunction());

        // addFunction("isNull", new IsNullFunction());
        // addFunction("isNotNull", new IsNotNullFunction());

        // // spatial
        // addFunction("distance", new DistanceFunction());
        // addFunction("point", new PointFunction());

        // // string
        // addFunction("left", new LeftFunction());
        // addFunction("lTrim", new LTrimFunction());
        // addFunction("replace", new ReplaceFunction());
        // addFunction("reverse", new ReverseFunction());
        // addFunction("right", new RightFunction());
        // addFunction("rTrim", new RTrimFunction());
        // addFunction("split", new SplitFunction());
        // addFunction("substring", new SubstringFunction());
        // addFunction("toLower", new ToLowerFunction());
        // addFunction("lower", new ToLowerFunction());
        // addFunction("toString", new ToStringFunction());
        // addFunction("toUpper", new ToUpperFunction());
        // addFunction("upper", new ToUpperFunction());
        // addFunction("trim", new TrimFunction());

        // addFunction("startsWith", new StartsWithFunction());
        // addFunction("endsWith", new EndsWithFunction());
        // addFunction("contains", new ContainsFunction());

        // // date
        // addFunction("year", new YearFunction());
        // addFunction("month", new MonthFunction());
        // addFunction("day", new DayFunction());
        // addFunction("localdatetime", new LocalDateTimeFunction());
        // addFunction("datetime", new DateTimeFunction());
        // addFunction("date", new DateFunction());
        // addFunction("localtime", new LocalTimeFunction());
        // addFunction("time", new TimeFunction());
        // addFunction("duration.between", new DurationBetweenFunction());
        // addFunction("duration.inMonths", new DurationInMonthsFunction());
        // addFunction("duration.inDays", new DurationInDaysFunction());
        // addFunction("duration.inSeconds", new DurationInSecondsFunction());
    }

    public PropertyGraphPlaces getGraph() {
        return graph;
    }

    public Set<String> getVertexLabels(Object vertexID) {
        return this.graph.getVertexLabels(vertexID);
    }

    public Map<String, CypherFunction> getFunctions() {
        return functions;
    }

    public void addFunction(String name, CypherFunction fn) {
        this.functions.put(name.toLowerCase(), fn);
    }

    public ExecutionStepWithResultName createFunctionExecutionStep(
        String functionName,
        String resultName,
        boolean distinct,
        ExecutionStepWithResultName[] argumentsExecutionStep
    ) {
        CypherFunction fn = functions.get(functionName.toLowerCase());
        if (fn == null) {
            throw new PropertyGraphCypherException(String.format("Function \"%s\" not found", functionName));
        }
        return fn.create(resultName, distinct, argumentsExecutionStep);
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

    public List<Object> getVertexByLabelandProperties(Set<String> labels, Map<String,String> nodeProperties) {
        
        List<Object> result = new ArrayList<Object>();
        Object[] labelVertex = this.graph.callAll(1,this.graph.getArguments(labels.toString()));
        Object[] nodePropertiesVertex = this.graph.callAll(2,this.graph.getArguments(nodeProperties.toString()));
        if(labelVertex.length != nodePropertiesVertex.length) {
            System.err.println("CTX getVertexByLabelandProperties function: Results from label vs nodeProperties are of different length.");
            return result;
        }

        int length = labelVertex.length;
        
        for(int i = 0; i < length; i++){
            if(labelVertex[i] != null && nodePropertiesVertex[i] != null){
                if(labelVertex[i] != nodePropertiesVertex[i]){
                    System.err.println("CTX getVertexByLabelandProperties function: vertexID in labelVertex and nodePropertiesVertex are different.");
                    continue;
                }else{
                    result.add(labelVertex[i]);
                }
            }
        }

        return result;
    }
}
