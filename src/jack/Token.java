package jack;

/** A single lexical token: its category and its text (string constants without quotes). */
public record Token(TokenType type, String value) {

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
