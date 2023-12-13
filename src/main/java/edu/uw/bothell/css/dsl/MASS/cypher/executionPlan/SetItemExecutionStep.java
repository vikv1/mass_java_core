package edu.uw.bothell.css.dsl.MASS.cypher.executionPlan;

import edu.uw.bothell.css.dsl.MASS.cypher.CypherResultRow;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherQueryContext;
import edu.uw.bothell.css.dsl.MASS.cypher.PropertyGraphCypherResult;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherSetItem;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherException;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherNotImplemented;
import java.util.Map;

public class SetItemExecutionStep extends ExecutionStepWithChildren {
    private final String leftResultName;
    private final CypherSetItem.Op op;
    private final String rightResultName;

    public SetItemExecutionStep(ExecutionStepWithResultName left, CypherSetItem.Op op, ExecutionStepWithResultName right) {
        super(left, right);
        this.leftResultName = left.getResultName();
        this.op = op;
        this.rightResultName = right.getResultName();
    }

    @Override
    public PropertyGraphCypherResult execute(PropertyGraphCypherQueryContext ctx, PropertyGraphCypherResult source) {
        source = super.execute(ctx, source);
        return source.peek(row -> {
            Object left = row.get(leftResultName);
            Object right = row.get(rightResultName);
            switch (op) {
            //     case PLUS_EQUAL:
            //         executePlusEquals(ctx, row, left, right);
            //         break;
            //     case EQUAL:
            //         executeEquals(ctx, row, left, right);
            //         break;
                default:
                    throw new PropertyGraphCypherNotImplemented("unhandled op " + op);
            }
        });
    }

    // @SuppressWarnings("unchecked")
    // private void executeEquals(PropertyGraphCypherQueryContext ctx, CypherResultRow row, Object left, Object right) {
    //     Element leftElement = (Element) row.get(leftResultName + LookupExecutionStep.SCOPE_ELEMENT_SUFFIX);
    //     Property leftProperty = (Property) row.get(leftResultName + LookupExecutionStep.SCOPE_PROPERTY_SUFFIX);
    //     String leftPropertyName = (String) row.get(leftResultName + LookupExecutionStep.SCOPE_PROPERTY_NAME_SUFFIX);

    //     if (leftProperty != null) {
    //         if (leftElement == null) {
    //             throw new PropertyGraphCypherException("left was a property but could not get element");
    //         }
    //         if (right == null) {
    //             ctx.removeProperty(leftElement, leftProperty);
    //             return;
    //         }
    //     }

    //     if (leftPropertyName != null) {
    //         if (leftElement == null) {
    //             throw new PropertyGraphCypherException("left was a property but could not get element");
    //         }
    //         if (right == null) {
    //             ctx.removeProperty(leftElement, leftPropertyName);
    //             return;
    //         }
    //         ctx.setProperty(leftElement, leftPropertyName, right);
    //         return;
    //     }

    //     if (leftElement == null && left == null) {
    //         return;
    //     }

    //     if (left instanceof Element) {
    //         leftElement = (Element) left;

    //         if (right instanceof Map) {
    //             Map<String, ?> rightMap = (Map<String, ?>) right;
    //             for (Property property : leftElement.getProperties()) {
    //                 if (!rightMap.containsKey(property.getName()) && !ctx.isLabelProperty(property)) {
    //                     ctx.removeProperty(leftElement, property);
    //                 }
    //             }
    //             ExistingElementMutation<Element> m = leftElement.prepareMutation();
    //             for (Map.Entry<String, ?> entry : rightMap.entrySet()) {
    //                 String propertyName = entry.getKey();
    //                 Object value = entry.getValue();
    //                 ctx.setProperty(m, propertyName, value);
    //             }
    //             ctx.saveElement(m);
    //             return;
    //         }

    //         if (right instanceof Element) {
    //             Element rightElement = (Element) right;
    //             ExistingElementMutation<Element> m = leftElement.prepareMutation();
    //             for (Property property : rightElement.getProperties()) {
    //                 if (ctx.isLabelProperty(property)) {
    //                     continue;
    //                 }
    //                 ctx.setProperty(m, property.getName(), property.getValue());
    //             }
    //             ctx.saveElement(m);
    //             return;
    //         }
    //     }

    //     throw new PropertyGraphCypherNotImplemented(String.format("set {left: %s, right: %s}", left, right));
    // }

    // @SuppressWarnings("unchecked")
    // private void executePlusEquals(PropertyGraphCypherQueryContext ctx, CypherResultRow row, Object left, Object right) {
    //     if (left instanceof Element) {
    //         Element leftElement = (Element) left;
    //         if (right instanceof Iterable) {
    //             executeSetLabels(ctx, leftElement, (Iterable) right);
    //             return;
    //         } else if (right.getClass().isArray()) {
    //             executeSetLabels(ctx, leftElement, (Iterable) ObjectUtils.arrayToList(right));
    //             return;
    //         } else if (right instanceof Map) {
    //             executeSetProperties(ctx, leftElement, (Map<String, ?>) right);
    //             return;
    //         }
    //     }

    //     throw new PropertyGraphCypherNotImplemented(String.format("set {left: %s, right: %s}", left, right));
    // }

    // private void executeSetProperties(PropertyGraphCypherQueryContext ctx, Element element, Map<String, ?> map) {
    //     ExistingElementMutation<Element> m = element.prepareMutation();
    //     for (Map.Entry<String, ?> entry : map.entrySet()) {
    //         String propertyName = entry.getKey();
    //         Object value = entry.getValue();
    //         if (value == null) {
    //             ctx.removeProperty(element, propertyName);
    //         } else {
    //             ctx.setProperty(m, propertyName, value);
    //         }
    //     }
    //     ctx.saveElement(m);
    // }

    // private void executeSetLabels(PropertyGraphCypherQueryContext ctx, Element element, Iterable<String> labels) {
    //     ExistingElementMutation<Element> m = element.prepareMutation();
    //     for (String label : labels) {
    //         ctx.setLabelProperty(m, label);
    //     }
    //     ctx.saveElement(m);
    // }
}
