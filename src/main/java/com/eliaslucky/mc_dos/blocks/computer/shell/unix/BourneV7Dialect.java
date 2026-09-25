package com.eliaslucky.mc_dos.blocks.computer.shell.unix;

import com.eliaslucky.mc_dos.api.shell.ShellDialect;

/**
 * Bourne shell as shipped with UNIX v7 (1979).
 * Adds shell variables, `export`, control flow (if/then/fi, while, for,
 * case), functions, backtick command substitution.
 * Still no `&&`/`||` (those came with ksh), no `$()`, no `$(( ))`.
 */
public final class BourneV7Dialect extends ShellDialect {
    public static final BourneV7Dialect INSTANCE = new BourneV7Dialect();
    private BourneV7Dialect() {}

    @Override public String name()              { return "Bourne sh (UNIX v7)"; }
    @Override public boolean supportsPipes()    { return true; }
    @Override public boolean supportsBackground() { return true; }
    @Override public boolean supportsAppend()     { return true; }
    @Override public boolean supportsPositional() { return true; }
    @Override public boolean supportsVariables()  { return true; }
    @Override public boolean supportsControlFlow() { return true; }
    @Override public boolean supportsFunctions()   { return true; }
    @Override public boolean supportsCommandSubst() { return true; }   // backticks only
    @Override public boolean supportsArithmetic()   { return false; }
    @Override public boolean supportsHereDoc()      { return true; }
    @Override public boolean supportsLogicalOps()   { return false; }
}
