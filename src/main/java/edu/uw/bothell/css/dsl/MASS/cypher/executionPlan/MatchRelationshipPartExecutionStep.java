package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

// import edu.uw.bothell.css.dsl.MASS.cypher.Direction;
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

public class MatchRelationshipPartExecutionStep extends MatchPartExecutionStep<MatchNodePartExecutionStep> {
    private final List<String> relTypesNames;
    private final CypherDirection direction;
    private final CypherRangeLiteral range;

    public MatchRelationshipPartExecutionStep(
        String originalName,
        String resultName,
        boolean optional,
        List<String> relTypesNames,
        CypherDirection direction,
        CypherRangeLiteral range,
        List<ExecutionStepWithResultName> properties
    ) {
        super(originalName, resultName, optional, properties);
        this.relTypesNames = relTypesNames;
        this.direction = direction;
        this.range = range;
    }

    @Override
    protected Stream<? extends CypherResultRow> executeConnectedGetElements(PropertyGraphCypherQueryContext ctx, CypherResultRow row) {
        if (row.get(getResultName()) != null) {
            // TODO apply additional filters?
            return Stream.of(row);
        }

        // List<? extends Element> connectedElements = getConnectedSteps().stream()
        //     .map(MatchPartExecutionStep::getResultName)
        //     .map(row::get)
        //     .map(e -> (Element) e)
        //     .collect(Collectors.toList());

        // if (connectedElements.size() != 2) {
        //     throw new PropertyGraphCypherException("Expected 2 connected elements found " + connectedElements.size());
        // }
        // if (connectedElements.get(0) != null && !(connectedElements.get(0) instanceof Vertex)) {
        //     throw new PropertyGraphCypherException("Expected Vertex found " + connectedElements.get(0).getClass().getName());
        // }
        // if (connectedElements.get(1) != null && !(connectedElements.get(1) instanceof Vertex)) {
        //     throw new PropertyGraphCypherException("Expected Vertex found " + connectedElements.get(1).getClass().getName());
        // }
        // Vertex left = (Vertex) connectedElements.get(0);
        // Vertex right = (Vertex) connectedElements.get(1);

        // Set<EdgeData> edgeDatas = new HashSet<>();

        // if (left != null) {
        //     Direction direction = toPropertyGraphQueryDirection(this.direction);
        //     Set<EdgeData> leftEdgeData = stream(left.getEdgeIds(direction, ctx.getAuthorizations()))
        //         .map(edgeId -> new EdgeData(left, edgeId))
        //         .collect(Collectors.toSet());
        //     edgeDatas.addAll(leftEdgeData);
        // }

        // // if this is a cyclic edge (left == right) don't add the edge id twice
        // if (right != null && left != right) {
        //     Direction direction = toPropertyGraphQueryDirection(this.direction).reverse();
        //     Set<EdgeData> rightEdgeData = stream(right.getEdgeIds(direction, ctx.getAuthorizations()))
        //         .map(edgeId -> new EdgeData(right, edgeId))
        //         .collect(Collectors.toSet());
        //     edgeDatas.addAll(rightEdgeData);
        // }

        // edgeDatas = populateAndFilterEdgeData(ctx, row, edgeDatas);

        // Stream<Object> result;
        // if (range != null) {
        //     result = edgeDatas.stream()
        //         .map(edgeData -> new RelationshipRangePathResult(edgeData.source, edgeData.edge))
        //         .flatMap(path -> expandPath(ctx, row, path, range))
        //         .map(path -> new RelationshipRangePathResult(path.getElements().skip(1).collect(Collectors.toList())))
        //         .filter(p -> p.getTailElement() instanceof Edge)
        //         .distinct()
        //         .map(p -> p);
        //     if (range.getFrom() != null && range.getFrom() == 0) {
        //         result = Stream.concat(Stream.of(new RelationshipRangePathResult()), result);
        //     }
        // } else {
        //     result = edgeDatas.stream().map(ed -> ed.edge);
        // }

        // Function<Object, CypherResultRow> transformResults = edge -> row.clone().set(getResultName(), edge);

        // if (isOptional()) {
        //     return mapOptional(result, transformResults);
        // } else {
        //     return result.map(transformResults);
        // }

        return null;
    }

