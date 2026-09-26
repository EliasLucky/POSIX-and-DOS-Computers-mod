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

record InputStmt(int line, String prompt, List<String> targets) implements Statement {
	@Override public int line() { return line; }

	@Override
	public void execute(ExecutionContext ctx, Host host) {
		// First execution: print prompt, request input, pc stays put.
		if (!ctx.hasInputValue()) {
			if (!prompt.isEmpty()) host.print(prompt);
			ctx.requestInput();
			return;
		}

		// Resumed: consume the supplied line and assign to each target.
		String raw = ctx.consumeInput();
		String[] parts = raw.split(",", -1);

		for (int i = 0; i < targets.size(); i++) {
			String name = targets.get(i);
			String val	= (i < parts.length) ? parts[i].trim() : "";

			if (name.toUpperCase().endsWith("$")) {
				ctx.setVar(name, Value.of(val));
			} else {
				try   { ctx.setVar(name, Value.of(Double.parseDouble(val))); }
				catch (NumberFormatException e) { ctx.setVar(name, Value.of(0)); }
			}
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

// IF cond THEN ... [ELSE ...]	(single-line form)
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
		double end	 = to.eval(ctx, host).asNumber();
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
		double secs = seconds.eval(ctx, host).asNumber();
		if (secs > 0) {
			int ms = (int)(secs * 1000);
			ctx.requestSleep(System.currentTimeMillis() + ms);
		}
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

record ScreenStmt(int line, Expression mode, Expression color, Expression apage, Expression vpage) implements Statement {
	@Override public int line() { return line; }
	@Override
	public void execute(ExecutionContext ctx, Host host) {
		if (mode != null) host.setScreenMode((int) mode.eval(ctx, host).asNumber());
		if (color != null) host.color((int) color.eval(ctx, host).asNumber(), 0);
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

// LINE (x1,y1)-(x2,y2) [, color [, B | BF]]
record LineStmt(int line, Expression x1, Expression y1, Expression x2, Expression y2, Expression color,  boolean box, boolean filledBox) implements Statement {
 	@Override public int line() { return line; }

 	@Override
	 	public void execute(ExecutionContext ctx, Host host) {
		 int ax = (int) x1.eval(ctx, host).asNumber();
		 int ay = (int) y1.eval(ctx, host).asNumber();
		 int bx = (int) x2.eval(ctx, host).asNumber();
		 int by = (int) y2.eval(ctx, host).asNumber();
		 int c	= color == null ? host.colorFg() : (int) color.eval(ctx, host).asNumber();
	
		 if (box) {
			 host.drawLine(ax, ay, bx, ay, c, 0);
			 host.drawLine(bx, ay, bx, by, c, 0);
			 host.drawLine(bx, by, ax, by, c, 0);
			 host.drawLine(ax, by, ax, ay, c, 0);
			 if (filledBox) {
				 int y0 = Math.min(ay, by);
				 int y1 = Math.max(ay, by);
				 int xl = Math.min(ax, bx);
				 int xr = Math.max(ax, bx);
				 for (int y = y0; y <= y1; y++) host.drawLine(xl, y, xr, y, c, 0);
			 }
		 } else {
			 host.drawLine(ax, ay, bx, by, c, 0);
		 }
	}
}

// CIRCLE (x,y), radius [, color]
record CircleStmt(int line, Expression x, Expression y, Expression radius, Expression color) implements Statement {
	@Override public int line() { return line; }

	@Override
 	public void execute(ExecutionContext ctx, Host host) {
	int cx = (int) x.eval(ctx, host).asNumber();
	int cy = (int) y.eval(ctx, host).asNumber();
	int r	= (int) radius.eval(ctx, host).asNumber();
	int c	= color == null ? host.colorFg() : (int) color.eval(ctx, host).asNumber();
	host.circle(cx, cy, r, c, false);
 	}
}

record RandomizeStmt(int line, Expression seed) implements Statement {
	@Override public int line() { return line; }
	@Override
	public void execute(ExecutionContext ctx, Host host) {
		long s = seed == null ? System.nanoTime()
							  : (long) seed.eval(ctx, host).asNumber();
		BuiltinFunctions.seed(s);
	}
}

// DO ... LOOP
record DoStmt(int line, Expression topCondition, boolean topUntil, boolean checkAtTop, JumpTarget exit) implements Statement {
 	@Override public int line() { return line; }

 	@Override
 	public void execute(ExecutionContext ctx, Host host) {
		 if (!checkAtTop) return;		// plain DO: just fall through
		 boolean cond = topCondition.eval(ctx, host).asNumber() != 0;
		 boolean shouldExit = topUntil ? cond : !cond;
		 if (shouldExit) ctx.jumpTo(exit.pc);
 	}
}

record LoopStmt(int line, Expression bottomCondition, boolean bottomUntil, boolean checkAtTop, JumpTarget bodyStart) implements Statement {
	@Override public int line() { return line; }

	@Override
	public void execute(ExecutionContext ctx, Host host) {
		 if (checkAtTop) {
			// DO WHILE / DO UNTIL: unconditional jump back; the top
			// statement re-evaluates and exits if needed.
			ctx.jumpTo(bodyStart.pc);
			return;
		 }
		 // LOOP WHILE / LOOP UNTIL — or bare LOOP (bottomCondition == null
		 // which means "always continue").
		 if (bottomCondition == null) {
			ctx.jumpTo(bodyStart.pc);
			return;
		 }
		 boolean cond = bottomCondition.eval(ctx, host).asNumber() != 0;
		 boolean shouldContinue = bottomUntil ? !cond : cond;
		 if (shouldContinue) ctx.jumpTo(bodyStart.pc);
	}
}

record ExitDoStmt(int line, JumpTarget target) implements Statement {
	@Override public int line() { return line; }
	@Override
	public void execute(ExecutionContext ctx, Host host) {
		 if (target.pc < 0) {
			// Should never happen — parser filled it in at LOOP.
			host.runtimeError(1, "EXIT DO with unresolved target", line);
			ctx.stop();
			return;
		 }
		 ctx.jumpTo(target.pc);
	}
}
// SELECT CASE
record SelectStmt(int line, Expression subject, List<CaseClause> clauses) implements Statement {
	public record CaseClause(List<Expression> values, List<Statement> body) {}
	
	@Override public int line() { return line; }
	
	@Override
	public void execute(ExecutionContext ctx, Host host) {
		Value subjectValue = subject.eval(ctx, host);		// ← renamed
		
		for (CaseClause clause : clauses) {
			for (Expression v : clause.values()) {
				Value cv = v.eval(ctx, host);
		
				boolean match = subjectValue.isString() == cv.isString()
							&& (subjectValue.isString()
							? subjectValue.asString().equals(cv.asString())
							: subjectValue.asNumber() == cv.asNumber());
		
				if (match) {
						for (Statement s : clause.body()) s.execute(ctx, host);
						return;
				}
			}
		}
	}
}

// VIEW
record ViewStmt(int line, Expression x1, Expression y1, Expression x2, Expression y2, Expression fill, Expression border, boolean reset) implements Statement {
	@Override public int line() { return line; }

	@Override
	public void execute(ExecutionContext ctx, Host host) {
		 if (reset) { host.resetViewport(); return; }
		 host.setViewport(
						 (int) x1.eval(ctx, host).asNumber(),
						 (int) y1.eval(ctx, host).asNumber(),
						 (int) x2.eval(ctx, host).asNumber(),
						 (int) y2.eval(ctx, host).asNumber(),
						 border == null ? -1 : (int) border.eval(ctx, host).asNumber());
	}
}

// WAIT — hardware sync, no-op in the mod
record WaitStmt(int line) implements Statement {
	@Override public int line() { return line; }
	@Override public void execute(ExecutionContext ctx, Host host) { /* no-op */ }
}

// WIDTH — for now, accept and ignore
record WidthStmt(int line) implements Statement {
	@Override public int line() { return line; }
	@Override public void execute(ExecutionContext ctx, Host host) { /* no-op */ }
}

// DEF FNname(params) = expr
record DefFnStmt(int line, String name, List<String> params, Expression body) implements Statement {
	@Override public int line() { return line; }
	@Override
	public void execute(ExecutionContext ctx, Host host) {
		ctx.defineFn(name, params, body);
	}
}

// SUB name(params) ... END SUB
//The body is not executed in place it's registered as a callable.
record SubDefStmt(int line, String name, List<String> params, List<Statement> body) implements Statement {
	@Override public int line() { return line; }
	@Override
	public void execute(ExecutionContext ctx, Host host) {
		ctx.defineSub(name, params, body);
	}
}

// CALL name(args)
record CallStmt(int line, String name, List<Expression> args) implements Statement {
	@Override public int line() { return line; }
	@Override
	public void execute(ExecutionContext ctx, Host host) {
		ctx.callSub(name, args, host);
	}
}
