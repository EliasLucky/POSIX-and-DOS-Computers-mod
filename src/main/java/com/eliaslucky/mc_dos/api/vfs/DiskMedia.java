package com.eliaslucky.mc_dos.api.vfs;

/**
 * A type of removable storage media.
 *
 * <p>Each value carries the physical characteristics that matter for
 * gameplay: how much can be stored, whether the media is writable
 * after insertion, and whether the media is small enough to be
 * inserted into a drive that shares its physical form factor.
 *
 * <p>Addons can add new media types by extending this enum — but since
 * Java enums cannot be extended by other jars, in practice addon
 * support for new media means the mod ships a generic
 * {@code RemovableMediaItem} and addons use one of the existing
 * {@code DiskMedia} values. If a genuinely new physical form factor is
 * needed (say, Zip disks), open an issue and it becomes a registry.
 *
 * @since 1.5
 */
public enum DiskMedia {
    // Floppies
    FLOPPY_360K  ("360K  5.25\"", 368_640L,    true,  75),
    FLOPPY_720K  ("720K  3.5\"",  737_280L,    true,  80),
    FLOPPY_1_2M  ("1.2M  5.25\"", 1_228_800L,  true,  80),
    FLOPPY_1_44M ("1.44M 3.5\"",  1_474_560L,  true,  80),

    // Optical
    CD_ROM   ("CD-ROM",   700_000_000L, false, 1),
    CD_RW    ("CD-RW",    700_000_000L, true,  1),
    DVD_ROM  ("DVD-ROM",  4_700_000_000L, false, 1),
    DVD_RW   ("DVD-RW",   4_700_000_000L, true,  1),
    BD_ROM   ("Blu-ray",  25_000_000_000L, false, 1),
    BD_RE    ("BD-RE",    25_000_000_000L, true,  1);

    private final String display;
    private final long   capacityBytes;
    private final boolean writable;
    private final int    maxNameLength;

    DiskMedia(String display, long capacityBytes, boolean writable, int maxNameLength) {
        this.display = display;
        this.capacityBytes = capacityBytes;
        this.writable = writable;
        this.maxNameLength = maxNameLength;
    }

    /** @return the label a BIOS or format tool would print. */
    public String displayName() { return display; }

    /** @return the maximum number of bytes a filled disk can hold. */
    public long capacityBytes() { return capacityBytes; }

    /** @return whether the media accepts writes after manufacture. */
    public boolean writable() { return writable; }

    /** @return the max filename length on this media (8 for DOS-era floppies). */
    public int maxNameLength() { return maxNameLength; }

    /** @return {@code true} if this is a floppy-family disk. */
    public boolean isFloppy() { return name().startsWith("FLOPPY"); }

    /** @return {@code true} if this is an optical disc. */
    public boolean isOptical() { return !isFloppy(); }
}
