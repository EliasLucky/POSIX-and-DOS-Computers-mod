package com.eliaslucky.mc_dos.blocks.computer.basic;

import java.util.*;

public class Tokenizer {
    private static final Set<String> KEYWORDS = Set.of(
            "PRINT", "LET", "IF", "THEN", "ELSE", "END", "ENDIF",
            "FOR", "TO", "STEP", "NEXT", "GOTO", "GOSUB", "RETURN",
            "CLS", "REM", "INPUT", "SCREEN", "COLOR", "LOCATE",
            "PSET", "LINE", "CIRCLE", "STOP", "DATA", "READ",
            "AND", "OR", "NOT", "MOD", "DIM", "AS", "SLEEP", "BEEP",
            "TRUE", "FALSE", "RANDOMIZE", "DO", "LOOP", "WHILE", "UNTIL", "EXIT",
            "SELECT", "CASE", "IS", "VIEW", "WAIT", "WIDTH",
            "DEF", "SUB", "CALL", "INKEY"
    );

    private final String src;
    private int pos = 0, line = 1, col = 1;

    public Tokenizer(String src) { this.src = src; }

    public List<Token> tokenize() {
        List<Token> out = new ArrayList<>();
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '\r') { pos++; continue; }
            if (c == '\n') {
                out.add(new Token(Token.TokenType.NEWLINE, "\n", line, col));
                pos++; line++; col = 1; continue;
            }
            if (c == ' ' || c == '\t') { pos++; col++; continue; }
            if (c == '\'') { skipLine(); continue; }
            if (c == '`')  { skipLine(); continue; }

            if (Character.isDigit(c)
                    || (c == '.' && pos + 1 < src.length() && Character.isDigit(src.charAt(pos + 1)))
                || (c == '&' && pos + 1 < src.length() && (src.charAt(pos + 1) == 'H' || src.charAt(pos + 1) == 'h'))) {
                out.add(readNumber()); continue;
            }
            if (Character.isLetter(c) || c == '_') { out.add(readWord()); continue; }
            if (c == '"') { out.add(readString()); continue; }
            if (c == '?') {
                // `?` is shorthand for PRINT
                out.add(new Token(Token.TokenType.KEYWORD, "PRINT", line, col));
                pos++; col++; continue;
            }

            out.add(readOp());
        }
        out.add(new Token(Token.TokenType.EOF, "", line, col));
        return out;
    }

    private Token readNumber() {
        int start = pos, sc = col;
        // Hex: &H followed by hex digits
        if (src.charAt(pos) == '&' && pos + 1 < src.length()
                && (src.charAt(pos + 1) == 'H' || src.charAt(pos + 1) == 'h')) {
            pos += 2; col += 2;
            int hexStart = pos;
            while (pos < src.length() && isHexDigit(src.charAt(pos))) {
                pos++; col++;
            }
            return new Token(Token.TokenType.NUMBER,
                    src.substring(hexStart, pos), line, sc);
        }
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isDigit(c) || c == '.' || c == 'E' || c == 'e') { pos++; col++; }
            else break;
        }
        return new Token(Token.TokenType.NUMBER, src.substring(start, pos), line, sc);
    }
    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    private Token readWord() {
        int start = pos, sc = col;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') { pos++; col++; }
            else if (c == '$' || c == '%' || c == '!' || c == '#') { pos++; col++; break; }
            else break;
        }
        String word = src.substring(start, pos);
        Token.TokenType t = KEYWORDS.contains(word.toUpperCase(Locale.ROOT))
                ? Token.TokenType.KEYWORD : Token.TokenType.IDENT;
        return new Token(t, word, line, sc);
    }

    private Token readString() {
        int sc = col; pos++; col++;
        StringBuilder sb = new StringBuilder();
        while (pos < src.length() && src.charAt(pos) != '"') {
            sb.append(src.charAt(pos)); pos++; col++;
        }
        if (pos < src.length()) { pos++; col++; }
        return new Token(Token.TokenType.STRING, sb.toString(), line, sc);
    }

    private Token readOp() {
        int sc = col;
        if (pos + 1 < src.length()) {
            String two = src.substring(pos, pos + 2);
            if (two.equals("<=") || two.equals(">=") || two.equals("<>")) {
                pos += 2; col += 2;
                return new Token(Token.TokenType.OP, two, line, sc);
            }
        }
        char c = src.charAt(pos); pos++; col++;
        Token.TokenType t = (c == ':' || c == ',' || c == ';' || c == '(' || c == ')')
                ? Token.TokenType.PUNCT : Token.TokenType.OP;
        return new Token(t, String.valueOf(c), line, sc);
    }

    private void skipLine() {
        while (pos < src.length() && src.charAt(pos) != '\n') { pos++; col++; }
    }
}
