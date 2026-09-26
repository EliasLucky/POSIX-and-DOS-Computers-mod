package com.eliaslucky.mc_dos.blocks.computer.basic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Parser {
    private final List<Token> toks;
    private int pos = 0;
    private final Deque<DoStmt> doStack = new ArrayDeque<>();

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

            parseOneLine(lineNo, out);
            autoLine = lineNo + 1;
        }
        return out;
    }
    
    /**
     * Parse all statements on one logical line, adding them to `out`.
     * If the line begins with DO, the whole DO ... LOOP is consumed
     * here and appended to `out` as flat statements.
     */
    private void parseOneLine(int lineNo, List<Statement> out) {
        while (!peek().is(Token.TokenType.NEWLINE) && !peek().is(Token.TokenType.EOF)) {

            if (peek().isKeyword("DO")) {
                parseBlockDo(lineNo, out);
                return;    // parseBlockDo consumes through LOOP
            }

            if (peek().isKeyword("EXIT")) {
                advance();
                if (peek().isKeyword("DO")) {
                    advance();
                    if (doStack.isEmpty()) {
                        throw new QBasicRuntimeException(1, lineNo, "EXIT DO without DO");
                    }
                    out.add(new ExitDoStmt(lineNo, doStack.peek().exit()));
                    continue;
                }
                // EXIT FOR (later), EXIT SUB, etc.
                throw new QBasicRuntimeException(1, lineNo, "EXIT used with unknown target");
            }

            Statement s = parseStatement(lineNo);
            if (s != null) out.add(s);

            if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(":")) {
                advance();
                continue;
            }
            break;
        }
        // Consume trailing tokens we didn't understand.
        while (!peek().is(Token.TokenType.NEWLINE) && !peek().is(Token.TokenType.EOF)) advance();
        if (peek().is(Token.TokenType.NEWLINE)) advance();
    }

    private Statement parseStatement(int line) {
        Token t = peek();

        if (t.isKeyword("REM"))    { advance(); skipLine(); return new RemStmt(line); }
        if (t.isKeyword("PRINT"))  { advance(); return parsePrint(line); }
        if (t.isKeyword("INPUT"))  { advance(); return parseInput(line); }
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
        if (t.isKeyword("LINE"))   { advance(); return parseLine(line); }
        if (t.isKeyword("CIRCLE")) { advance(); return parseCircle(line); }
        if (t.isKeyword("RANDOMIZE")) { advance(); return parseRandomize(line); }
        if (t.isKeyword("SELECT")) { advance(); return parseSelect(line); }
        if (t.isKeyword("VIEW"))   { advance(); return parseView(line); }
        if (t.isKeyword("WAIT"))   { advance(); skipLine(); return new WaitStmt(line); }
        if (t.isKeyword("WIDTH"))  { advance(); skipLine(); return new WidthStmt(line); }
        if (t.isKeyword("DEF"))    { advance(); return parseDefFn(line); }
        if (t.isKeyword("SUB"))    { advance(); return parseSubDef(line); }
        if (t.isKeyword("CALL"))   { advance(); return parseCall(line); }

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
    
    private Statement parseInput(int line) {
        // Two forms:
        //   INPUT [;] ["prompt" {;|,}] var [, var]...
        //   INPUT promptVar; var [, var]...
        // We support the first form and the bare form. The prompt-variable form
        // is rare in beginner code and can be added later.

        // Optional leading semicolon (suppresses the "? " auto-prompt).
        boolean skipQuestionMark = false;
        if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(";")) {
            advance();
            skipQuestionMark = true;
        }

        // Optional string-literal prompt.
        String prompt = skipQuestionMark ? "" : "? ";
        if (peek().is(Token.TokenType.STRING)) {
            prompt = peek().text();
            advance();
            // Separator between prompt and variables: ; or ,
            if (peek().is(Token.TokenType.PUNCT)
                    && (peek().text().equals(";") || peek().text().equals(","))) {
                advance();
            }
        }

        // Comma-separated variable list.
        List<String> targets = new ArrayList<>();
        while (true) {
            Token var = expect(Token.TokenType.IDENT, "variable name");
            targets.add(var.text());
            if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(",")) {
                advance();
                continue;
            }
            break;
        }

        return new InputStmt(line, prompt, targets);
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

        if (!atEndOfStatement()) {
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
	                break;
	            }
	        }
	        return new IfStmt(line, cond, thenBody, elseBody);
        }
        skipLine();
        return parseBlockIf(line, cond);
    }
    
    private Statement parseBlockIf(int line, Expression cond) {
        List<Statement> thenBody = new ArrayList<>();
        List<Statement> elseBody = new ArrayList<>();
        List<Statement> current  = thenBody;

        while (true) {
            skipNewlines();
            if (peek().is(Token.TokenType.EOF)) throw new QBasicRuntimeException(1, line, "IF without END IF");

            if (peek().isKeyword("ENDIF")) {
                advance();
                break;
            }
            
            // END IF
            if (peek().isKeyword("END")) {
                advance();
                if (peek().isKeyword("IF")) { advance(); break; }
                // plain END inside the block
                current.add(new EndStmt(line));
                continue;
            }

            // ELSE switches the active branch
            if (peek().isKeyword("ELSE")) {
                advance();
                current = elseBody;
                continue;
            }

            // Optional line number
            int subLine = line;
            if (peek().is(Token.TokenType.NUMBER)) {
                try { subLine = (int) Double.parseDouble(peek().text()); } catch (Exception ignored) {}
                advance();
            }

            Statement s = parseStatement(subLine);
            if (s != null) current.add(s);
            // Consume to end of line if the statement parser didn't.
            while (!peek().is(Token.TokenType.NEWLINE)
                   && !peek().is(Token.TokenType.EOF)
                   && !peek().isKeyword("ELSE")
                   && !peek().isKeyword("END")
                   && !peek().isKeyword("ENDIF")) {
                advance();
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
    	Expression mode = null, color = null, apage = null, vpage = null;

        if (!atEndOfStatement() && !peekIsPunct(",")) {
            mode = parseExpr();
        }
        if (peekIsPunct(",")) {
            advance();
            if (!atEndOfStatement() && !peekIsPunct(",")) color = parseExpr();
        }
        if (peekIsPunct(",")) {
            advance();
            if (!atEndOfStatement() && !peekIsPunct(",")) apage = parseExpr();
        }
        if (peekIsPunct(",")) {
            advance();
            if (!atEndOfStatement()) vpage = parseExpr();
        }
        skipLine();
        return new ScreenStmt(line, mode, color, apage, vpage);
    }
    private boolean peekIsPunct(String s) {
        return peek().is(Token.TokenType.PUNCT) && peek().text().equals(s);
    }
    private Statement parsePset(int line) {
        expectPunct("(");
        Expression x = parseExpr();
        expectPunct(",");
        Expression y = parseExpr();
        expectPunct(")");
        Expression color = null;
        if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(",")) {
            advance(); color = parseExpr();
        }
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
        if (t.isKeyword("TRUE"))  { advance(); return new NumberLiteral(-1); }
        if (t.isKeyword("FALSE")) { advance(); return new NumberLiteral(0);  }
        if (t.is(Token.TokenType.NUMBER)) {
            advance();
            String s = t.text();
            if (s.startsWith("0x")) {
                return new NumberLiteral((double) Long.parseLong(s.substring(2), 16));
            }
            return new NumberLiteral(Double.parseDouble(s));
        }
        if (t.is(Token.TokenType.STRING)) {
            advance(); return new StringLiteral(t.text());
        }
        if (t.is(Token.TokenType.IDENT)) {
        	String name = t.text();
            advance();

            // Function call:  NAME ( arg, arg, ... )
            if (peek().is(Token.TokenType.PUNCT) && peek().text().equals("(")) {
                advance();
                List<Expression> args = new ArrayList<>();
                if (!(peek().is(Token.TokenType.PUNCT) && peek().text().equals(")"))) {
                    args.add(parseExpr());
                    while (peek().is(Token.TokenType.PUNCT) && peek().text().equals(",")) {
                        advance();
                        args.add(parseExpr());
                    }
                }
                expectPunct(")");
                return new FunctionCall(name, args);
            }

            // Known zero-argument built-ins that don't require parentheses.
            if (name.equalsIgnoreCase("INKEY$") || name.equalsIgnoreCase("INKEY")) {
                return new FunctionCall(name, java.util.List.of());
            }
            return new VariableRef(name);
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
    
    private Statement parseLine(int line) {
        // LINE (x1,y1)-(x2,y2) [, color [, B | BF]]
        if (!(peek().is(Token.TokenType.PUNCT) && peek().text().equals("("))) {
            throw new QBasicRuntimeException(1, line, "LINE requires coordinates");
        }
        advance();
        Expression x1 = parseExpr();
        expectPunct(",");
        Expression y1 = parseExpr();
        expectPunct(")");

        expectOp("-");
        expectPunct("(");
        Expression x2 = parseExpr();
        expectPunct(",");
        Expression y2 = parseExpr();
        expectPunct(")");

        Expression color = null;
        boolean box = false, filled = false;

        if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(",")) {
            advance();
            color = parseExpr();

            if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(",")) {
                advance();
                // B or BF — comes as an IDENT
                if (peek().is(Token.TokenType.IDENT)) {
                    String mode = peek().text().toUpperCase();
                    advance();
                    if (mode.equals("B"))       { box = true; }
                    else if (mode.equals("BF")) { box = true; filled = true; }
                }
            }
        }
        return new LineStmt(line, x1, y1, x2, y2, color, box, filled);
    }

    private Statement parseCircle(int line) {
        expectPunct("(");
        Expression x = parseExpr();
        expectPunct(",");
        Expression y = parseExpr();
        expectPunct(")");
        expectPunct(",");
        Expression r = parseExpr();

        Expression color = null;
        if (peek().is(Token.TokenType.PUNCT) && peek().text().equals(",")) {
            advance();
            color = parseExpr();
            // Ignore start/end/aspect args for now.
        }
        return new CircleStmt(line, x, y, r, color);
    }

    private Statement parseRandomize(int line) {
        // RANDOMIZE [expr]
        Expression seed = null;
        if (!atEndOfStatement()) seed = parseExpr();
        return new RandomizeStmt(line, seed);
    }
    
    /**
     * Parse DO [WHILE|UNTIL expr] ... LOOP [WHILE|UNTIL expr]
     * Appends DoStmt, body, LoopStmt to `out` as flat statements.
     */
    private void parseBlockDo(int lineNo, List<Statement> out) {
        advance();   // consume DO

        Expression topCondition = null;
        boolean topUntil = false;
        boolean checkAtTop = false;

        if (peek().isKeyword("WHILE")) { advance(); topCondition = parseExpr(); checkAtTop = true; }
        else if (peek().isKeyword("UNTIL")) { advance(); topCondition = parseExpr(); checkAtTop = true; topUntil = true; }

        skipLine();

        JumpTarget exitTarget = new JumpTarget();
        DoStmt doStmt = new DoStmt(lineNo, topCondition, topUntil, checkAtTop, exitTarget);

        int doIndex = out.size();
        out.add(doStmt);
        doStack.push(doStmt);

        // Body: parse until LOOP (at the same nesting level).
        while (true) {
            skipNewlines();
            if (peek().is(Token.TokenType.EOF)) {
                throw new QBasicRuntimeException(1, lineNo, "DO without LOOP");
            }
            if (peek().isKeyword("LOOP")) {
                advance();
                break;
            }

            int subLine = lineNo;
            if (peek().is(Token.TokenType.NUMBER)) {
                try { subLine = (int) Double.parseDouble(peek().text()); }
                catch (Exception ignored) {}
                advance();
            }
            parseOneLine(subLine, out);
        }

        // Optional LOOP WHILE / LOOP UNTIL.
        Expression bottomCondition = null;
        boolean bottomUntil = false;
        if (peek().isKeyword("WHILE")) { advance(); bottomCondition = parseExpr(); }
        else if (peek().isKeyword("UNTIL")) { advance(); bottomCondition = parseExpr(); bottomUntil = true; }

        skipLine();

        JumpTarget bodyStart = new JumpTarget();
        bodyStart.pc = doIndex + 1;

        LoopStmt loopStmt = new LoopStmt(lineNo, bottomCondition, bottomUntil, checkAtTop, bodyStart);
        int loopIndex = out.size();
        out.add(loopStmt);

        exitTarget.pc = loopIndex + 1;

        doStack.pop();
    }
    private Statement parseSelect(int line) {
        expectKeyword("CASE");
        Expression subject = parseExpr();
        skipLine();

        List<SelectStmt.CaseClause> clauses = new ArrayList<>();
        while (true) {
            skipNewlines();
            if (peek().is(Token.TokenType.EOF)) {
                throw new QBasicRuntimeException(1, line, "SELECT without END SELECT");
            }
            if (peek().isKeyword("END")) {
                advance();
                if (peek().isKeyword("SELECT")) { advance(); break; }
                break;
            }
            if (!peek().isKeyword("CASE")) {
                // Unexpected token;
                advance(); continue;
            }
            advance();  // CASE

            List<Expression> values = new ArrayList<>();
            while (true) {
                values.add(parseExpr());
                if (peekIsPunct(",")) { advance(); continue; }
                break;
            }
            skipLine();

            List<Statement> body = new ArrayList<>();
            while (true) {
                skipNewlines();
                if (peek().is(Token.TokenType.EOF)) break;
                if (peek().isKeyword("CASE")) break;
                if (peek().isKeyword("END")) {
                    advance();
                    if (peek().isKeyword("SELECT")) { advance(); }
                    clauses.add(new SelectStmt.CaseClause(values, body));
                    return new SelectStmt(line, subject, clauses);
                }
                int subLine = line;
                if (peek().is(Token.TokenType.NUMBER)) {
                    try { subLine = (int) Double.parseDouble(peek().text()); } catch (Exception ignored) {}
                    advance();
                }
                parseOneLine(subLine, body);
            }
            clauses.add(new SelectStmt.CaseClause(values, body));
        }
        return new SelectStmt(line, subject, clauses);
    }
    
    private Statement parseView(int line) {
        // VIEW [SCREEN] (x1,y1)-(x2,y2) [, fill [, border]]
        // VIEW   resets to fullscreen
        if (atEndOfStatement()) {
            return new ViewStmt(line, null, null, null, null, null, null, true);
        }

        if (peek().isKeyword("SCREEN")) advance();

        expectPunct("(");
        Expression x1 = parseExpr();
        expectPunct(",");
        Expression y1 = parseExpr();
        expectPunct(")");
        expectOp("-");
        expectPunct("(");
        Expression x2 = parseExpr();
        expectPunct(",");
        Expression y2 = parseExpr();
        expectPunct(")");

        Expression fill = null, border = null;
        if (peekIsPunct(",")) {
            advance();
            fill = parseExpr();
            if (peekIsPunct(",")) {
                advance();
                border = parseExpr();
            }
        }
        skipLine();
        return new ViewStmt(line, x1, y1, x2, y2, fill, border, false);
    }
    private Statement parseDefFn(int line) {
        // DEF FNname(a, b, ...) = expression
        Token nameTok = expect(Token.TokenType.IDENT, "function name");
        expectPunct("(");
        List<String> params = new ArrayList<>();
        if (!peekIsPunct(")")) {
            params.add(expect(Token.TokenType.IDENT, "parameter").text());
            while (peekIsPunct(",")) { advance(); params.add(expect(Token.TokenType.IDENT, "parameter").text()); }
        }
        expectPunct(")");
        expectOp("=");
        Expression body = parseExpr();
        skipLine();
        return new DefFnStmt(line, nameTok.text(), params, body);
    }

    private Statement parseSubDef(int line) {
        Token nameTok = expect(Token.TokenType.IDENT, "sub name");
        expectPunct("(");
        List<String> params = new ArrayList<>();
        if (!peekIsPunct(")")) {
            params.add(expect(Token.TokenType.IDENT, "parameter").text());
            while (peekIsPunct(",")) { advance(); params.add(expect(Token.TokenType.IDENT, "parameter").text()); }
        }
        expectPunct(")");
        skipLine();

        List<Statement> body = new ArrayList<>();
        while (true) {
            skipNewlines();
            if (peek().is(Token.TokenType.EOF)) {
                throw new QBasicRuntimeException(1, line, "SUB without END SUB");
            }
            if (peek().isKeyword("END")) {
                advance();
                if (peek().isKeyword("SUB")) { advance(); break; }
            }
            int subLine = line;
            if (peek().is(Token.TokenType.NUMBER)) {
                try { subLine = (int) Double.parseDouble(peek().text()); } catch (Exception ignored) {}
                advance();
            }
            parseOneLine(subLine, body);
        }
        return new SubDefStmt(line, nameTok.text(), params, body);
    }

    private Statement parseCall(int line) {
        Token nameTok = expect(Token.TokenType.IDENT, "sub name");
        expectPunct("(");
        List<Expression> args = new ArrayList<>();
        if (!peekIsPunct(")")) {
            args.add(parseExpr());
            while (peekIsPunct(",")) { advance(); args.add(parseExpr()); }
        }
        expectPunct(")");
        skipLine();
        return new CallStmt(line, nameTok.text(), args);
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