    // private Stream<PathResultBase> expandPath(
    //     PropertyGraphCypherQueryContext ctx,
    //     CypherResultRow row,
    //     RelationshipRangePathResult path,
    //     CypherRangeLiteral range
    // ) {
    //     Stream<PathResultBase> results;
    //     if (range.getTo() != null && range.getTo() == 0) {
    //         return Stream.empty();
    //     }
    //     if (range.isInRange(path.getLength())) {
    //         results = Stream.of(path);
    //     } else {
    //         results = Stream.empty();
    //     }

    //     Element lastElement = path.getTailElement();
    //     if (lastElement instanceof Edge) {
    //         Edge edge = (Edge) lastElement;
    //         String vertexId = edge.getOtherVertexId(path.getLastVertex().getId());
    //         if (path.containsVertexId(vertexId)) {
    //             return results;
    //         }
    //         Vertex vertex = ctx.getGraph().getVertex(vertexId, ctx.getAuthorizations());
    //         return Stream.concat(results, expandPath(ctx, row, new RelationshipRangePathResult(path, vertex), range));
    //     } else if (lastElement instanceof Vertex) {
    //         Vertex vertex = (Vertex) lastElement;
    //         Set<EdgeData> edgeDatas = stream(vertex.getEdgeIds(toPropertyGraphQueryDirection(this.direction), ctx.getAuthorizations()))
    //             .map(edgeId -> new EdgeData(vertex, edgeId))
    //             .collect(Collectors.toSet());
    //         edgeDatas = populateAndFilterEdgeData(ctx, row, edgeDatas);
    //         Stream<PathResultBase> additionalPaths = edgeDatas.stream()
    //             .map(edgeData -> new RelationshipRangePathResult(path, edgeData.edge))
    //             .flatMap(p -> expandPath(ctx, row, p, range));
    //         return Stream.concat(results, additionalPaths);
    //     } else {
    //         throw new PropertyGraphCypherNotImplemented("Unhandled element type: " + lastElement.getClass().getName());
    //     }
    // }

    // private Set<EdgeData> populateAndFilterEdgeData(PropertyGraphCypherQueryContext ctx, CypherResultRow row, Set<EdgeData> edgeDatas) {
    //     Iterable<String> edgeIds = edgeDatas.stream().map(ed -> ed.edgeId).collect(Collectors.toList());
    //     Map<String, Edge> edgesById = stream(ctx.getGraph().getEdges(edgeIds, ctx.getAuthorizations()))
    //         .collect(Collectors.toMap(Element::getId, e -> e));
    //     return edgeDatas.stream()
    //         .peek(edgeData -> edgeData.edge = edgesById.get(edgeData.edgeId))
    //         .filter(edgeData -> edgeData.edge != null)
    //         .filter(edgeData -> doesEdgeMatch(ctx, row, edgeData.edge))
    //         .collect(Collectors.toSet());
    // }

    // private boolean doesEdgeMatch(PropertyGraphCypherQueryContext ctx, CypherResultRow row, Edge edge) {
    //     if (!doLabelNamesMatch(ctx, edge)) {
    //         return false;
    //     }
    //     return doPropertiesMatch(ctx, row, edge);
    // }

    // private boolean doLabelNamesMatch(PropertyGraphCypherQueryContext ctx, Edge edge) {
    //     if (relTypesNames.size() > 0) {
    //         Stream<String> labelNames = relTypesNames.stream()
    //             .map(ctx::normalizeLabelName);
    //         if (labelNames.noneMatch(ln -> edge.getLabel().equals(ln))) {
    //             return false;
    //         }
    //     }
    //     return true;
    // }

    // protected boolean doPropertiesMatch(PropertyGraphCypherQueryContext ctx, CypherResultRow row, Element element) {
    //     if (propertyResultNames.size() == 0) {
    //         return true;
    //     }
    //     for (String propertyResultName : propertyResultNames) {
    //         String propertyName = ctx.normalizePropertyName(propertyResultName);
    //         Object value = row.get(propertyResultName);
    //         for (Object propertyValue : element.getPropertyValues(propertyName)) {
    //             if (propertyValue.equals(value)) {
    //                 return true;
    //             }
    //         }
    //     }
    //     return false;
    // }
        
