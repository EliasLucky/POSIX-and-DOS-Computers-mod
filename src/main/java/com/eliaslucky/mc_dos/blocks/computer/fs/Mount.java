package com.eliaslucky.mc_dos.blocks.computer.fs;

import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

/**
 * A mounted volume in the virtual file system.
 *
 * <p>A mount attaches an external tree — a floppy's contents, a
 * CD's files — under an identifier. On DOS the identifier is a drive
 * letter ({@code "A:"}); on POSIX it's an absolute path
 * ({@code "/mnt/floppy"}).
 *
 * <p>Each mount carries a {@link MountUsage} that tracks its byte and
 * entry usage. Callers that mutate a mounted tree should update
 * {@code usage()} accordingly.
 *
 * @since 1.5
 */
public final class Mount {
    private final String id;
    private final VirtualFileSystem.Node rootNode;
    private final boolean readOnly;
    private final String source;
    private final long capacityBytes;
    private final int maxEntries;
    private final MountUsage usage;

    /**
     * @param id            identifier: {@code "A:"} or {@code "/mnt/floppy"}
     * @param rootNode      the root of the mounted tree
     * @param readOnly      whether the volume rejects writes
     * @param source        human description, e.g. {@code "floppy bay 0"}
     * @param capacityBytes byte capacity; 0 for unlimited
     * @param maxEntries    entry limit; 0 for unlimited
     */
    public Mount(String id,
                 VirtualFileSystem.Node rootNode,
                 boolean readOnly,
                 String source,
                 long capacityBytes,
                 int maxEntries) {
        this.id = id;
        this.rootNode = rootNode;
        this.readOnly = readOnly;
        this.source = source;
        this.capacityBytes = capacityBytes;
        this.maxEntries = maxEntries;
        this.usage = new MountUsage(rootNode);
    }

    public String id()                       { return id; }
    public VirtualFileSystem.Node rootNode() { return rootNode; }
    public boolean readOnly()                { return readOnly; }
    public String source()                   { return source; }
    public long capacityBytes()              { return capacityBytes; }
    public int maxEntries()                  { return maxEntries; }
    public MountUsage usage()                { return usage; }

    /** @return bytes still available, or {@link Long#MAX_VALUE} if unlimited. */
    public long freeBytes() {
        if (capacityBytes == 0) return Long.MAX_VALUE;
        return Math.max(0, capacityBytes - usage.bytesUsed());
    }

    /** @return entries still available, or {@link Integer#MAX_VALUE} if unlimited. */
    public int freeEntries() {
        if (maxEntries == 0) return Integer.MAX_VALUE;
        return Math.max(0, maxEntries - usage.entriesUsed());
    }
    /**
     * Check whether a write of {@code bytes} bytes would be accepted.
     *
     * @param name          the target filename
     * @param incomingBytes the size of the content after write
     * @return an ok result, or a failure describing why
     */
    public FileOpResult checkWrite(String name, int incomingBytes) {
        if (readOnly) return FileOpResult.fail(FileError.WRITE_PROTECTED, name);

        long freeBytes = freeBytes();
        if (freeBytes < incomingBytes) {
            return FileOpResult.fail(FileError.DISK_FULL, name);
        }
        int free = freeEntries();
        if (free < 1 && !rootNode.children.containsKey(name)) {
            return FileOpResult.fail(FileError.TOO_MANY_FILES, name);
        }
        return FileOpResult.ok();
    }
}
