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
            tokens.add(nextToken());
        }
        return tokens;
    }

    private Token nextToken() {
        char c = src.charAt(pos);
        if (SYMBOLS.indexOf(c) >= 0) {
            pos++;
            return new Token(TokenType.SYMBOL, String.valueOf(c));
        }
        if (c == '"') {
            int end = src.indexOf('"', pos + 1);
            if (end < 0) {
                throw new IllegalStateException("Unterminated string constant at position " + pos);
            }
            String value = src.substring(pos + 1, end);
            pos = end + 1;
            return new Token(TokenType.STRING_CONSTANT, value);
        }
        if (Character.isDigit(c)) {
            int start = pos;
            while (pos < src.length() && Character.isDigit(src.charAt(pos))) {
                pos++;
            }
            return new Token(TokenType.INTEGER_CONSTANT, src.substring(start, pos));
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
        throw new IllegalStateException("Unexpected character '" + c + "' at position " + pos);
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
                pos = end < 0 ? src.length() : end + 2;
            } else {
                break;
            }
        }
    }
}
