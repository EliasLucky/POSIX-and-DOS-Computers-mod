package com.eliaslucky.mc_dos.api.vfs;

/**
 * A type of removable storage media.
 *
 * <p>Each value carries the physical characteristics that matter for
 * gameplay: how much can be stored, whether the media is writable
 * after insertion, and whether the media is small enough to be
 * inserted into a drive that shares its physical form factor.
 *
 * <p>Addon support for new media means the mod ships a generic
 * {@code RemovableMediaItem} and addons use one of the existing
 * {@code DiskMedia} values. If a genuinely new physical form factor is
 * needed (say, Zip disks), open an issue and it becomes a registry.
 *
 * @since 1.5
 */
public enum DiskMedia {
    // Floppies
	FLOPPY_360K("360K  5.25\"", 5_760L, 112, true),
	FLOPPY_720K("720K  3.5\"", 11_520L, 112, true),
	FLOPPY_1_2M("1.2M  5.25\"", 19_200L, 224, true),
	FLOPPY_1_44M("1.44M 3.5\"", 23_040L, 224, true),

    // Optical
    CD_ROM   ("CD-ROM",   11_000_000L,65_535, false),
    CD_RW    ("CD-RW",    11_000_000L,65_535, true),
    DVD_ROM  ("DVD-ROM",  73_000_000L,65_535, false),
    DVD_RW   ("DVD-RW",   73_000_000L,65_535, true),
    BD_ROM   ("Blu-ray",  390_000_000L,65_535, false),
    BD_RE    ("BD-RE",    390_000_000L,65_535, true);

    private final String display;
    private final long   capacityBytes;
    private final boolean writable;
    private final int    maxEntries;
    /**
     * @param display       the label printed by BIOS and format tools
     * @param capacityBytes maximum content size, scaled for the mod
     * @param maxEntries    maximum number of file entries
     * @param writable      whether the media accepts writes
     */
    DiskMedia(String display, long capacityBytes,int maxEntries, boolean writable) {
        this.display = display;
        this.capacityBytes = capacityBytes;
        this.writable = writable;
        this.maxEntries = maxEntries;
    }

    /** @return the label a BIOS or format tool would print. */
    public String displayName() { return display; }

    /** @return the maximum number of bytes a filled disk can hold. */
    public long capacityBytes() { return capacityBytes; }

    /** @return whether the media accepts writes after manufacture. */
    public boolean writable() { return writable; }

    /** @return the max filename length on this media (8 for DOS-era floppies). */
    public int maxEntries() { return maxEntries; }

    /** @return {@code true} if this is a floppy-family disk. */
    public boolean isFloppy() { return name().startsWith("FLOPPY"); }

    /** @return {@code true} if this is an optical disc. */
    public boolean isOptical() { return !isFloppy(); }
}
