package jack;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for Jack. Consumes the token list and produces the
 * nand2tetris-style parse tree as indented XML lines.
 */
public class CompilationEngine {
    private final List<Token> tokens;
    private final List<String> out = new ArrayList<>();
    private int pos = 0;
    private int indent = 0;

    public CompilationEngine(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** The XML lines produced so far. */
    public List<String> getOutput() {
        return out;
    }

    // ---- token cursor helpers ----

    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }

    private boolean isSymbol(String s) {
        Token t = peek();
        return t != null && t.type() == TokenType.SYMBOL && t.value().equals(s);
    }

    private boolean isKeyword(String... keywords) {
        Token t = peek();
        if (t == null || t.type() != TokenType.KEYWORD) {
            return false;
        }
        for (String k : keywords) {
            if (t.value().equals(k)) {
                return true;
            }
        }
        return false;
    }

    /** Writes the current token to the output and moves to the next one. */
    private Token advance() {
        Token t = peek();
        if (t == null) {
            throw error("more tokens");
        }
        emit(t.toXml());
        pos++;
        return t;
    }

    private void eatSymbol(String s) {
        if (!isSymbol(s)) {
            throw error("'" + s + "'");
        }
        advance();
    }

    private void eatKeyword(String... allowed) {
        if (!isKeyword(allowed)) {
            throw error(String.join(" or ", allowed));
        }
        advance();
    }

    private void eatIdentifier() {
        Token t = peek();
        if (t == null || t.type() != TokenType.IDENTIFIER) {
            throw error("an identifier");
        }
        advance();
    }

    /** type: int | char | boolean | className */
    private void eatType() {
        if (isKeyword("int", "char", "boolean")) {
            advance();
        } else {
            eatIdentifier();
        }
    }

    // ---- XML output helpers ----

    private void open(String tag) {
        emit("<" + tag + ">");
        indent++;
    }

    private void close(String tag) {
        indent--;
        emit("</" + tag + ">");
    }

    private void emit(String line) {
        out.add("  ".repeat(indent) + line);
    }

    // ---- expressions ----

    private static final String OPS = "+-*/&|<>=";

    /** expression: term (op term)* */
    public void compileExpression() {
        open("expression");
        compileTerm();
        while (isOp()) {
            advance();
            compileTerm();
        }
        close("expression");
    }

    private boolean isOp() {
        Token t = peek();
        return t != null && t.type() == TokenType.SYMBOL && OPS.contains(t.value());
    }

    /** term: constant | varName | varName[expr] | subroutineCall | (expr) | unaryOp term */
    public void compileTerm() {
        Token t = peek();
        if (t == null) {
            throw error("a term");
        }
        open("term");
        switch (t.type()) {
            case INTEGER_CONSTANT, STRING_CONSTANT -> advance();
            case KEYWORD -> {
                if (!isKeyword("true", "false", "null", "this")) {
                    throw error("a keyword constant (true, false, null, this)");
                }
                advance();
            }
            case IDENTIFIER -> {
                advance();
                if (isSymbol("[")) {
                    advance();
                    compileExpression();
                    eatSymbol("]");
                } else if (isSymbol("(")) {
                    advance();
                    compileExpressionList();
                    eatSymbol(")");
                } else if (isSymbol(".")) {
                    advance();
                    eatIdentifier();
                    eatSymbol("(");
                    compileExpressionList();
                    eatSymbol(")");
                }
            }
            case SYMBOL -> {
                if (isSymbol("(")) {
                    advance();
                    compileExpression();
                    eatSymbol(")");
                } else if (isSymbol("-") || isSymbol("~")) {
                    advance();
                    compileTerm();
                } else {
                    throw error("a term");
                }
            }
        }
        close("term");
    }

    /** expressionList: (expression (',' expression)*)? */
    public void compileExpressionList() {
        open("expressionList");
        if (!isSymbol(")")) {
            compileExpression();
            while (isSymbol(",")) {
                advance();
                compileExpression();
            }
        }
        close("expressionList");
    }

    private IllegalStateException error(String expected) {
        Token t = peek();
        return new IllegalStateException("Syntax error: expected " + expected + " but found "
                + (t == null ? "end of input" : "'" + t.value() + "'") + " (token #" + pos + ")");
    }
}
