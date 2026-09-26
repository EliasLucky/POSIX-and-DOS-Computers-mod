package com.eliaslucky.mc_dos.blocks.computer.fs;

/**
 * The outcome of a filesystem operation that could fail.
 *
 * <p>Callers either check {@link #success()} and format an error, or
 * propagate the result up until something decides how to present it.
 * The presentation (a QBASIC dialog, a shell line, a status bar) is
 * always a caller decision — the result type itself is silent.
 *
 * @param success whether the operation completed
 * @param error   the error category, or {@code null} on success
 * @param detail  extra context (filename, drive letter); may be {@code null}
 *
 * @since 1.5
 */
public record FileOpResult(boolean success, FileError error, String detail) {
    /** @return a successful result. */
    public static FileOpResult ok() {
        return new FileOpResult(true, null, null);
    }

    /**
     * A successful result with context.
     *
     * @param detail context (bytes written, etc.)
     * @return the result
     */
    public static FileOpResult ok(String detail) {
        return new FileOpResult(true, null, detail);
    }

    /**
     * A failed result.
     *
     * @param e      the error category
     * @param detail extra context (filename, etc.)
     * @return the result
     */
    public static FileOpResult fail(FileError e, String detail) {
        return new FileOpResult(false, e, detail);
    }

    /**
     * Convenience: the formatted message for a given OS family.
     *
     * @param osFamily {@code "dos"} / {@code "posix"} / {@code "unix"}
     * @return a display string, or {@code ""} on success
     */
    public String messageFor(String osFamily) {
        if (success) return "";
        String base = error.messageFor(osFamily);
        if (detail == null || detail.isEmpty()) return base;
        return base + ": " + detail;
    }
}
