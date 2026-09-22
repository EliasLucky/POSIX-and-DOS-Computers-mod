package com.eliaslucky.mc_dos.blocks.computer.basic;

import java.util.List;

/**
 * QBASIC interpreter.
 *
 * Supported:
 *   PRINT with ; and , separators
 *   LET / bare assignment
 *   IF ... THEN ... [ELSE ...]       (single-line form)
 *   FOR / NEXT [STEP]
 *   GOTO / GOSUB / RETURN
 *   CLS, REM, END, BEEP, SLEEP
 *   SCREEN, COLOR, LOCATE, PSET      (forwarded to Host)
 *
 * Most of the calls goes through Host interface
 */
public class QBasicInterpreter {
    /** prevents `10 GOTO 10` from freezing the game. */
    public static final int MAX_STEPS = 1_000_000;

    private final Host host;
    private ExecutionContext ctx;
    private volatile boolean stopped = false;

    public QBasicInterpreter(Host host) { this.host = host; }

    /** Parse and execute to completion (synchronous). */
    public void run(String source) {
        this.stopped = false;

        List<Token> tokens;
        List<Statement> program;
        try {
            tokens  = new Tokenizer(source).tokenize();
            program = new Parser(tokens).parse();
        } catch (QBasicRuntimeException ex) {
            host.runtimeError(ex.code, ex.getMessage(), ex.sourceLine);
            return;
        } catch (RuntimeException ex) {
            host.runtimeError(1, "Parse error: " + ex.getMessage(), 0);
            return;
        }

        this.ctx = new ExecutionContext(program);

        int steps = 0;
        try {
            while (ctx.hasNext() && !stopped) {
                if (++steps > MAX_STEPS) {
                    host.runtimeError(7, "Program exceeded step limit", 0);
                    break;
                }
                Statement s = ctx.next();
                int before = ctx.getPc();
                s.execute(ctx, host);
                if (ctx.getPc() == before && !s.isTerminal()) ctx.advance();
            }
        } catch (QBasicRuntimeException ex) {
            host.runtimeError(ex.code, ex.getMessage(), ex.sourceLine);
        } catch (RuntimeException ex) {
            host.runtimeError(1, "Runtime error: " + ex.getMessage(), 0);
        } finally {
            host.end();
            this.ctx = null;
        }
    }

    public void stop() { this.stopped = true; }
    public boolean isRunning() { return ctx != null; }
}
