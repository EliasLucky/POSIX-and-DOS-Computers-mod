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
	/** Maximum statements executed per tick call. */
    public static final int STEPS_PER_TICK = 500;
    /** prevents `10 GOTO 10` from freezing the game. */
    public static final int MAX_STEPS = 1_000_000;

    private final Host host;
    private ExecutionContext ctx;
    private int totalSteps;
    private boolean aborted;

    public QBasicInterpreter(Host host) { this.host = host; }
    
    public void start(String source) {
        this.aborted = false;
        this.totalSteps = 0;

        try {
            List<Token> tokens  = new Tokenizer(source).tokenize();
            List<Statement> prog = new Parser(tokens).parse();
            this.ctx = new ExecutionContext(prog);
        } catch (QBasicRuntimeException ex) {
            host.runtimeError(ex.code, ex.getMessage(), ex.sourceLine);
            this.ctx = null;
        } catch (RuntimeException ex) {
            host.runtimeError(1, "Parse error: " + ex.getMessage(), 0);
            this.ctx = null;
        }
    }

    /**
     * Execute up to {@code maxSteps} statements. Returns the state
     */
    public RunState tick(int maxSteps) {
        if (ctx == null) return RunState.FINISHED;

        int stepsThisTick = 0;

        try {
            while (ctx.hasNext() && stepsThisTick < maxSteps) {

                // SLEEP from a previous statement
                if (ctx.hasPendingSleep()) {
                    if (System.currentTimeMillis() < ctx.getSleepUntil()) {
                        return RunState.WAITING_SLEEP;
                    }
                    ctx.clearSleep();
                }

                if (++totalSteps > MAX_STEPS) {
                    host.runtimeError(7, "Program exceeded step limit", 0);
                    return finish();
                }

                Statement s = ctx.next();
                int before = ctx.getPc();
                s.execute(ctx, host);

                // INPUT just requested
                if (ctx.hasInputRequest()) {
                    return RunState.WAITING_INPUT;   // pc stays put
                }

                if (ctx.getPc() == before && !s.isTerminal()) ctx.advance();
                stepsThisTick++;
            }

            if (!ctx.hasNext()) return finish();
            return RunState.RUNNING;

        } catch (QBasicRuntimeException ex) {
            host.runtimeError(ex.code, ex.getMessage(), ex.sourceLine);
            return finish();
        } catch (RuntimeException ex) {
            host.runtimeError(1, "Runtime error: " + ex.getMessage(), 0);
            return finish();
        }
    }

    private RunState finish() {
        host.end();
        ctx = null;
        return RunState.FINISHED;
    }

    // Input from the UI
    public void provideInput(String line) {
        if (ctx != null) ctx.provideInput(line == null ? "" : line);
    }

    /** Parse and execute to completion (synchronous). */
    /*public void run(String source) {
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
    }*/
    
    /** Run for the immediate pane / tests */
    public void run(String source) {
        start(source);
        if (ctx == null) return;
        while (true) {
            RunState s = tick(1000);
            if (s == RunState.FINISHED) return;
            if (s == RunState.WAITING_INPUT) { provideInput(""); continue; }
            if (s == RunState.WAITING_SLEEP) { ctx.clearSleep(); continue; }
            if (s == RunState.WAITING_INPUT) {
                host.runtimeError(0, "INPUT is not available in the immediate window", 0);
                return;
            }
        }
    }

    public void stop() {
        if (ctx != null) {
            host.end();
            ctx = null;
            aborted = true;
        }
    }
    public boolean isRunning() { return ctx != null; }
}
