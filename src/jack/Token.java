package jack;

/**
 * A single lexical token: its category, its text (string constants without quotes)
 * and the source position (line and column, both 1-based) where it starts.
 */
public record Token(TokenType type, String value, int line, int column) {

    public Token(TokenType type, String value) {
        this(type, value, 0, 0);
    }

    /** Renders the token as a line of the nand2tetris token XML (escaping &lt; &gt; &amp; and quotes). */
    public String toXml() {
        String escaped = value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
        return "<" + type.xmlTag() + "> " + escaped + " </" + type.xmlTag() + ">";
    }
}
