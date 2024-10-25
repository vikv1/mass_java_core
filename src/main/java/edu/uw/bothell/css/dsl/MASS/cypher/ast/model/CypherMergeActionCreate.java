package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

public class CypherMergeActionCreate extends CypherMergeAction {
    public CypherMergeActionCreate(CypherSetClause set) {
        super(set);
    }

    @Override
    public String toString() {
        return "ON CREATE " + getSet();
    }
}
