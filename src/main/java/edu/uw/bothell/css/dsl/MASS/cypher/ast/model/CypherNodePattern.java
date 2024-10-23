package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

import java.util.stream.Stream;

public class CypherNodePattern extends CypherElementPattern {
    private final CypherListLiteral<CypherLabelName> labelNames;

    public CypherNodePattern(
        String name,
        CypherMapLiteral<String, CypherAstBase> properties,
        CypherListLiteral<CypherLabelName> labelNames
    ) {
        super(name, properties);
        this.labelNames = labelNames;
    }

    public CypherListLiteral<CypherLabelName> getLabelNames() {
        return labelNames;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("      Node Pattern: \n ");
        if (getName() != null) {
            result.append("Node GetName: ");
            result.append(getName());
            result.append(";");
        }
        if (getLabelNames() != null && getLabelNames().size() > 0) {
            result.append("Node GetLabelNames: ");
            result.append(getLabelNames());
            result.append(";");
        }
        if (getPropertiesMap() != null && getPropertiesMap().size() > 0) {
            result.append("Node GetPropertyMap: ");
            result.append(" ").append(getPropertiesMap());
        }
        result.append("\n");
        return result.toString();
    }

    @Override
    public Stream<? extends CypherAstBase> getChildren() {
        return Stream.concat(super.getChildren(), labelNames.stream());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        CypherNodePattern that = (CypherNodePattern) o;

        if (!(labelNames != null ? labelNames.equals(that.labelNames) : that.labelNames == null)) {
            return false;
        }

        return super.equals(o);

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (labelNames != null ? labelNames.hashCode() : 0);
        return result;
    }

    @Override
    public int getConstraintCount() {
        return super.getConstraintCount() + (getLabelNames() == null ? 0 : getLabelNames().size());
    }
}