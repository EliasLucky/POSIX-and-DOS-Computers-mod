package com.eliaslucky.mc_dos.api.shell;

/** Operator between two stages of a pipeline. */
public enum LogicalOp {
    /** Pipes: stage N's stdout becomes stage N+1's stdin. */
    PIPE,
    /** `;` — run stage N+1 unconditionally after N. */
    SEQ,
    /** `&&` — run stage N+1 only if N exited 0. Deferred (bash only). */
    AND,
    /** `||` — run stage N+1 only if N exited nonzero. Deferred (bash only). */
    OR,
    /** `&` — start stage N+1 in the background. Deferred. */
    BACKGROUND
}
