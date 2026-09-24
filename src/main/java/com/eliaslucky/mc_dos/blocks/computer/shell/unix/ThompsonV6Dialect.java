package com.eliaslucky.mc_dos.blocks.computer.shell.unix;

import com.eliaslucky.mc_dos.api.shell.ShellDialect;

/**
 * Thompson shell as shipped with UNIX v6 (1975).
 * Adds `>>`, and `$1`..`$9` positional parameters. Still no variables,
 * no control flow, no command substitution.
 */
public final class ThompsonV6Dialect extends ShellDialect {
    public static final ThompsonV6Dialect INSTANCE = new ThompsonV6Dialect();
    private ThompsonV6Dialect() {}

    @Override public String name()              { return "Thompson sh (UNIX v6)"; }
    @Override public boolean supportsPipes()    { return true; }
    @Override public boolean supportsBackground() { return true; }
    @Override public boolean supportsAppend()     { return true; }
    @Override public boolean supportsPositional() { return true; }
    @Override public boolean supportsVariables()  { return false; }
    @Override public boolean supportsControlFlow() { return false; }
    @Override public boolean supportsFunctions()   { return false; }
    @Override public boolean supportsCommandSubst() { return false; }
    @Override public boolean supportsArithmetic()   { return false; }
    @Override public boolean supportsHereDoc()      { return false; }
    @Override public boolean supportsLogicalOps()   { return false; }
}
