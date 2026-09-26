package com.eliaslucky.mc_dos.blocks.computer.fs;

/**
 * A categorized file-system error.
 *
 * <p>Each value carries the OS-specific message that a user should
 * see. The category itself is what callers switch on; the message is
 * only for display. This lets a command processor, a TUI dialog, and
 * a redirect all react to the same failure while phrasing it the way
 * their OS did.
 *
 * <p>Error codes are taken from MS-DOS's documented error table and
 * from POSIX's {@code errno.h}.
 *
 * @since 1.5
 */
public enum FileError {
    /** DOS 39, POSIX ENOSPC. */
    DISK_FULL           (39, 28,
                         "Insufficient disk space",
                         "No space left on device"),

    /** DOS 4, POSIX EMFILE. Too many files. */
    TOO_MANY_FILES      (4, 24,
                         "Too many open files",
                         "Too many open files"),

    /** DOS 19, POSIX EROFS. */
    WRITE_PROTECTED     (19, 30,
                         "Write protect error writing drive",
                         "Read-only file system"),

    /** DOS 5, POSIX EACCES. */
    ACCESS_DENIED       (5, 13,
                         "Access denied",
                         "Permission denied"),

    /** DOS 2, POSIX ENOENT. */
    FILE_NOT_FOUND      (2, 2,
                         "File not found",
                         "No such file or directory"),

    /** POSIX ENOTDIR / ENOTEMPTY hybrid. */
    DIRECTORY_PROBLEM   (3, 20,
                         "Path not found",
                         "Not a directory"),

    /** POSIX ENAMETOOLONG. */
    NAME_TOO_LONG       (3, 36,
                         "File name too long",
                         "File name too long"),

    /** Generic invalid name — illegal characters. */
    INVALID_NAME        (3, 22,
                         "Invalid file name",
                         "Invalid argument"),

    /** Catch-all for anything uncategorized. */
    OTHER               (1, 5,
                         "General failure",
                         "Input/output error");

    private final int dosCode;
    private final int posixErrno;
    private final String dosMessage;
    private final String posixMessage;

    FileError(int dosCode, int posixErrno, String dosMessage, String posixMessage) {
        this.dosCode = dosCode;
        this.posixErrno = posixErrno;
        this.dosMessage = dosMessage;
        this.posixMessage = posixMessage;
    }

    /** @return the DOS-style error code (0 = none). */
    public int dosCode() { return dosCode; }

    /** @return the POSIX errno value. */
    public int posixErrno() { return posixErrno; }

    /**
     * The display message for a given OS family.
     *
     * @param osFamily {@code "dos"}, {@code "posix"}, {@code "unix"}
     * @return the message
     */
    public String messageFor(String osFamily) {
        return "dos".equalsIgnoreCase(osFamily) ? dosMessage : posixMessage;
    }
}
