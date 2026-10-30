package jack;

import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for Jack. Consumes the token list and produces the
 * nand2tetris-style parse tree as indented XML lines.
 */
public class Parser {
    private final List<Token> tokens;
    private final List<String> out = new ArrayList<>();
    private int pos = 0;
    private int indent = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /** Convenience constructor: scans the source text and prepares the parser. */
    public Parser(String source) {
        this(new Tokenizer(source).tokenize());
    }

    /** Entry point: parses a whole class (the start symbol of the Jack grammar). */
    public void parse() {
        parseClass();
        if (peek() != null) {
            throw error("fim do arquivo");
        }
    }

    /** The XML lines produced so far. */
    public List<String> getOutput() {
        return out;
    }

    /** The parse tree as a single XML string. */
    public String XMLOutput() {
        return String.join("\n", out) + "\n";
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
            throw error("um token");
        }
        emit(t.toXml());
        pos++;
        return t;
    }

    private void eatSymbol(String s) {
        if (!isSymbol(s)) {
            throw error(s);
        }
        advance();
    }

    private void eatKeyword(String... allowed) {
        if (!isKeyword(allowed)) {
            throw error(String.join("' ou '", allowed));
        }
        advance();
    }

    private void eatIdentifier() {
        Token t = peek();
        if (t == null || t.type() != TokenType.IDENTIFIER) {
            throw error("identificador");
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
    public void parseExpression() {
        open("expression");
        parseTerm();
        while (isOp()) {
            advance();
            parseTerm();
        }
        close("expression");
    }

    private boolean isOp() {
        Token t = peek();
        return t != null && t.type() == TokenType.SYMBOL && OPS.contains(t.value());
    }

    /** term: constant | varName | varName[expr] | subroutineCall | (expr) | unaryOp term */
    public void parseTerm() {
        Token t = peek();
        if (t == null) {
            throw error("term");
        }
        open("term");
        switch (t.type()) {
            case INTEGER_CONSTANT, STRING_CONSTANT -> advance();
            case KEYWORD -> {
                if (!isKeyword("true", "false", "null", "this")) {
                    throw error("true' ou 'false' ou 'null' ou 'this");
                }
                advance();
            }
            case IDENTIFIER -> {
                advance(); // identificador comum a todas as alternativas
                if (isSymbol("(") || isSymbol(".")) {
                    parseSubroutineCall();
                } else if (isSymbol("[")) {
                    advance();
                    parseExpression();
                    eatSymbol("]");
                }
                // nenhum dos dois: varName simples
            }
            case SYMBOL -> {
                if (isSymbol("(")) {
                    advance();
                    parseExpression();
                    eatSymbol(")");
                } else if (isSymbol("-") || isSymbol("~")) {
                    advance();
                    parseTerm();
                } else {
                    throw error("term");
                }
            }
        }
        close("term");
    }

    /** expressionList: (expression (',' expression)*)? */
    public void parseExpressionList() {
        open("expressionList");
        if (!isSymbol(")")) {
            parseExpression();
            while (isSymbol(",")) {
                advance();
                parseExpression();
            }
        }
        close("expressionList");
    }

    // ---- statements ----

    /** statements: statement* */
    public void parseStatements() {
        open("statements");
        while (isKeyword("let", "if", "while", "do", "return")) {
            switch (peek().value()) {
                case "let" -> parseLet();
                case "if" -> parseIf();
                case "while" -> parseWhile();
                case "do" -> parseDo();
                default -> parseReturn();
            }
        }
        close("statements");
    }

    /** let varName ('[' expression ']')? '=' expression ';' */
    public void parseLet() {
        open("letStatement");
        eatKeyword("let");
        eatIdentifier();
        if (isSymbol("[")) {
            advance();
            parseExpression();
            eatSymbol("]");
        }
        eatSymbol("=");
        parseExpression();
        eatSymbol(";");
        close("letStatement");
    }

    /** if '(' expression ')' '{' statements '}' (else '{' statements '}')? */
    public void parseIf() {
        open("ifStatement");
        eatKeyword("if");
        eatSymbol("(");
        parseExpression();
        eatSymbol(")");
        eatSymbol("{");
        parseStatements();
        eatSymbol("}");
        if (isKeyword("else")) {
            advance();
            eatSymbol("{");
            parseStatements();
            eatSymbol("}");
        }
        close("ifStatement");
    }

    /** while '(' expression ')' '{' statements '}' */
    public void parseWhile() {
        open("whileStatement");
        eatKeyword("while");
        eatSymbol("(");
        parseExpression();
        eatSymbol(")");
        eatSymbol("{");
        parseStatements();
        eatSymbol("}");
        close("whileStatement");
    }

    /**
     * subroutineName '(' expressionList ')' | (className | varName) '.' subroutineName '(' expressionList ')'
     * Auxiliary rule without its own XML tag. Precondition: the leading identifier was already consumed.
     */
    public void parseSubroutineCall() {
        if (isSymbol("(")) {
            advance();
            parseExpressionList();
            eatSymbol(")");
        } else {
            eatSymbol(".");
            eatIdentifier();
            eatSymbol("(");
            parseExpressionList();
            eatSymbol(")");
        }
    }

    /** do subroutineCall ';' */
    public void parseDo() {
        open("doStatement");
        eatKeyword("do");
        eatIdentifier();
        parseSubroutineCall();
        eatSymbol(";");
        close("doStatement");
    }

    /** return expression? ';' */
    public void parseReturn() {
        open("returnStatement");
        eatKeyword("return");
        if (!isSymbol(";")) {
            parseExpression();
        }
        eatSymbol(";");
        close("returnStatement");
    }

    // ---- subroutines ----

    /** parameterList: ((type varName) (',' type varName)*)? */
    public void parseParameterList() {
        open("parameterList");
        if (!isSymbol(")")) {
            eatType();
            eatIdentifier();
            while (isSymbol(",")) {
                advance();
                eatType();
                eatIdentifier();
            }
        }
        close("parameterList");
    }

    /** var type varName (',' varName)* ';' */
    public void parseVarDec() {
        open("varDec");
        eatKeyword("var");
        eatType();
        eatIdentifier();
        while (isSymbol(",")) {
            advance();
            eatIdentifier();
        }
        eatSymbol(";");
        close("varDec");
    }

    /** subroutineBody: '{' varDec* statements '}' */
    public void parseSubroutineBody() {
        open("subroutineBody");
        eatSymbol("{");
        while (isKeyword("var")) {
            parseVarDec();
        }
        parseStatements();
        eatSymbol("}");
        close("subroutineBody");
    }

    /** (constructor | function | method) (void | type) subroutineName '(' parameterList ')' subroutineBody */
    public void parseSubroutineDec() {
        open("subroutineDec");
        eatKeyword("constructor", "function", "method");
        if (isKeyword("void")) {
            advance();
        } else {
            eatType();
        }
        eatIdentifier();
        eatSymbol("(");
        parseParameterList();
        eatSymbol(")");
        parseSubroutineBody();
        close("subroutineDec");
    }

    // ---- class ----

    /** (static | field) type varName (',' varName)* ';' */
    public void parseClassVarDec() {
        open("classVarDec");
        eatKeyword("static", "field");
        eatType();
        eatIdentifier();
        while (isSymbol(",")) {
            advance();
            eatIdentifier();
        }
        eatSymbol(";");
        close("classVarDec");
    }

    /** class className '{' classVarDec* subroutineDec* '}'  -- entry point of the parser. */
    public void parseClass() {
        open("class");
        eatKeyword("class");
        eatIdentifier();
        eatSymbol("{");
        while (isKeyword("static", "field")) {
            parseClassVarDec();
        }
        while (isKeyword("constructor", "function", "method")) {
            parseSubroutineDec();
        }
        eatSymbol("}");
        close("class");
    }

    /** Builds the error in the format required by the course: Linha N: Esperado 'X' encontrado 'Y'. */
    private IllegalStateException error(String expected) {
        Token t = peek();
        int line = t != null ? t.line() : (tokens.isEmpty() ? 0 : tokens.get(tokens.size() - 1).line());
        String found = t == null ? "fim do arquivo" : t.value();
        return new IllegalStateException("Linha " + line + ": Esperado '" + expected + "' encontrado '" + found + "'");
    }
}
