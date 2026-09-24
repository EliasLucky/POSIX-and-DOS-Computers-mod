package com.eliaslucky.mc_dos.blocks.computer.shell.unix;

import com.eliaslucky.mc_dos.api.shell.ShellDialect;

/**
 * POSIX sh (IEEE 1003.1, 1988+). Everything Bourne has, plus:
 *   ${VAR:-default}, ${VAR:=...}, ${#VAR}
 *   $((arithmetic))
 *   $(command)  (modern command substitution)
 * Bourne-compatible backticks still work.
 */
public final class PosixShDialect extends ShellDialect {
    public static final PosixShDialect INSTANCE = new PosixShDialect();
    private PosixShDialect() {}

    @Override public String name()              { return "POSIX sh"; }
    @Override public boolean supportsPipes()    { return true; }
    @Override public boolean supportsBackground() { return true; }
    @Override public boolean supportsAppend()     { return true; }
    @Override public boolean supportsPositional() { return true; }
    @Override public boolean supportsVariables()  { return true; }
    @Override public boolean supportsControlFlow() { return true; }
    @Override public boolean supportsFunctions()   { return true; }
    @Override public boolean supportsCommandSubst() { return true; }
    @Override public boolean supportsArithmetic()   { return true; }
    @Override public boolean supportsHereDoc()      { return true; }
    @Override public boolean supportsParamExpansion() { return true; }
    @Override public boolean supportsLogicalOps()   { return false; } // still no && ||
}