    protected Stream<CypherResultRow> executeInitialQuery(PropertyGraphCypherQueryContext ctx, CypherResultRow row) {
        // for (String propertyName : propertyResultNames) {
        //     Object value = row.get(propertyName);
        //     if (!ctx.getGraph().isPropertyDefined(propertyName)) {
        //         ctx.defineProperty(propertyName, value);
        //     }
        //     q.has(propertyName, value);
        // }

        List<Object> elements = null;
        if (isOptional()) {
            CypherResultRow newRow = row.clone()
                .set(getResultName(), null);
            return new SingleRowPropertyGraphCypherResult(newRow);
        }

        return stream(elements)
            .map(element -> {
                CypherResultRow newRow = row.clone();
                newRow.set(getResultName(), element);
                return newRow;
            });
    }


    // private Direction toPropertyGraphQueryDirection(CypherDirection direction) {
    //     if (direction == CypherDirection.BOTH || direction == CypherDirection.UNSPECIFIED) {
    //         return Direction.BOTH;
    //     }
    //     if (direction == CypherDirection.OUT) {
    //         return Direction.OUT;
    //     }
    //     if (direction == CypherDirection.IN) {
    //         return Direction.IN;
    //     }
    //     throw new PropertyGraphCypherException("Unhandled direction: " + direction);
    // }

    public String getOtherVertexId(CypherResultRow row, MatchNodePartExecutionStep nodePartExecutionStep) {
        // MatchNodePartExecutionStep otherExecutionStep = getOtherExecutionStep(nodePartExecutionStep);
        // Vertex otherVertex = (Vertex) row.get(otherExecutionStep.getResultName());
        // if (otherVertex != null) {
        //     Object value = row.get(getResultName());
        //     if (value == null) {
        //         if (row.has(getResultName())) {
        //             return null;
        //         }
        //         throw new PropertyGraphCypherNotImplemented("could not find edge or path");
        //     }
        //     if (value instanceof Edge) {
        //         Edge edge = (Edge) value;
        //         return edge.getOtherVertexId(otherVertex.getId());
        //     } else if (value instanceof PathResultBase) {
        //         PathResultBase pathResult = (PathResultBase) value;
        //         return pathResult.getOtherVertexId(otherVertex.getId());
        //     } else {
        //         throw new PropertyGraphCypherNotImplemented("Unhandled type: " + value + " (class: " + value.getClass() + ")");
        //     }
        // }
        return null;
    }

    private MatchNodePartExecutionStep getOtherExecutionStep(MatchNodePartExecutionStep nodePartExecutionStep) {
        if (getConnectedSteps().size() != 2) {
            throw new PropertyGraphCypherException("Expected 2 connected elements found " + getConnectedSteps().size());
        }
        if (getConnectedSteps().get(0) == nodePartExecutionStep) {
            return getConnectedSteps().get(1);
        }
        if (getConnectedSteps().get(1) == nodePartExecutionStep) {
            return getConnectedSteps().get(0);
        }
        throw new PropertyGraphCypherException("Could not find execution step on either end: " + nodePartExecutionStep);
    }

    // private static class EdgeData {
    //     public final Vertex source;
    //     public final String edgeId;
    //     public Edge edge;

    //     public EdgeData(Vertex source, String edgeId) {
    //         this.source = source;
    //         this.edgeId = edgeId;
    //     }

    //     @Override
    //     public boolean equals(Object o) {
    //         if (this == o) {
    //             return true;
    //         }
    //         if (o == null || getClass() != o.getClass()) {
    //             return false;
    //         }
    //         EdgeData edgeData = (EdgeData) o;
    //         return edgeId.equals(edgeData.edgeId);
    //     }

    //     @Override
    //     public int hashCode() {
    //         return Objects.hash(edgeId);
    //     }
    // }

    @Override
    public String toString() {
        return String.format("In %s: {relationshipType = %s}", super.toString(), relTypesNames);
    }
}
