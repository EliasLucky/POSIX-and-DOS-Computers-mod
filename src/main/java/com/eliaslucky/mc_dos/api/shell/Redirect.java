package com.eliaslucky.mc_dos.api.shell;

/**
 * A redirection target. Attached to a Stage to say "this is where stdin
 * comes from" or "this is where stdout goes."
 *
 * A Redirect carries the target and the operation. Which stream it belongs
 * to (stdin vs stdout vs stderr) is known by which slot on the Stage it
 * occupies, not by the Redirect itself.
 */
public sealed interface Redirect {

    enum Mode { READ, WRITE, APPEND }

    /** A file on the local filesystem. */
    record File(String path, Mode mode) implements Redirect {}

    /** A named device in the kernel's namespace (DOS "PRN", UNIX "/dev/lp"). */
    record Device(String name, Mode mode) implements Redirect {}
}
