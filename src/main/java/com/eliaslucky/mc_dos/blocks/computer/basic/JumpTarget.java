package com.eliaslucky.mc_dos.blocks.computer.basic;

/**
 * Mutable holder for a program counter that isn't known
 * until the parser has finished building the surrounding block.
 *
 * Shared by DoStmt, LoopStmt, and ExitDoStmt: the parser creates one
 * instance per DO ... LOOP and threads it into all three, then fills in
 * `pc` once the target statement's index in the flat statement list
 * is known.
 */
public final class JumpTarget {
    /** Set once the parser resolves it. -1 means "not yet assigned". */
    public int pc = -1;
}
