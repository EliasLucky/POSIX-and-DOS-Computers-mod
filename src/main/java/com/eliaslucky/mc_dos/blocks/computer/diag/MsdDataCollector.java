package com.eliaslucky.mc_dos.blocks.computer.diag;

import com.eliaslucky.mc_dos.api.bios.Bios;
import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.hardware.PeripheralAddress;
import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.MachineType;
import com.eliaslucky.mc_dos.blocks.computer.bus.AdjacentBlocksBus;

import java.util.List;
import java.util.Locale;

/**
 * Gathers the diagnostic data that MSD displays, from the server side,
 * and packages it into a sectioned text format.
 *
 * <p>Every fact reported here comes from one of five sources:
 * <ul>
 *	 <li>{@link ComputerType} — model, CPU, bus type</li>
 *	 <li>{@link Bios} — firmware manufacturer, version, date</li>
 *	 <li>{@link MachineConfig} — drives, memory, display</li>
 *	 <li>{@link Kernel} — loaded device drivers</li>
 *	 <li>{@link PeripheralBus} — attached hardware</li>
 * </ul>
 *
 * <h2>Output format</h2>
 * <p>The report is a single string. Sections begin with a line of the
 * form {@code "=== SECTION NAME ==="} and end at the next section
 * marker or at end of string. The client-side {@code MsdApplication}
 * parses this back into per-category lists for rendering.
 *
 * <p>Facts are line-oriented within each section:
 * {@code "  Label:	value"} with the label padded to
 * column 24.
 *
 * @since 1.5
 */
public final class MsdDataCollector {
	/** Horizontal rule width used inside the sections. */
	private static final int SECTION_RULE_WIDTH = 54;

	private MsdDataCollector() {}

