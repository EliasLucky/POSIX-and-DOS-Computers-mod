package com.eliaslucky.mc_dos.api.bios;

/**
 * The BIOS-level configuration of a machine.
 *
 * <p>Everything the SETUP screen edits and the POST screen reports lives
 * here. This is <em>not</em> the same as the operating system's state —
 * the OS sees files and drivers, while {@code MachineConfig} sees
 * hardware. Inserting a diskette does not change this record; it
 * describes the drive bay, not the media in it.
 *
 * <p>Instances are immutable. To change a value, use one of the
 * {@code withX(...)} helpers, which return a new config.
 *
 * @param systemTime        wall-clock time the BIOS thinks it is, in
 *                          milliseconds since the epoch. Modified by
 *                          the SETUP screen's Time and Date editors.
 * @param floppyA           type of the drive on A: / first floppy bay
 * @param floppyB           type of the drive on B: / second floppy bay
 * @param hardDisk1         type of the first hard disk (C: on DOS)
 * @param hardDisk2         type of the second hard disk (D: on DOS)
 * @param baseMemoryKb      conventional memory in kilobytes; 640 for
 *                          every PC-compatible
 * @param extendedMemoryKb  memory beyond 1 MB, in kilobytes
 * @param mathCoprocessor   whether a floating-point coprocessor is fitted
 * @param primaryDisplay    the display adapter the BIOS will use
 *
 * @see Bios
 * @since 1.5
 */
public record MachineConfig(
        long systemTime,
        FloppyType floppyA,
        FloppyType floppyB,
        DiskType hardDisk1,
        DiskType hardDisk2,
        int baseMemoryKb,
        int extendedMemoryKb,
        boolean mathCoprocessor,
        DisplayType primaryDisplay
) {

    /**
     * Floppy drive capability. Values match the entries a real IBM AT
     * BIOS offered in its "Floppy Disk A:" / "Floppy Disk B:" rows.
     */
    public enum FloppyType {
        NONE        ("Not Installed"),
        FDD_360K    ("360K  5.25\""),
        FDD_1_2M    ("1.2M  5.25\""),
        FDD_720K    ("720K  3.5\""),
        FDD_1_44M   ("1.44M 3.5\"");

        private final String display;
        FloppyType(String display) { this.display = display; }
        /** @return the string a BIOS would print for this type. */
        public String displayName() { return display; }
    }

    /**
     * Hard disk type. The real IBM AT had 47 predefined types;
     * only a handful. Adding more is a matter of extending this enum.
     */
    public enum DiskType {
        NONE    ("Not Installed"),
        TYPE_1  ("Type 1"),
        TYPE_2  ("Type 2"),
        TYPE_3  ("Type 3"),
        TYPE_4  ("Type 4"),
        TYPE_5  ("Type 5"),
        TYPE_47 ("User Defined");

        private final String display;
        DiskType(String display) { this.display = display; }
        /** @return the string a BIOS would print for this type. */
        public String displayName() { return display; }
    }

    /**
     * Primary display adapter.
     */
    public enum DisplayType {
        MONO        ("Monochrome"),
        CGA_40      ("CGA 40-column"),
        CGA_80      ("CGA 80-column"),
        EGA         ("EGA"),
        VGA         ("VGA"),
        SPECIAL_EGA ("Special (EGA)");

        private final String display;
        DisplayType(String display) { this.display = display; }
        /** @return the string a BIOS would print for this type. */
        public String displayName() { return display; }
    }

    // Factory presets

    /**
     * A factory configuration matching a stock IBM Personal Computer AT.
     *
     * @param systemTime the current time in millis
     * @return a config with 1.2M floppy, one 20 MB hard disk, 640K base,
     *         and EGA display
     */
    public static MachineConfig ibmAt(long systemTime) {
        return new MachineConfig(
                systemTime,
                FloppyType.FDD_1_2M,
                FloppyType.NONE,
                DiskType.TYPE_2,
                DiskType.TYPE_3,
                640,
                1024,
                true,
                DisplayType.SPECIAL_EGA);
    }

    /**
     * A factory configuration for a turn-of-the-millennium PC.
     *
     * @param systemTime the current time in millis
     * @return a config with one 1.44M floppy, two hard disks, VGA display
     */
    public static MachineConfig pentium4(long systemTime) {
        return new MachineConfig(
                systemTime,
                FloppyType.FDD_1_44M,
                FloppyType.NONE,
                DiskType.TYPE_47,
                DiskType.TYPE_47,
                640,
                512 * 1024,
                true,
                DisplayType.VGA);
    }

    // Immutable "setters"

    /** @return a new config with the given system time. */
    public MachineConfig withSystemTime(long t) {
        return new MachineConfig(t, floppyA, floppyB, hardDisk1, hardDisk2,
                baseMemoryKb, extendedMemoryKb, mathCoprocessor, primaryDisplay);
    }

    /** @return a new config with the given floppy A type. */
    public MachineConfig withFloppyA(FloppyType t) {
        return new MachineConfig(systemTime, t, floppyB, hardDisk1, hardDisk2,
                baseMemoryKb, extendedMemoryKb, mathCoprocessor, primaryDisplay);
    }

    /** @return a new config with the given floppy B type. */
    public MachineConfig withFloppyB(FloppyType t) {
        return new MachineConfig(systemTime, floppyA, t, hardDisk1, hardDisk2,
                baseMemoryKb, extendedMemoryKb, mathCoprocessor, primaryDisplay);
    }

    /** @return a new config with the given hard disk 1 type. */
    public MachineConfig withHardDisk1(DiskType t) {
        return new MachineConfig(systemTime, floppyA, floppyB, t, hardDisk2,
                baseMemoryKb, extendedMemoryKb, mathCoprocessor, primaryDisplay);
    }

    /** @return a new config with the given hard disk 2 type. */
    public MachineConfig withHardDisk2(DiskType t) {
        return new MachineConfig(systemTime, floppyA, floppyB, hardDisk1, t,
                baseMemoryKb, extendedMemoryKb, mathCoprocessor, primaryDisplay);
    }

    /** @return a new config with the given primary display. */
    public MachineConfig withPrimaryDisplay(DisplayType t) {
        return new MachineConfig(systemTime, floppyA, floppyB, hardDisk1, hardDisk2,
                baseMemoryKb, extendedMemoryKb, mathCoprocessor, t);
    }

    // Formatting helpers

    /**
     * Format the system time as a BIOS would in a SETUP row:
     * {@code "HH:MM:SS"}.
     *
     * @return a 24-hour time string
     */
    public String timeString() {
        return new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(systemTime));
    }

    /**
     * Format the system date as a BIOS would in a SETUP row:
     * {@code "MM/DD/YYYY"}.
     *
     * @return an American-format date string, matching 1980s BIOS conventions
     */
    public String dateString() {
        return new java.text.SimpleDateFormat("MM/dd/yyyy").format(new java.util.Date(systemTime));
    }
}
