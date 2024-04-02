package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import com.google.common.collect.ImmutableList;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.*;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherException;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExecutionPlanBuilder {
    public ExecutionPlan build(PropertyGraphCypherQueryContext ctx, CypherStatement statement) {
        return new ExecutionPlan(visitStatement(ctx, statement));
    }

    private ExecutionStep visitStatement(PropertyGraphCypherQueryContext ctx, CypherStatement statement) {
        return visitQueryOrUnion(ctx, statement.getQuery());
    }

    private ExecutionStep visitQueryOrUnion(PropertyGraphCypherQueryContext ctx, CypherAstBase query) {
        if (query instanceof CypherQuery) {
            return visitQuery(ctx, (CypherQuery) query);
        } else if (query instanceof CypherUnion) {
            return visitUnion(ctx, (CypherUnion) query);
        } else {
            throw new PropertyGraphCypherNotImplemented("unhandled query type: " + query.getClass().getName());
        }
    }

    private ExecutionStep visitUnion(PropertyGraphCypherQueryContext ctx, CypherUnion union) {
        return new UnionExecutionStep(
            union.isAll(),
            visitQueryOrUnion(ctx, union.getLeft()),
            visitQueryOrUnion(ctx, union.getRight())
        );
    }

    private ExecutionStep visitQuery(PropertyGraphCypherQueryContext ctx, CypherQuery query) {
        SeriesExecutionStep executionPlan = new SeriesExecutionStep();
        ImmutableList<CypherClause> clauses = query.getClauses();
        for (int i = 0; i < clauses.size(); i++) {
            CypherClause clause = clauses.get(i);
            if (clause instanceof CypherCreateClause) {
                executionPlan.addChildStep(visitCreateClause(ctx, (CypherCreateClause) clause));
            } else if (clause instanceof CypherMatchClause) {
                executionPlan.addChildStep(visitMatchClause(ctx, (CypherMatchClause) clause));
            } else if (clause instanceof CypherReturnClause) {
                executionPlan.addChildStep(visitReturnClause(ctx, (CypherReturnClause) clause));
            } else {
                throw new PropertyGraphCypherNotImplemented("unhandled clause type (" + clause.getClass().getName() + "): " + clause);
            }
        }

        if (!(clauses.get(clauses.size() - 1) instanceof CypherReturnClause)) {
            executionPlan.addChildStep(new NoReturnValueExecutionStep());
        }

        return executionPlan;
    }


    private ReturnExecutionStep visitReturnClause(PropertyGraphCypherQueryContext ctx, CypherReturnClause clause) {
        boolean distinct = clause.isDistinct();
        CypherReturnBody returnBody = clause.getReturnBody();
        return visitReturnBody(ctx, distinct, returnBody);
    }

    private ReturnExecutionStep visitReturnBody(PropertyGraphCypherQueryContext ctx, boolean distinct, CypherReturnBody returnBody) {
        return new ReturnExecutionStep(
            distinct,
            returnBody.getReturnItems().stream()
                .map(returnItem -> new ReturnPartExecutionStep(
                    returnItem.getAlias(),
                    returnItem.getOriginalText(),
                    returnItem.getOriginalText().equals("*")
                        ? null
                        : visitExpression(ctx, returnItem.getExpression())
                ))
                .collect(Collectors.toList()),
            returnBody.getSkip() == null ? null : visitExpression(ctx, returnBody.getSkip().getExpression()),
            returnBody.getLimit() == null ? null : visitExpression(ctx, returnBody.getLimit().getExpression())
        );
    }

    public ExecutionStepWithResultName visitExpression(PropertyGraphCypherQueryContext ctx, CypherAstBase expression) {
        return visitExpression(ctx, UUID.randomUUID().toString(), expression);
    }

    @SuppressWarnings("unchecked")
    public ExecutionStepWithResultName visitExpression(PropertyGraphCypherQueryContext ctx, String resultName, CypherAstBase expression) {
        if (expression instanceof CypherVariable) {
            return visitVariable(ctx, resultName, (CypherVariable) expression);
        } else if (expression instanceof CypherLookup) {
            return visitLookup(ctx, resultName, (CypherLookup) expression);
        } else if (expression instanceof CypherLiteral) {
            return visitLiteral(ctx, resultName, (CypherLiteral) expression);
        } else 
        throw new PropertyGraphCypherNotImplemented("expression: " + expression + " " + (expression == null ? "null" : expression.getClass().getName()));
    }

    @SuppressWarnings("unchecked")
    private ExecutionStepWithResultName visitLiteral(PropertyGraphCypherQueryContext ctx, String resultName, CypherLiteral expression) {
        Object value = expression.getValue();
        return new LiteralExecutionStep(resultName, value);
    }

    private LookupExecutionStep visitLookup(PropertyGraphCypherQueryContext ctx, String resultName, CypherLookup expression) {
        return new LookupExecutionStep(
            resultName,
            visitExpression(ctx, expression.getAtom()),
            expression.getProperty(),
            expression.getLabels()
        );
    }

    private ExecutionStepWithResultName visitVariable(PropertyGraphCypherQueryContext ctx, String resultName, CypherVariable expression) {
        return new GetVariableExecutionStep(resultName, expression.getName());
    }

    private MatchExecutionStep visitMatchClause(PropertyGraphCypherQueryContext ctx, CypherMatchClause clause) {
        return new MatchExecutionStep(
            clause.getPatternParts().stream()
                .map(patternPart -> visitPatternPart(ctx, clause.isOptional(), patternPart))
                .toArray(MatchPatternPartExecutionStep[]::new)
        );
    }

    @SuppressWarnings("unchecked")
    private MatchPatternPartExecutionStep visitPatternPart(PropertyGraphCypherQueryContext ctx, boolean optional, CypherPatternPart patternPart) {
        CypherListLiteral<CypherElementPattern> elementPatterns = patternPart.getElementPatterns();
        List<MatchPartExecutionStep> steps = elementPatterns.stream()
            .map(elementPattern -> {
                String resultName = elementPattern.getName();
                if (resultName == null) {
                    resultName = UUID.randomUUID().toString();
                }
                return createMatchPartExecutionStep(ctx, resultName, optional, elementPattern);
            })
            .collect(Collectors.toList());

        MatchPartExecutionStep[] matchPartExecutionSteps = new MatchPartExecutionStep[steps.size()];
        for (int i = 0; i < steps.size(); i++) {
            MatchPartExecutionStep step = steps.get(i);
            // if (i > 0) {
            //     step.addConnectedStep(steps.get(i - 1));
            // }
            // if (i < steps.size() - 1) {
            //     step.addConnectedStep(steps.get(i + 1));
            // }
            matchPartExecutionSteps[i] = step;
        }
        return new MatchPatternPartExecutionStep(
            patternPart.getName(),
            matchPartExecutionSteps
        );
    }

    private MatchPartExecutionStep createMatchPartExecutionStep(
        PropertyGraphCypherQueryContext ctx,
        String resultName,
        boolean optional,
        CypherElementPattern elementPattern
    ) {
        if (elementPattern instanceof CypherNodePattern) {
            CypherNodePattern nodePattern = (CypherNodePattern) elementPattern;
            return new MatchNodePartExecutionStep(
                elementPattern.getName(),  // vertex name
                resultName,  // usually same as vertex name
                optional,
                nodePattern.getLabelNames().stream().map(CypherLiteral::getValue).collect(Collectors.toSet()),
                visitPropertyMap(ctx, nodePattern.getPropertiesMap())
            );
        } else if (elementPattern instanceof CypherRelationshipPattern) {
            CypherRelationshipPattern relPattern = (CypherRelationshipPattern) elementPattern;
            return new MatchRelationshipPartExecutionStep(
                elementPattern.getName(),
                resultName,
                optional,
                relPattern.getRelTypeNames() == null
                    ? new ArrayList<>()
                    : relPattern.getRelTypeNames().stream().map(CypherLiteral::getValue).collect(Collectors.toList()),
                relPattern.getDirection(),
                relPattern.getRange(),
                visitPropertyMap(ctx, relPattern.getPropertiesMap())
            );
        } else {
            throw new PropertyGraphCypherException("Expected a node or relationship pattern found " + elementPattern.getClass().getName());
        }
    }

    private JoinExecutionStep visitCreateClause(PropertyGraphCypherQueryContext ctx, CypherCreateClause clause) {
        JoinExecutionStep executionPlan = new JoinExecutionStep(true);
        for (CypherPatternPart patternPart : clause.getPatternParts()) {
            executionPlan.addChildStep(visitCreateClausePatternPart(ctx, patternPart, null));
        }
        return executionPlan;
    }

    private CreatePatternExecutionStep visitCreateClausePatternPart(
        PropertyGraphCypherQueryContext ctx,
        CypherPatternPart patternPart,
        List<CypherMergeAction> mergeActions
    ) {
        CypherListLiteral<CypherElementPattern> elementPatterns = patternPart.getElementPatterns();
        CreateElementPatternExecutionStep[] steps = new CreateElementPatternExecutionStep[elementPatterns.size()];
        List<CreateNodePatternExecutionStep> createNodeExecutionSteps = new ArrayList<>();
        List<CreateRelationshipPatternExecutionStep> createRelationshipExecutionSteps = new ArrayList<>();

        for (int i = 0; i < elementPatterns.size(); i++) {
            CypherElementPattern elementPattern = elementPatterns.get(i);
            if (elementPattern instanceof CypherNodePattern) {
                CreateNodePatternExecutionStep step = visitCreateNodePattern(ctx, (CypherNodePattern) elementPattern, mergeActions);
                createNodeExecutionSteps.add(step);
                steps[i] = step;
            } else if (elementPattern instanceof CypherRelationshipPattern) {
                // OK
            } else {
                throw new PropertyGraphCypherNotImplemented("Unhandled create pattern type: " + elementPattern.getClass().getName());
            }
        }

        for (int i = 0; i < elementPatterns.size(); i++) {
            CypherElementPattern elementPattern = elementPatterns.get(i);
            if (elementPattern instanceof CypherRelationshipPattern) {
                String left = steps[i - 1].getResultName();
                String right = steps[i + 1].getResultName();
                CreateRelationshipPatternExecutionStep step = visitCreateRelationshipPattern(
                    ctx,
                    (CypherRelationshipPattern) elementPattern,
                    left,
                    right,
                    mergeActions
                );
                steps[i] = step;
                createRelationshipExecutionSteps.add(step);
            }
        }

        return new CreatePatternExecutionStep(
            patternPart.getName(),
            createNodeExecutionSteps,
            createRelationshipExecutionSteps
        );
    }

    private CreateRelationshipPatternExecutionStep visitCreateRelationshipPattern(
        PropertyGraphCypherQueryContext ctx,
        CypherRelationshipPattern p,
        String leftNodeName,
        String rightNodeName,
        List<CypherMergeAction> mergeActions
    ) {
        return new CreateRelationshipPatternExecutionStep(
            p.getName() == null ? UUID.randomUUID().toString() : p.getName(),
            p.getRelTypeNames() == null
                ? new ArrayList<>()
                : p.getRelTypeNames().stream().map(CypherLiteral::getValue).collect(Collectors.toList()),
            p.getDirection(),
            leftNodeName,
            rightNodeName,
            visitPropertyMap(ctx, p.getPropertiesMap()),
            visitMergeActions(ctx, mergeActions)
        );
    }

    private CreateNodePatternExecutionStep visitCreateNodePattern(
        PropertyGraphCypherQueryContext ctx,
        CypherNodePattern p,
        List<CypherMergeAction> mergeActions
    ) {
        return new CreateNodePatternExecutionStep(
            p.getName() == null ? UUID.randomUUID().toString() : p.getName(),
            p.getLabelNames().stream().map(CypherLiteral::getValue).collect(Collectors.toList()),
            visitPropertyMap(ctx, p.getPropertiesMap()),
            visitMergeActions(ctx, mergeActions)
        );
    }

    private List<ExecutionStep> visitMergeActions(PropertyGraphCypherQueryContext ctx, List<CypherMergeAction> mergeActions) {
        // to do 
        return null;
    }

    private List<ExecutionStepWithResultName> visitPropertyMap(PropertyGraphCypherQueryContext ctx, CypherMapLiteral<String, CypherAstBase> propertiesMap) {
        List<ExecutionStepWithResultName> results = new ArrayList<>();
        for (Map.Entry<String, CypherAstBase> propertyEntry : propertiesMap.entrySet()) {
            ExecutionStepWithResultName propertyStep = visitExpression(ctx, propertyEntry.getKey(), propertyEntry.getValue());
            results.add(propertyStep);
        }
        return results;
    }
}
