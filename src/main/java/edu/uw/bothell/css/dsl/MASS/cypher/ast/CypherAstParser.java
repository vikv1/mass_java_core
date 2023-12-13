package edu.uw.bothell.css.dsl.MASS.cypher.ast;

import org.antlr.v4.runtime.*;
import edu.uw.bothell.css.dsl.MASS.antlr.CypherLexer;
import edu.uw.bothell.css.dsl.MASS.antlr.CypherParser;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherAstBase;
import edu.uw.bothell.css.dsl.MASS.cypher.ast.model.CypherStatement;
import edu.uw.bothell.css.dsl.MASS.cypher.exceptions.PropertyGraphCypherSyntaxErrorException;

public class CypherAstParser {
    private static final CypherAstParser instance = new CypherAstParser();

    public static CypherAstParser getInstance() {
        return instance;
    }

    public CypherStatement parse(CypherCompilerContext ctx, String code) {
        // create a CharStream that reads from standard input
        CodePointCharStream input = CharStreams.fromString(code);
        // create lexer
        CypherLexer lexer = new CypherLexer(input);
        // create a buffer of tokens pulled from the lexer and create a parser that feeds off the tokens buffer
        CypherParser parser = new CypherParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new ParserErrorHandler(code));
        CypherParser.OC_CypherContext tree = parser.oC_Cypher();// begin parsing at oC_Cypher
        String treeText = tree.getText();

        if (treeText.endsWith("<EOF>")) {
            treeText = treeText.substring(0, treeText.length() - "<EOF>".length());
        }
        if (!treeText.equals(code)) {
            throw new PropertyGraphCypherSyntaxErrorException("Parsing error, \"" + code.substring(treeText.length()) + "\"");
        }
        return new PropertyGraphCypherVisitor(ctx).visitOC_Cypher(tree);
    }

    public CypherAstBase parseExpression(String expressionString) {
        CypherLexer lexer = new CypherLexer(CharStreams.fromString(expressionString));
        CypherParser parser = new CypherParser(new CommonTokenStream(lexer));
        parser.setErrorHandler(new ParserErrorHandler(expressionString));
        CypherParser.OC_ExpressionContext expressionContext = parser.oC_Expression();
        return new PropertyGraphCypherVisitor().visitOC_Expression(expressionContext);
    }

    private static class ParserErrorHandler extends BailErrorStrategy {
        private final String code;

        public ParserErrorHandler(String code) {
            this.code = code;
        }

        @Override
        public void reportError(Parser recognizer, RecognitionException e) {
            String messagePrefix = "";
            if (e.getCtx() instanceof CypherParser.OC_RelationshipPatternContext) {
                messagePrefix = "InvalidRelationshipPattern: ";
            }
            throw new PropertyGraphCypherSyntaxErrorException(
                String.format(
                    "%sCould not parse (%d:%d): %s",
                    messagePrefix,
                    e.getOffendingToken().getLine(),
                    e.getOffendingToken().getCharPositionInLine(),
                    code
                ),
                e);
        }
    }
}