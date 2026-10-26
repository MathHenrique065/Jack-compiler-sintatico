package jack;

/** A single lexical token: its category, its text (string constants without quotes) and source line. */
public record Token(TokenType type, String value, int line) {

    public Token(TokenType type, String value) {
        this(type, value, 0);
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
