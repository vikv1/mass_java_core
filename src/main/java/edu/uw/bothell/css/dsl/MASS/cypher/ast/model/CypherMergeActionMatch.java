package edu.uw.bothell.css.dsl.MASS.cypher.ast.model;

public class CypherMergeActionMatch extends CypherMergeAction {
    public CypherMergeActionMatch(CypherSetClause set) {
        super(set);
    }

    @Override
    public String toString() {
        return "ON MATCH " + getSet();
    }
}
