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

    // ---- token cursor helpers: peek(), advance(), match(type), consume(type) ----

    /** The current (lookahead) token, or null at the end of the input. Does not consume it. */
    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
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

    /** True if the current token has the given type (does not consume it). */
    private boolean match(TokenType type) {
        Token t = peek();
        return t != null && t.type() == type;
    }

    /** True if the current token has the given type and one of the given values (does not consume it). */
    private boolean match(TokenType type, String... values) {
        if (!match(type)) {
            return false;
        }
        for (String v : values) {
            if (peek().value().equals(v)) {
                return true;
            }
        }
        return false;
    }

    /** Consumes the current token if it has the given type; otherwise reports a syntax error. */
    private Token consume(TokenType type) {
        if (!match(type)) {
            throw error(type.xmlTag());
        }
        return advance();
    }

    /** Consumes the current token if it has the given type and one of the values; otherwise a syntax error. */
    private Token consume(TokenType type, String... values) {
        if (!match(type, values)) {
            throw error(String.join("' ou '", values));
        }
        return advance();
    }

    private boolean isSymbol(String s) {
        return match(TokenType.SYMBOL, s);
    }

    private boolean isKeyword(String... keywords) {
        return match(TokenType.KEYWORD, keywords);
    }

    private void eatSymbol(String s) {
        consume(TokenType.SYMBOL, s);
    }

    private void eatKeyword(String... allowed) {
        consume(TokenType.KEYWORD, allowed);
    }

    private void eatIdentifier() {
        consume(TokenType.IDENTIFIER);
    }

    /** type: int | char | boolean | className */
    private void eatType() {
        if (isKeyword("int", "char", "boolean")) {
            advance();
        } else if (match(TokenType.IDENTIFIER)) {
            advance();
        } else {
            throw error("int' ou 'char' ou 'boolean' ou 'identifier");
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

    /**
     * expression: term (op term)*
     * Jack has no operator precedence: all binary operators have the same priority and are
     * applied left to right (left associativity); parentheses are the only way to change the
     * order. That is exactly the flat structure of the official parse trees, so a single loop
     * is enough - no expression/term/factor hierarchy is needed.
     */
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
        return match(TokenType.SYMBOL) && OPS.contains(peek().value());
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
            case KEYWORD -> consume(TokenType.KEYWORD, "true", "false", "null", "this");
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

    /** Builds the error in the course format: Linha N, coluna C: Esperado 'X' encontrado 'Y'. */
    private IllegalStateException error(String expected) {
        Token t = peek();
        Token at = t != null ? t : (tokens.isEmpty() ? null : tokens.get(tokens.size() - 1));
        String where = at == null ? "Linha 0" : "Linha " + at.line() + ", coluna " + at.column();
        String found = t == null ? "fim do arquivo" : t.value();
        return new IllegalStateException(where + ": Esperado '" + expected + "' encontrado '" + found + "'");
    }
}
