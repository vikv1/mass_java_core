package edu.uw.bothell.css.dsl.MASS.cypher.ast;

import java.util.HashMap;
import java.util.Map;

import edu.uw.bothell.css.dsl.MASS.cypher.functions.CypherFunction;

public class CypherCompilerContext {
    private final Map<String, CypherFunction> functions;

    public CypherCompilerContext() {
        this(new HashMap<>());
    }

    public CypherCompilerContext(Map<String, CypherFunction> functions) {
        this.functions = functions;
    }

    public CypherFunction getFunction(String functionName) {
        return functions.get(functionName.toLowerCase());
    }
}