	/**
	 * Collect the full diagnostic report for a machine.
	 *
	 * @param computer the block entity to inspect; never {@code null}
	 * @return a sectioned report
	 */
	public static String collect(ComputerBlockEntity computer) {
		MachineType type = computer.getMachineType();
		Bios bios = type.bios();
		MachineConfig config = computer.getMachineConfig();
		Kernel kernel = computer.getKernel();
		PeripheralBus bus = new AdjacentBlocksBus(
				computer.getLevel(), computer.getBlockPos());
		List<PeripheralAddress> devices = bus.scan();

		StringBuilder out = new StringBuilder();

		// COMPUTER
		section(out, "COMPUTER");
		line(out, "Computer Name", type.modelName());
		line(out, "Processor", type.cpuName());
		line(out, "BIOS Manufacturer", bios.manufacturer());
		line(out, "BIOS Version", bios.version());
		line(out, "BIOS Date", bios.releaseDate());
		line(out, "Bus Type", type.busType());
		line(out, "Keyboard Type", "101-key Enhanced");
		line(out, "Math Coprocessor",
				config != null && config.mathCoprocessor()
						? "Installed" : "Not installed");

		// MEMORY
		section(out, "MEMORY");
		int baseKb = config != null ? config.baseMemoryKb() : 640;
		int extKb  = config != null ? config.extendedMemoryKb() : 0;
		line(out, "Total Conventional", baseKb + " K");
		line(out, "Total Extended", extKb + " K");
		line(out, "Total Expanded", "0 K");
		line(out, "Available to MS-DOS", (baseKb - 35) + " K");
		// The 35 KB deduction matches what DOS reported: the kernel
		// and COMMAND.COM occupied that much of conventional memory.

		// VIDEO
		section(out, "VIDEO");
		MachineConfig.DisplayType display = config != null
				? config.primaryDisplay()
				: MachineConfig.DisplayType.SPECIAL_EGA;
		line(out, "Video Adapter", display.displayName());
		line(out, "Video Mode", "80 x 25 Color Text");
		line(out, "Video BIOS", "IBM EGA BIOS");
		line(out, "Video Memory", "256 K");
		// Video BIOS strings would come from the display adapter in a
		// full model; for now they match the assumed EGA card.

		// NETWORK
		section(out, "NETWORK");
		// The mod doesn't ship a network adapter yet. This section is
		// the placeholder that a future NIC would populate.
		line(out, "Network Adapter", "None detected");
		line(out, "Network Driver", "None");

		// OS VERSION
		section(out, "OS VERSION");
		line(out, "Operating System", type.osVersion());
		line(out, "Boot Drive", "C:");
		line(out, "MS-DOS Location", "HMA");

		// MOUSE
		section(out, "MOUSE");
		boolean hasMouse = kernel != null && kernel.getDevices().isDevice("MOUSE");
		line(out, "Mouse Driver", hasMouse ? "Installed" : "Not installed");

		// OTHER ADAPTERS
		section(out, "OTHER ADAPTERS");
		line(out, "Game Adapter", "Not installed");
		line(out, "Joystick 1", "N/A");
		line(out, "Joystick 2", "N/A");

		// DISK DRIVES
		section(out, "DISK DRIVES");
		if (config != null) {
			line(out, "A:", config.floppyA().displayName());
			line(out, "B:", config.floppyB().displayName());
			line(out, "C:", "Fixed Disk" + config.hardDisk1().displayName());
			if (config.hardDisk2() != MachineConfig.DiskType.NONE) {
				line(out, "D:", "Fixed Disk  " + config.hardDisk2().displayName());
			}
		} else {
			out.append("  No drives reported.\n");
		}
		out.append('\n');
		line(out, "Fixed Disk Controller", "IDE");
		line(out, "Floppy Disk Controller",
				(config != null
						&& (config.floppyA() != MachineConfig.FloppyType.NONE
						 || config.floppyB() != MachineConfig.FloppyType.NONE))
						? "Standard" : "None installed");

		// LPT PORTS
		section(out, "LPT PORTS");
		for (String l : IrqCatalog.lptContent(kernel)) out.append("  ").append(l).append("\n");

		// COM PORTS
		section(out, "COM PORTS");
		for (String l : IrqCatalog.comContent(kernel)) out.append("  ").append(l).append("\n");

		// IRQ STATUS
		section(out, "IRQ STATUS");
		for (String l : IrqCatalog.irqContent(config, kernel, bus)) {
			out.append("  ").append(l).append("\n");
		}

		// TSR PROGRAMS
		section(out, "TSR PROGRAMS");
		// The mod has no TSR concept; a real machine reported every
		// program that hooked an interrupt. We show only the built-ins.
		out.append("  Program            Address   Size\n");
		out.append("  -----------------  --------  ----\n");
		line(out, "COMMAND.COM", "       --        0 K");
		line(out, "HIMEM.SYS", "       --        0 K");

		// DEVICE DRIVERS
		section(out, "DEVICE DRIVERS");
		if (kernel == null) {
			out.append("  No kernel loaded.\n");
		} else {
			List<String> names = kernel.getDevices().names();
			if (names.isEmpty()) {
				out.append("  No device drivers loaded.\n");
			} else {
				out.append("  Name        Description\n");
				out.append("  ----------  ------------------------------------\n");
				for (String name : names) {
					String desc = kernel.getDevices().lookup(name).description();
					out.append(String.format("  %-10s  %s\n", name, desc));
				}
			}
		}

		// LPC BUS
		section(out, "LPC BUS");
		if (devices.isEmpty()) {
			out.append("  No devices detected.\n");
		} else {
			out.append("  Slot  Class         Vendor      Product         Status\n");
			out.append("  ----  ------------  ----------  --------------  ----------------\n");
			for (PeripheralAddress a : devices) {
				String devName = a.deviceClass().toUpperCase(Locale.ROOT);
				boolean loaded = kernel != null && kernel.getDevices().isDevice(devName);
				out.append(String.format("  %-4d  %-12s  %-10s  %-14s  %s\n",
						a.slot(),
						a.deviceClass(),
						a.vendorId(),
						a.productId(),
						loaded ? "Driver loaded" : "No driver"));
			}
		}

		// CONFIG.SYS SUGGESTIONS
		// If any attached hardware has no matching driver loaded, tell
		// the user exactly what to add to CONFIG.SYS.
		boolean anyUnclaimed = false;
		for (PeripheralAddress a : devices) {
			String devName = a.deviceClass().toUpperCase(Locale.ROOT);
			boolean loaded = kernel != null && kernel.getDevices().isDevice(devName);
			if (loaded) continue;
			if (!anyUnclaimed) {
				out.append("\n");
				out.append("=== CONFIG.SYS SUGGESTIONS ===\n");
				out.append("To load a driver for unclaimed hardware, add\n");
				out.append("the following line(s) to C:\\CONFIG.SYS:\n\n");
				anyUnclaimed = true;
			}
			out.append("  DEVICE=C:\\DRIVERS\\")
			   .append(devName)
			   .append(".SYS /SLOT=")
			   .append(a.slot())
			   .append("\n");
		}

		return out.toString();
	}

	// Formatting helpers

	/**
	 * Emit a section header. Subsequent lines belong to this section
	 * until the next header or end of string.
	 *
	 * @param out	 the output buffer
	 * @param name	 the section name, uppercase by convention
	 */
	private static void section(StringBuilder out, String name) {
		out.append("\n=== ").append(name).append(" ===\n");
	}

	/**
	 * Emit a labelled line. The label is followed by a colon and
	 * padded so all values start at the same column.
	 *
	 * @param out	the output buffer
	 * @param label the label text
	 * @param value the value text
	 */
	private static void line(StringBuilder out, String label, String value) {
		out.append(String.format("  %-22s%s\n", label + ":", value));
	}
}
