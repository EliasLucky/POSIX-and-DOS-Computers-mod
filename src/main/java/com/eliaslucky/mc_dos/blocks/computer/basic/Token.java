package com.eliaslucky.mc_dos.blocks.computer.basic;

public record Token(TokenType type, String text, int line, int col) {
    public enum TokenType { IDENT, NUMBER, STRING, KEYWORD, OP, PUNCT, NEWLINE, EOF }

    public boolean is(TokenType t) { return type == t; }
    public boolean isKeyword(String kw) {
        return type == TokenType.KEYWORD && text.equalsIgnoreCase(kw);
    }
    public boolean isOp(String op) {
        return type == TokenType.OP && text.equals(op);
    }
}
