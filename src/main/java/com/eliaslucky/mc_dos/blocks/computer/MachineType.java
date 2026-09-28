package com.eliaslucky.mc_dos.blocks.computer;

import com.eliaslucky.mc_dos.api.bios.Bios;
import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.api.vfs.DriveType;
import com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor;

import java.util.List;
import java.util.function.Supplier;

/**
 * A type of machine. The interface behind {@link ComputerType} and
 * any addon-provided machine descriptors.
 *
 * <p>Every field of {@code ComputerType} appears here as a method.
 * The enum implements this interface so its values can be stored in
 * {@link MachineTypeRegistry} alongside addon types.
 *
 * @since 1.5
 */
public interface MachineType {
	/** @return unique identifier, e.g. {@code "mc_dos:ibm_pc_at"} */
	String id();

	/** @return the model name shown in the terminal window title. */
	String modelName();

	/** @return the CPU description. */
	String cpuName();

	/** @return the OS version string, returned by {@code VER}. */
	String osVersion();

	/** @return the bus type, e.g. {@code "ISA"} or {@code "PCI / AGP"}. */
	String busType();

	/** @return the terminal foreground color. */
	int textColor();

	/** 
	 * Lines the terminal prints after POST completes and before the shell prompt appears.
	 * Represents the operating system's own boot phase - the messages that come from
	 * the bootloader, the kernel, and the autoexec process.
	 *
	 * <p>Return an empty list for machines that hand off silently.
	 * Never include the shell prompt itself, the terminal draws that
	 * from {@link com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor#getPrompt}.
	 *
	 * @return the boot lines, in order; never {@code null} */
	default List<String> osBootLines() { return List.of(); }

	/** @return files to seed into the VFS at first boot. */
	List<String> defaultFiles();

	/** @return the shell and kernel factory for this machine. */
	ICommandProcessor commandProcessor();

	/** @return initial working directory. */
	String defaultPath();

	/** @return the BIOS firmware. */
	Bios bios();

	/** @return physical drive bays fitted to this machine. */
	List<DriveBaySpec> driveBays();

	/** @return factory for this machine's factory-default configuration. */
	Supplier<MachineConfig> defaultConfig();
	/**
	 * A drive bay fitted to a machine. Describes the bay's physical
	 * capabilities and the naming conventions the OS should use for it.
	 *
	 * @param type			  the drive type (floppy, CD, DVD, ...)
	 * @param dosLetter		  the DOS drive letter, or {@code null}
	 * @param posixDevice	  the POSIX device path, or {@code null}
	 * @param posixMountPoint the POSIX default mount point, or {@code null}
	 */
	record DriveBaySpec(DriveType type,
						String dosLetter,
						String posixDevice,
						String posixMountPoint) {}
}
