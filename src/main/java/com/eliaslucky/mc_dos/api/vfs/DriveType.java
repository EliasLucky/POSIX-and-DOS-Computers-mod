package com.eliaslucky.mc_dos.api.vfs;

/**
 * A physical drive bay's capability. Determines what media can be
 * inserted and read.
 *
 * <p>The compatibility table reflects real-world behavior:
 * <ul>
 *   <li>1.2M 5.25" drives read 360K disks (at reduced speed)</li>
 *   <li>1.44M 3.5" drives read 720K disks</li>
 *   <li>DVD drives read CDs</li>
 *   <li>Blu-ray drives read DVDs and CDs</li>
 *   <li>The reverse is not true in any case</li>
 * </ul>
 *
 * @since 1.5
 */
public enum DriveType {
    FDD_360K ("360K  5.25\"", "A"),
    FDD_1_2M ("1.2M  5.25\"", "A"),
    FDD_720K ("720K  3.5\"",  "A"),
    FDD_1_44M("1.44M 3.5\"",  "A"),

    CD_ROM   ("CD-ROM Drive",  "D"),
    CD_RW    ("CD-RW Drive",   "D"),
    DVD_ROM  ("DVD-ROM Drive", "D"),
    DVD_RW   ("DVD-RW Drive",  "D"),
    BD_ROM   ("Blu-ray Drive", "D"),
    BD_RE    ("Blu-ray Burner","D");

    private final String display;
    private final String defaultDosLetter;

    DriveType(String display, String defaultDosLetter) {
        this.display = display;
        this.defaultDosLetter = defaultDosLetter;
    }

    /** @return the label a BIOS would print. */
    public String displayName() { return display; }

    /** @return the DOS drive letter this type defaults to (A or D). */
    public String defaultDosLetter() { return defaultDosLetter; }

    /** @return {@code true} if this bay can read the given media. */
    public boolean canRead(DiskMedia media) {
        if (media == null) return false;
        return switch (this) {
            case FDD_360K  -> media == DiskMedia.FLOPPY_360K;
            case FDD_1_2M  -> media == DiskMedia.FLOPPY_360K
                           || media == DiskMedia.FLOPPY_1_2M;
            case FDD_720K  -> media == DiskMedia.FLOPPY_720K;
            case FDD_1_44M -> media == DiskMedia.FLOPPY_720K
                           || media == DiskMedia.FLOPPY_1_44M;
            case CD_ROM    -> media == DiskMedia.CD_ROM;
            case CD_RW     -> media == DiskMedia.CD_ROM
                           || media == DiskMedia.CD_RW;
            case DVD_ROM   -> media == DiskMedia.CD_ROM || media == DiskMedia.CD_RW
                           || media == DiskMedia.DVD_ROM;
            case DVD_RW    -> media == DiskMedia.CD_ROM || media == DiskMedia.CD_RW
                           || media == DiskMedia.DVD_ROM || media == DiskMedia.DVD_RW;
            case BD_ROM    -> media != DiskMedia.FLOPPY_360K
                           && media != DiskMedia.FLOPPY_720K
                           && media != DiskMedia.FLOPPY_1_2M
                           && media != DiskMedia.FLOPPY_1_44M;
            case BD_RE     -> media.isOptical();
        };
    }

    /** @return {@code true} if this is a floppy-family drive. */
    public boolean isFloppy() { return name().startsWith("FDD"); }

    /** @return {@code true} if the drive can write to removable media. */
    public boolean canWrite() {
        return this == CD_RW || this == DVD_RW || this == BD_RE
            || this == FDD_360K || this == FDD_1_2M
            || this == FDD_720K || this == FDD_1_44M;
    }
}
