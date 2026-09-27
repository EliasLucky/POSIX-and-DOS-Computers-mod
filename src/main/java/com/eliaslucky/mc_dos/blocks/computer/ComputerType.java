package com.eliaslucky.mc_dos.blocks.computer;

import java.util.List;

import com.eliaslucky.mc_dos.api.bios.Bios;
import com.eliaslucky.mc_dos.api.vfs.DriveType;
import com.eliaslucky.mc_dos.blocks.computer.processors.Dos6CommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor;
import com.eliaslucky.mc_dos.blocks.computer.processors.LinuxCommandProcessor;

/**
 * A type of machine. Each value describes one physical computer: its
 * model name, its CPU, its BIOS, its bus, its default drives, its
 * pre-installed files, and the operating system processor that runs
 * on it.
 *
 * <p>A {@code ComputerType} is the source of truth for every fact that
 * does not change between boots. Anything the user can edit in BIOS
 * SETUP lives on {@link MachineConfig} instead.
 *
 * <p>Addons can add new machine types by extending this enum — but
 * Java enums cannot be extended from another jar. The practical way
 * for an addon to ship its own machine is to define its own block and
 * its own {@code ICommandProcessor} that returns a new kernel. The
 * existing {@code ComputerType} values stay in the base mod.
 */
public enum ComputerType {
	IBM_PC_AT(
		"IBM Personal Computer AT (Model 5170)",
		"Intel 80286 @ 8 MHz",   
		"IBM Personal Computer DOS Version 3.30",
        "ISA",
		0xFFFFFF/*0x00FF00*/,
		List.of(
				"COMMAND.COM",
				"AUTOEXEC.BAT",
				"CONFIG.SYS",
				"DOS/",
				"DOS/QBASIC.EXE",
        		"DOS/MSD.EXE"),
		new Dos6CommandProcessor(),
		"C:\\",
		new IBMATBios(),
		List.of(
			    new DriveBaySpec(DriveType.FDD_1_2M,  "A", "/dev/fd0", "/mnt/floppy"),
			    new DriveBaySpec(DriveType.FDD_1_44M, "B", "/dev/fd1", "/mnt/floppy2")),
        () -> MachineConfig.ibmAt(System.currentTimeMillis())
	),

	PENTIUM_4_LINUX(
		"Pentium 4 ACPI BIOS Revision 1008",
		"Intel(R) Pentium(R) 4 CPU 2.40GHz",
		"Debian GNU/Linux 3.0 (woody)",
        "PCI / AGP",
		0xFFFFFF,
		List.of(
	            "bin/",
	            "dev/",
	            "etc/",
	            "home/",
	            "root/",
	            "usr/",
	            "var/",
	            "etc/passwd",
	            "etc/fstab",
	            "etc/hostname",
	            "root/.bashrc"
	        ),
		new LinuxCommandProcessor(),
		"/",
		new AwardBios(),
		List.of(
			    new DriveBaySpec(DriveType.FDD_1_44M, "A", "/dev/fd0", "/mnt/floppy"),
			    new DriveBaySpec(DriveType.DVD_RW,    "D", "/dev/sr0", "/mnt/cdrom")),
        () -> MachineConfig.pentium4(System.currentTimeMillis())
	);

	public final String modelName;
	public final String cpuName;
    public final String osVersion;
	public final int textColor;
	public final List<String> defaultFiles;
	public final ICommandProcessor commandProcessor;
	public final String defaultPath;
    public final Bios bios;
    public final List<DriveBaySpec> driveBays;
    public final Supplier<MachineConfig> defaultConfig;
    /**
     * A drive bay fitted to a machine.
     *
     * @param type             the drive type (floppy, CD, DVD, ...)
     * @param dosLetter        the DOS drive letter, or {@code null}
     * @param posixDevice      the POSIX device path, or {@code null}
     * @param posixMountPoint  the POSIX default mount point, or {@code null}
     */
    public record DriveBaySpec(
            DriveType type,
            String dosLetter,        // "A", "B", or null
            String posixDevice,      // "/dev/fd0" or "/dev/sr0" or null
            String posixMountPoint   // "/mnt/floppy" or "/mnt/cdrom" or null
    ) {}
	ComputerType(String modelName,String cpuName, String osVersion,String busType, int textColor, List<String> defaultFiles, ICommandProcessor commandProcessor, String defaultPath,Bios bios,Supplier<MachineConfig> defaultConfig) {
		this.modelName = modelName;
		this.cpuName = cpuName;
        this.osVersion = osVersion;
		this.textColor = textColor;
		this.defaultFiles = defaultFiles;
		this.commandProcessor = commandProcessor;
		this.defaultPath = defaultPath;
        this.bios = bios;
        this.defaultConfig = defaultConfig;
	}
}
