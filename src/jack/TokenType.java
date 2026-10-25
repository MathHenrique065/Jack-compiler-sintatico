package jack;

/** Lexical categories of the Jack language, with their XML tag names. */
public enum TokenType {
    KEYWORD("keyword"),
    SYMBOL("symbol"),
    INTEGER_CONSTANT("integerConstant"),
    STRING_CONSTANT("stringConstant"),
    IDENTIFIER("identifier");

    private final String xmlTag;

    TokenType(String xmlTag) {
        this.xmlTag = xmlTag;
    }

    public String xmlTag() {
        return xmlTag;
    }
}
