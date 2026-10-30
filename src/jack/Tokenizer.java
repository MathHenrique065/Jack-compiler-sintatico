package jack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Breaks Jack source code into tokens, skipping whitespace and comments. */
public class Tokenizer {
    private static final Set<String> KEYWORDS = Set.of(
            "class", "constructor", "function", "method", "field", "static", "var",
            "int", "char", "boolean", "void", "true", "false", "null", "this",
            "let", "do", "if", "else", "while", "return");

    private static final String SYMBOLS = "{}()[].,;+-*/&|<>=~";

    private final String src;
    private int pos = 0;
    private int line = 1;
    private int linePos = 0;

    public Tokenizer(String source) {
        this.src = source;
    }

    public static Tokenizer fromFile(Path file) throws IOException {
        return new Tokenizer(Files.readString(file));
    }

    /** Tokenizes the whole input. */
    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (true) {
            skipWhitespaceAndComments();
            if (pos >= src.length()) {
                break;
            }
            tokens.add(withLine(nextToken()));
        }
        return tokens;
    }

    /** Stamps the token with the line where it starts (counted lazily from the last known position). */
    private Token withLine(Token t) {
        return new Token(t.type(), t.value(), line);
    }

    /** Line number (1-based) of an arbitrary position in the source. */
    private int lineAt(int position) {
        int n = 1;
        for (int i = 0; i < position && i < src.length(); i++) {
            if (src.charAt(i) == '\n') {
                n++;
            }
        }
        return n;
    }

    private IllegalStateException error(int position, String message) {
        return new IllegalStateException("Linha " + lineAt(position) + ": " + message);
    }

    private Token nextToken() {
        for (; linePos < pos; linePos++) {
            if (src.charAt(linePos) == '\n') {
                line++;
            }
        }
        char c = src.charAt(pos);
        if (SYMBOLS.indexOf(c) >= 0) {
            pos++;
            return new Token(TokenType.SYMBOL, String.valueOf(c));
        }
        if (c == '"') {
            int end = src.indexOf('"', pos + 1);
            if (end < 0) {
                throw error(pos, "constante string sem aspas de fechamento");
            }
            String value = src.substring(pos + 1, end);
            if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                throw error(pos, "constante string nao pode conter quebra de linha");
            }
            pos = end + 1;
            return new Token(TokenType.STRING_CONSTANT, value);
        }
        if (Character.isDigit(c)) {
            int start = pos;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
            String digits = src.substring(start, pos);
            if (digits.length() > 5 || Integer.parseInt(digits) > 32767) {
                throw error(start, "constante inteira fora do intervalo 0..32767: " + digits);
            }
            return new Token(TokenType.INTEGER_CONSTANT, digits);
        }
        if (Character.isLetter(c) || c == '_') {
            int start = pos;
            while (pos < src.length()
                    && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) {
                pos++;
            }
            String word = src.substring(start, pos);
            return new Token(KEYWORDS.contains(word) ? TokenType.KEYWORD : TokenType.IDENTIFIER, word);
        }
        throw error(pos, "caractere inesperado '" + c + "'");
    }

    private void skipWhitespaceAndComments() {
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isWhitespace(c)) {
                pos++;
            } else if (src.startsWith("//", pos)) {
                int end = src.indexOf('\n', pos);
                pos = end < 0 ? src.length() : end + 1;
            } else if (src.startsWith("/*", pos)) {
                int end = src.indexOf("*/", pos + 2);
                if (end < 0) {
                    throw error(pos, "comentario '/*' sem fechamento '*/'");
                }
                pos = end + 2;
            } else {
                break;
            }
        }
    }
}
