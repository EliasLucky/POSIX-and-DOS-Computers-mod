package com.eliaslucky.mc_dos.blocks.computer;

import java.util.List;
import java.util.function.Supplier;

import com.eliaslucky.mc_dos.api.bios.Bios;
import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.api.vfs.DriveType;
import com.eliaslucky.mc_dos.blocks.computer.bios.AwardBios;
import com.eliaslucky.mc_dos.blocks.computer.bios.IbmAtBios;
import com.eliaslucky.mc_dos.blocks.computer.processors.Dos6CommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.LinuxCommandProcessor;

/**
 * The built-in machine types. Both implement {@link MachineType} so
 * they live in {@link MachineTypeRegistry} alongside addon types.
 *
 * <p>Addons do not extend this enum — they implement
 * {@code MachineType} themselves and register via
 * {@code MachineTypeRegistry.register(...)}.
 *
 * @since 1.0
 */
public enum ComputerType implements MachineType {

    IBM_PC_AT(
        "mc_dos:ibm_pc_at",
        "IBM Personal Computer AT (Model 5170)",
        "Intel 80286 @ 8 MHz",
        "IBM Personal Computer DOS Version 3.30",
        "ISA",
        0xFFFFFF,
        List.of(
            "COMMAND.COM",
            "AUTOEXEC.BAT",
            "CONFIG.SYS",
            "DOS/",
            "DOS/QBASIC.EXE",
            "DOS/EDIT.COM",
            "DOS/MSD.EXE"
        ),
        new Dos6CommandProcessor(),
        "C:\\",
        new IbmAtBios(),
        List.of(
            new DriveBaySpec(DriveType.FDD_1_2M,  "A", "/dev/fd0", "/mnt/floppy"),
            new DriveBaySpec(DriveType.FDD_1_44M, "B", "/dev/fd1", "/mnt/floppy2")
        ),
        () -> MachineConfig.ibmAt(System.currentTimeMillis())
    ),

    PENTIUM_4_LINUX(
        "mc_dos:pentium_4_linux",
        "Pentium 4 ACPI BIOS Revision 1008",
        "Intel(R) Pentium(R) 4 CPU 2.40GHz",
        "Debian GNU/Linux 3.0 (woody)",
        "PCI / AGP",
        0xFFFFFF,
        List.of(
            "bin/", "dev/", "etc/", "home/", "root/", "usr/", "var/",
            "etc/passwd", "etc/fstab", "etc/hostname", "root/.bashrc"
        ),
        new LinuxCommandProcessor(),
        "/",
        new AwardBios(),
        List.of(
            new DriveBaySpec(DriveType.FDD_1_44M, "A", "/dev/fd0", "/mnt/floppy"),
            new DriveBaySpec(DriveType.DVD_RW,    "D", "/dev/sr0", "/mnt/cdrom")
        ),
        () -> MachineConfig.pentium4(System.currentTimeMillis())
    );

    private final String id;
    private final String modelName;
    private final String cpuName;
    private final String osVersion;
    private final String busType;
    private final int    textColor;
    private final List<String> defaultFiles;
    private final ICommandProcessor commandProcessor;
    private final String defaultPath;
    private final Bios   bios;
    private final List<DriveBaySpec> driveBays;
    private final Supplier<MachineConfig> defaultConfig;

    ComputerType(String id,
                 String modelName,
                 String cpuName,
                 String osVersion,
                 String busType,
                 int textColor,
                 List<String> defaultFiles,
                 ICommandProcessor commandProcessor,
                 String defaultPath,
                 Bios bios,
                 List<DriveBaySpec> driveBays,
                 Supplier<MachineConfig> defaultConfig) {
        this.id = id;
        this.modelName = modelName;
        this.cpuName = cpuName;
        this.osVersion = osVersion;
        this.busType = busType;
        this.textColor = textColor;
        this.defaultFiles = defaultFiles;
        this.commandProcessor = commandProcessor;
        this.defaultPath = defaultPath;
        this.bios = bios;
        this.driveBays = driveBays;
        this.defaultConfig = defaultConfig;
    }

    @Override public String id()                       { return id; }
    @Override public String modelName()                { return modelName; }
    @Override public String cpuName()                  { return cpuName; }
    @Override public String osVersion()                { return osVersion; }
    @Override public String busType()                  { return busType; }
    @Override public int    textColor()                { return textColor; }
    @Override public List<String> defaultFiles()       { return defaultFiles; }
    @Override public ICommandProcessor commandProcessor() { return commandProcessor; }
    @Override public String defaultPath()              { return defaultPath; }
    @Override public Bios   bios()                     { return bios; }
    @Override public List<DriveBaySpec> driveBays()    { return driveBays; }
    @Override public Supplier<MachineConfig> defaultConfig() { return defaultConfig; }
}