public record Token(TokenType type, String text, int line, int col) {}
public enum TokenType { IDENT, NUMBER, STRING, KEYWORD, OP, NEWLINE, EOF, COLON }
