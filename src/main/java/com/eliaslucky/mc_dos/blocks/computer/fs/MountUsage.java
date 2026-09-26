package com.eliaslucky.mc_dos.blocks.computer.fs;

import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

/**
 * Byte and entry usage accounting for a mounted volume.
 *
 * <p>A {@code MountUsage} is created once per mount and lives as long
 * as the mount does. It exposes two counters bytes used by file
 * content, and total file + directory entries; and provides both
 * incremental updates and a full recompute.
 *
 * <h2>Caching model</h2>
 * <p>Callers that know exactly what changed use the incremental
 * methods ({@link #addEntry}, {@link #removeEntry}, {@link #changeSize}).
 * When a caller is unsure (or performs a batch operation), it calls
 * {@link #markDirty()}. Reads of {@link #bytesUsed()} or
 * {@link #entriesUsed()} trigger a full recompute if the dirty flag is
 * set. This makes the cache correct even if a mutation path forgets to
 * notify at the cost of one tree walk the next time usage is read.
 *
 * <p>All public methods are {@code synchronized}. Concurrent access is
 * unlikely (server thread only) but the cost is negligible.
 *
 * @since 1.5
 */
public final class MountUsage {
    private final VirtualFileSystem.Node rootNode;

    private long bytesUsed;
    private int  entriesUsed;
    private boolean dirty;

    /**
     * @param rootNode the root of the mounted tree; never {@code null}
     */
    public MountUsage(VirtualFileSystem.Node rootNode) {
        this.rootNode = rootNode;
        recompute();
    }

    // Reads

    /** @return total bytes used by file content in this volume. */
    public synchronized long bytesUsed() {
        if (dirty) recompute();
        return bytesUsed;
    }

    /** @return total file + directory entries in this volume. */
    public synchronized int entriesUsed() {
        if (dirty) recompute();
        return entriesUsed;
    }

    /**
     * Account for a newly created entry.
     *
     * @param bytes the file's content length; 0 for directories
     */
    public synchronized void addEntry(int bytes) {
        if (dirty) return;         // recompute will catch it
        bytesUsed += bytes;
        entriesUsed++;
    }

    /**
     * Account for a removed entry.
     *
     * @param bytes the file's content length at removal time
     */
    public synchronized void removeEntry(int bytes) {
        if (dirty) return;
        bytesUsed -= bytes;
        entriesUsed--;
    }

    /**
     * Account for a size change on an existing file.
     *
     * @param delta the change in bytes; negative for shrinkage
     */
    public synchronized void changeSize(long delta) {
        if (dirty) return;
        bytesUsed += delta;
    }

    // Full recompute

    /**
     * Mark the cache stale. Called by callers that performed a mutation
     * without tracking it incrementally (rare, but useful for safety).
     */
    public synchronized void markDirty() {
        dirty = true;
    }

    /** Recompute the counters from scratch by walking the tree. */
    public synchronized void recompute() {
        long[] totals = { 0, 0 };
        walk(rootNode, totals);
        bytesUsed   = totals[0];
        entriesUsed = (int) totals[1];
        dirty       = false;
    }

    private static void walk(VirtualFileSystem.Node node, long[] totals) {
        for (VirtualFileSystem.Node child : node.children.values()) {
            if (child.isDirectory) {
                totals[1]++;                // directories count as entries
                walk(child, totals);
            } else {
                totals[0] += child.content.length();
                totals[1]++;
            }
        }
    }
}
