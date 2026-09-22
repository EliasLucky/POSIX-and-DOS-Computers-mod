package com.eliaslucky.mc_dos.blocks.computer.basic;

import java.util.List;

public interface Statement {
    int line();
    void execute(ExecutionContext ctx, Host host);
    default boolean isTerminal() { return false; }
}

// PRINT
record PrintStmt(int line, List<Expression> exprs, List<Character> separators, boolean trailingSuppress) implements Statement {

    @Override public int line() { return line; }

    @Override
    public void execute(ExecutionContext ctx, Host host) {
        if (exprs.isEmpty()) { host.printNewline(); ctx.setPrintColumn(0); return; }

        int column = ctx.getPrintColumn();
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < exprs.size(); i++) {
            if (i > 0) {
                char sep = separators.get(i - 1);
                if (sep == ',') {
                    int target = ((column / 14) + 1) * 14;
                    while (column < target) { buf.append(' '); column++; }
                }
                // ';' concatenates with no space
            }
            String s = exprs.get(i).eval(ctx, host).toPrintString();
            buf.append(s);
            column += s.length();
        }

        host.print(buf.toString());
        ctx.setPrintColumn(column);

        if (!trailingSuppress) {
            host.printNewline();
            ctx.setPrintColumn(0);
        }
    }
}

// Assignment  [LET] var = expr
record AssignStmt(int line, String name, Expression value) implements Statement {
    @Override public int line() { return line; }
    @Override public void execute(ExecutionContext ctx, Host host) {
        ctx.setVar(name, value.eval(ctx, host));
    }
}

// IF cond THEN ... [ELSE ...]  (single-line form)
record IfStmt(int line, Expression cond, List<Statement> thenBody, List<Statement> elseBody) implements Statement {
    @Override public int line() { return line; }
    @Override public void execute(ExecutionContext ctx, Host host) {
        boolean truthy = cond.eval(ctx, host).asNumber() != 0;
        for (Statement s : (truthy ? thenBody : elseBody)) s.execute(ctx, host);
    }
}

// FOR / NEXT
record ForStmt(int line, String var, Expression from, Expression to, Expression step) implements Statement {

    @Override public int line() { return line; }

    @Override
    public void execute(ExecutionContext ctx, Host host) {
        double start = from.eval(ctx, host).asNumber();
        double end   = to.eval(ctx, host).asNumber();
        double stepV = (step == null) ? 1.0 : step.eval(ctx, host).asNumber();
        if (stepV == 0) stepV = 1;

        ctx.setVar(var, Value.of(start));
        ctx.pushFor(new ExecutionContext.ForFrame(var, end, stepV, ctx.getPc() + 1));
    }
}

record NextStmt(int line, String var) implements Statement {
    @Override public int line() { return line; }
    @Override
    public void execute(ExecutionContext ctx, Host host) {
        ExecutionContext.ForFrame frame = ctx.peekFor();
        if (frame == null) {
            host.runtimeError(1, "NEXT without FOR", line);
            ctx.stop();
            return;
        }
        if (var != null && !var.equalsIgnoreCase(frame.var())) {
            host.runtimeError(1, "NEXT variable mismatch", line);
            ctx.stop();
            return;
        }

        double cur = ctx.getVar(frame.var()).asNumber() + frame.step();
        ctx.setVar(frame.var(), Value.of(cur));

        boolean done = frame.step() > 0 ? cur > frame.end() : cur < frame.end();
        if (done) ctx.popFor();
        else ctx.jumpTo(frame.bodyStartPc());
    }
}

// GOTO / GOSUB / RETURN
record GotoStmt(int line, String label) implements Statement {
    @Override public int line() { return line; }
    @Override
    public void execute(ExecutionContext ctx, Host host) {
        Integer target = ctx.labelToPc(label);
        if (target == null) {
            host.runtimeError(3, "Label not found: " + label, line);
            ctx.stop();
            return;
        }
        ctx.jumpTo(target);
    }
}

record GosubStmt(int line, String label) implements Statement {
    @Override public int line() { return line; }
    @Override
    public void execute(ExecutionContext ctx, Host host) {
        Integer target = ctx.labelToPc(label);
        if (target == null) {
            host.runtimeError(3, "Label not found: " + label, line);
            ctx.stop();
            return;
        }
        ctx.pushReturn(ctx.getPc() + 1);
        ctx.jumpTo(target);
    }
}

record ReturnStmt(int line) implements Statement {
    @Override public int line() { return line; }
    @Override
    public void execute(ExecutionContext ctx, Host host) {
        Integer ret = ctx.popReturn();
        if (ret == null) {
            host.runtimeError(3, "RETURN without GOSUB", line);
            ctx.stop();
            return;
        }
        ctx.jumpTo(ret);
    }
}

// Simple statements
record ClsStmt(int line) implements Statement {
    @Override public int line() { return line; }
    @Override public void execute(ExecutionContext ctx, Host host) { host.cls(); }
}

record RemStmt(int line) implements Statement {
    @Override public int line() { return line; }
    @Override public void execute(ExecutionContext ctx, Host host) { /* nothing */ }
}

record EndStmt(int line) implements Statement {
    @Override public int line() { return line; }
    @Override public void execute(ExecutionContext ctx, Host host) { ctx.stop(); }
    @Override public boolean isTerminal() { return true; }
}

record BeepStmt(int line) implements Statement {
    @Override public int line() { return line; }
    @Override public void execute(ExecutionContext ctx, Host host) { host.beep(); }
}

record SleepStmt(int line, Expression seconds) implements Statement {
    @Override public int line() { return line; }
    @Override
    public void execute(ExecutionContext ctx, Host host) {
        int ms = (int)(seconds.eval(ctx, host).asNumber() * 1000);
        if (ms > 0) host.sleep(ms);
    }
}

record LocateStmt(int line, Expression row, Expression col) implements Statement {
    @Override public int line() { return line; }
    @Override
    public void execute(ExecutionContext ctx, Host host) {
        host.locate((int) row.eval(ctx, host).asNumber(),
                    (int) col.eval(ctx, host).asNumber());
    }
}

record ColorStmt(int line, Expression fg, Expression bg) implements Statement {
    @Override public int line() { return line; }
    @Override
    public void execute(ExecutionContext ctx, Host host) {
        int f = (int) fg.eval(ctx, host).asNumber();
        int b = (bg == null) ? 0 : (int) bg.eval(ctx, host).asNumber();
        host.color(f, b);
    }
}

record ScreenStmt(int line, Expression mode) implements Statement {
    @Override public int line() { return line; }
    @Override
    public void execute(ExecutionContext ctx, Host host) {
        host.setScreenMode((int) mode.eval(ctx, host).asNumber());
    }
}

record PsetStmt(int line, Expression x, Expression y, Expression color) implements Statement {
    @Override public int line() { return line; }
    @Override
    public void execute(ExecutionContext ctx, Host host) {
        int c = (color == null) ? 15 : (int) color.eval(ctx, host).asNumber();
        host.pset((int) x.eval(ctx, host).asNumber(),
                  (int) y.eval(ctx, host).asNumber(), c);
    }
}
