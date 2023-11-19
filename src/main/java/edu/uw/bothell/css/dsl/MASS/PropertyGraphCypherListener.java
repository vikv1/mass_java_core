package edu.uw.bothell.css.dsl.MASS;

import edu.uw.bothell.css.dsl.MASS.antlr.CypherBaseListener;
import edu.uw.bothell.css.dsl.MASS.antlr.CypherParser;

public class PropertyGraphCypherListener extends CypherBaseListener {
    private PropertyGraphPlaces graphPlaces;

    public PropertyGraphCypherListener(PropertyGraphPlaces graphPlaces) {
        this.graphPlaces = graphPlaces;
    }

     @Override
    public void enterOC_RelationshipPattern(CypherParser.OC_RelationshipPatternContext ctx) {
        
    }

    @Override
    public void enterOC_Query(CypherParser.OC_QueryContext ctx) {
        
            
        }

    @Override
    public void enterOC_Match(CypherParser.OC_MatchContext ctx) {
        
    }
}