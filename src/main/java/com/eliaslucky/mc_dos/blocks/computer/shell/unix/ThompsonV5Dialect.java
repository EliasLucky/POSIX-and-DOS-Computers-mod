package com.eliaslucky.mc_dos.blocks.computer.shell.unix;

import com.eliaslucky.mc_dos.api.shell.ShellDialect;

/**
 * Thompson shell as shipped with UNIX v5 (1974).
 * Pipes, `>`, `<`, `&`, `;`, and `(...)`. No variables. No control flow.
 */
public final class ThompsonV5Dialect extends ShellDialect {
    public static final ThompsonV5Dialect INSTANCE = new ThompsonV5Dialect();
    private ThompsonV5Dialect() {}

    @Override public String name()              { return "Thompson sh (UNIX v5)"; }
    @Override public boolean supportsPipes()    { return true; }
    @Override public boolean supportsBackground() { return true; }
    @Override public boolean supportsVariables()  { return false; }
    @Override public boolean supportsControlFlow() { return false; }
    @Override public boolean supportsFunctions()   { return false; }
    @Override public boolean supportsCommandSubst() { return false; }
    @Override public boolean supportsArithmetic()   { return false; }
    @Override public boolean supportsHereDoc()      { return false; }
    @Override public boolean supportsLogicalOps()   { return false; }
}
