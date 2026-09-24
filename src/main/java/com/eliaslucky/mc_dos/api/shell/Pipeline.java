package com.eliaslucky.mc_dos.api.shell;

import java.util.List;

/**
 * The result of parsing one shell command line. A pipeline is one or more
 * stages joined by logical operators.
 *
 * For a simple command "dir", stages has one element and between is empty.
 * For "cat file | grep foo", stages has two, between has one (PIPE).
 * For "a ; b ; c", stages has three, between has two (SEQ, SEQ).
 */
public final class Pipeline {
    public final List<Stage> stages;
    public final List<LogicalOp> between;

    public Pipeline(List<Stage> stages, List<LogicalOp> between) {
        this.stages = List.copyOf(stages);
        this.between = List.copyOf(between);
    }

    public boolean isEmpty() { return stages.isEmpty(); }
    public int size()        { return stages.size(); }

    /**
     * One command plus its redirections. The command never sees the
     * redirect syntax — the executor reads stdin, calls the processor,
     * and writes stdout on the command's behalf.
     */
    public static final class Stage {
        public final String raw;          // original text of this stage
        public final String command;      // "echo", "grep", "MCCMD", ...
        public final String args;         // rest of the tokens joined by space
        public final Redirect stdin;      // null = inherit
        public final Redirect stdout;     // null = inherit
        public final Redirect stderr;     // null = inherit

        public Stage(String raw, String command, String args, Redirect stdin, Redirect stdout, Redirect stderr) {
            this.raw = raw;
            this.command = command;
            this.args = args;
            this.stdin = stdin;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        @Override public String toString() {
            return "Stage[" + command + " " + args + "]";
        }
    }
}