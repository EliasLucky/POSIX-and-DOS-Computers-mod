package com.eliaslucky.mc_dos.blocks.computer.basic;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final List<Token> toks;
    private int pos = 0;

    public Parser(List<Token> tokens) { this.toks = tokens; }

    public List<Statement> parse() {
        List<Statement> out = new ArrayList<>();
        int autoLine = 1;

        while (!peek().is(Token.TokenType.EOF)) {
            skipNewlines();
            if (peek().is(Token.TokenType.EOF)) break;

            int lineNo = autoLine;
            if (peek().is(Token.TokenType.NUMBER)) {
                try { lineNo = (int) Double.parseDouble(peek().text()); } catch (Exception ignored) {}
                advance();
            }

            // Multiple statements on one line separated by ':'
            while (!peek().is(Token.TokenType.NEWLINE) && !peek().is(Token.TokenType.EOF)) {
                Statement s = parseStatement(lineNo);
                if (s != null) out.add(s);
                if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(":")) {
                    advance(); continue;
                }
                break;
            }
            autoLine = lineNo + 1;
        }
        return out;
    }

    private Statement parseStatement(int line) {
        Token t = peek();

        if (t.isKeyword("REM"))    { advance(); skipLine(); return new RemStmt(line); }
        if (t.isKeyword("PRINT"))  { advance(); return parsePrint(line); }
        if (t.isKeyword("LET"))    { advance(); return parseAssign(line); }
        if (t.isKeyword("IF"))     { advance(); return parseIf(line); }
        if (t.isKeyword("FOR"))    { advance(); return parseFor(line); }
        if (t.isKeyword("NEXT"))   { advance(); return parseNext(line); }
        if (t.isKeyword("GOTO"))   { advance(); return parseGoto(line); }
        if (t.isKeyword("GOSUB"))  { advance(); return parseGosub(line); }
        if (t.isKeyword("RETURN")) { advance(); skipLine(); return new ReturnStmt(line); }
        if (t.isKeyword("END"))    { advance(); skipLine(); return new EndStmt(line); }
        if (t.isKeyword("CLS"))    { advance(); skipLine(); return new ClsStmt(line); }
        if (t.isKeyword("BEEP"))   { advance(); skipLine(); return new BeepStmt(line); }
        if (t.isKeyword("SLEEP"))  { advance(); return parseSleep(line); }
        if (t.isKeyword("LOCATE")) { advance(); return parseLocate(line); }
        if (t.isKeyword("COLOR"))  { advance(); return parseColor(line); }
        if (t.isKeyword("SCREEN")) { advance(); return parseScreen(line); }
        if (t.isKeyword("PSET"))   { advance(); return parsePset(line); }

        if (t.is(Token.TokenType.IDENT)) return parseAssign(line);

        advance();
        return null;
    }

    // PRINT
    private Statement parsePrint(int line) {
        List<Expression>  exprs = new ArrayList<>();
        List<Character>   seps  = new ArrayList<>();
        boolean trailingSuppress = false;

        while (!atEndOfStatement()) {
            exprs.add(parseExpr());

            if (peek().is(Token.TokenType.PUNCT)) {
                String p = peek().text();
                if (p.equals(";") || p.equals(",")) {
                    advance();
                    seps.add(p.charAt(0));
                    if (atEndOfStatement()) { trailingSuppress = true; break; }
                    continue;
                }
            }
            break;
        }
        return new PrintStmt(line, exprs, seps, trailingSuppress);
    }

    // Assignment
    private Statement parseAssign(int line) {
        Token name = expect(Token.TokenType.IDENT, "variable");
        expectOp("=");
        Expression value = parseExpr();
        return new AssignStmt(line, name.text(), value);
    }

    // IF
    private Statement parseIf(int line) {
        Expression cond = parseExpr();
        expectKeyword("THEN");

        List<Statement> thenBody = new ArrayList<>();
        while (!atEndOfStatement() && !peek().isKeyword("ELSE")) {
            Statement s = parseStatement(line);
            if (s != null) thenBody.add(s);
            if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(":")) { advance(); continue; }
            else break;
        }

        List<Statement> elseBody = new ArrayList<>();
        if (peek().isKeyword("ELSE")) {
            advance();
            while (!atEndOfStatement()) {
                Statement s = parseStatement(line);
                if (s != null) elseBody.add(s);
                if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(":")) { advance(); continue; }
                else break;
            }
        }
        return new IfStmt(line, cond, thenBody, elseBody);
    }

    // FOR / NEXT
    private Statement parseFor(int line) {
        Token var = expect(Token.TokenType.IDENT, "loop variable");
        expectOp("=");
        Expression from = parseExpr();
        expectKeyword("TO");
        Expression to = parseExpr();
        Expression step = null;
        if (peek().isKeyword("STEP")) { advance(); step = parseExpr(); }
        return new ForStmt(line, var.text(), from, to, step);
    }

    private Statement parseNext(int line) {
        String var = null;
        if (peek().is(Token.TokenType.IDENT)) { var = peek().text(); advance(); }
        return new NextStmt(line, var);
    }

    // GOTO / GOSUB
    private Statement parseGoto(int line) {
        Token label = expect(Token.TokenType.NUMBER, "line number");
        return new GotoStmt(line, label.text());
    }
    private Statement parseGosub(int line) {
        Token label = expect(Token.TokenType.NUMBER, "line number");
        return new GosubStmt(line, label.text());
    }

    // Extras
    private Statement parseSleep(int line) {
        return new SleepStmt(line, parseExpr());
    }
    private Statement parseLocate(int line) {
        Expression row = parseExpr();
        expectPunct(",");
        Expression col = parseExpr();
        return new LocateStmt(line, row, col);
    }
    private Statement parseColor(int line) {
        Expression fg = parseExpr();
        Expression bg = null;
        if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(",")) {
            advance(); bg = parseExpr();
        }
        return new ColorStmt(line, fg, bg);
    }
    private Statement parseScreen(int line) {
        return new ScreenStmt(line, parseExpr());
    }
    private Statement parsePset(int line) {
        expectPunct("(");
        Expression x = parseExpr();
        expectPunct(",");
        Expression y = parseExpr();
        Expression color = null;
        if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(",")) {
            advance(); color = parseExpr();
        }
        expectPunct(")");
        return new PsetStmt(line, x, y, color);
    }

    // Expression parsing (precedence climbing)
    private Expression parseExpr() { return parseOr(); }

    private Expression parseOr() {
        Expression l = parseAnd();
        while (peek().isKeyword("OR")) { advance(); l = new BinaryOp("OR", l, parseAnd()); }
        return l;
    }
    private Expression parseAnd() {
        Expression l = parseNot();
        while (peek().isKeyword("AND")) { advance(); l = new BinaryOp("AND", l, parseNot()); }
        return l;
    }
    private Expression parseNot() {
        if (peek().isKeyword("NOT")) { advance(); return new UnaryOp('!', parseNot()); }
        return parseRelational();
    }
    private Expression parseRelational() {
        Expression l = parseAdditive();
        while (peek().is(Token.TokenType.OP)) {
            String op = peek().text();
            if (op.equals("=") || op.equals("<") || op.equals(">")
                    || op.equals("<=") || op.equals(">=") || op.equals("<>")) {
                advance();
                l = new BinaryOp(op, l, parseAdditive());
            } else break;
        }
        return l;
    }
    private Expression parseAdditive() {
        Expression l = parseMultiplicative();
        while (peek().is(Token.TokenType.OP)) {
            String op = peek().text();
            if (op.equals("+") || op.equals("-")) {
                advance();
                l = new BinaryOp(op, l, parseMultiplicative());
            } else break;
        }
        return l;
    }
    private Expression parseMultiplicative() {
        Expression l = parseUnary();
        while (true) {
            if (peek().is(Token.TokenType.OP)) {
                String op = peek().text();
                if (op.equals("*") || op.equals("/") || op.equals("\\") || op.equals("^")) {
                    advance();
                    l = new BinaryOp(op, l, parseUnary());
                    continue;
                }
            }
            if (peek().isKeyword("MOD")) {
                advance();
                l = new BinaryOp("MOD", l, parseUnary());
                continue;
            }
            break;
        }
        return l;
    }
    private Expression parseUnary() {
        if (peek().is(Token.TokenType.OP) && peek().text().equals("-")) {
            advance();
            return new UnaryOp('-', parseUnary());
        }
        return parsePrimary();
    }
    private Expression parsePrimary() {
        Token t = peek();
        if (t.is(Token.TokenType.NUMBER)) {
            advance(); return new NumberLiteral(Double.parseDouble(t.text()));
        }
        if (t.is(Token.TokenType.STRING)) {
            advance(); return new StringLiteral(t.text());
        }
        if (t.is(Token.TokenType.IDENT)) {
            advance(); return new VariableRef(t.text());
        }
        if (t.is(Token.TokenType.PUNCT) && t.text().equals("(")) {
            advance();
            Expression inner = parseExpr();
            expectPunct(")");
            return inner;
        }
        
        advance();
        return new NumberLiteral(0);
    }

    // Token
    private Token peek()   { return toks.get(Math.min(pos, toks.size() - 1)); }
    private void advance() { if (pos < toks.size() - 1) pos++; }

    private boolean atEndOfStatement() {
        return peek().is(Token.TokenType.NEWLINE) || peek().is(Token.TokenType.EOF)
            || (peek().is(Token.TokenType.PUNCT) && peek().text().equals(":"));
    }

    private Token expect(Token.TokenType t, String what) {
        if (peek().is(t)) { Token r = peek(); advance(); return r; }
        throw new QBasicRuntimeException(1, peek().line(),
                "Parse error: expected " + what + " but got " + peek().text());
    }
    private void expectOp(String op) {
        if (peek().is(Token.TokenType.OP) && peek().text().equals(op)) { advance(); return; }
        throw new QBasicRuntimeException(1, peek().line(), "Parse error: expected '" + op + "'");
    }
    private void expectPunct(String p) {
        if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(p)) { advance(); return; }
        throw new QBasicRuntimeException(1, peek().line(), "Parse error: expected '" + p + "'");
    }
    private void expectKeyword(String kw) {
        if (peek().isKeyword(kw)) { advance(); return; }
        throw new QBasicRuntimeException(1, peek().line(), "Parse error: expected " + kw);
    }
    private void skipNewlines() { while (peek().is(Token.TokenType.NEWLINE)) advance(); }
    private void skipLine() {
        while (!peek().is(Token.TokenType.NEWLINE) && !peek().is(Token.TokenType.EOF)) advance();
        if (peek().is(Token.TokenType.NEWLINE)) advance();
    }
}
