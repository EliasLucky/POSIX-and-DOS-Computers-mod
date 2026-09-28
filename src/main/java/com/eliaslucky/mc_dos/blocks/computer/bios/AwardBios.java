package com.eliaslucky.mc_dos.blocks.computer.bios;

import com.eliaslucky.mc_dos.api.bios.Bios;
import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.api.hardware.PeripheralAddress;
import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * Phoenix / Award BIOS v6.00PG, circa 2001-2005. The firmware on the
 * mod's Pentium 4 machine.
 *
 * <p>POST output is the modern style: CPU line, memory test, IDE
 * device enumeration, then a two-key prompt for SETUP and boot menu.
 * The SETUP screen is the blue Award interface with a top menu bar
 * (Main, Advanced, IO, Boot, Save &amp; Exit).
 *
 * @since 1.5
 */
public class AwardBios implements Bios {
	@Override
	public String name() { return "Phoenix - AwardBIOS v6.00PG"; }

	@Override
	public String version() { return "v6.00PG"; }
	@Override public String manufacturer() { return "Phoenix Technologies, LTD"; }
	@Override public String releaseDate()  { return "05/14/2003"; }
	@Override public String copyright()    { return "Copyright (C) 1984-2003, Phoenix Technologies, LTD"; }

	@Override
	public List<String> runPost(ComputerBlockEntity machine, PeripheralBus bus, MachineConfig config) {
		List<String> out = new ArrayList<>();

		// Firmware banner
		out.add(name());
		out.add("Copyright (C) 1984-2003, Phoenix Technologies, LTD");
		out.add("An Energy Star Ally");
		out.add("");

		// CPU identification
		out.add("Main Processor   : " + cpuName(machine));
		out.add(String.format("Memory Test		: %dK OK", config.baseMemoryKb() + config.extendedMemoryKb()));
		out.add("");

		// IDE enumeration
		out.add("IDE Channel 0 Master : " +
				(config.hardDisk1() == MachineConfig.DiskType.NONE
						? "None"
						: "ST340014A"));
		out.add("IDE Channel 0 Slave  : None");
		out.add("IDE Channel 1 Master : " +
				(config.floppyA() == MachineConfig.FloppyType.NONE
						? "None"
						: "Floppy 1.44M"));
		out.add("IDE Channel 1 Slave  : None");
		out.add("");

		// Detected add-in devices
		List<PeripheralAddress> devices = bus.scan();
		if (!devices.isEmpty()) {
			out.add("Onboard Devices:");
			for (PeripheralAddress addr : devices) {
				out.add(String.format("  %s %s at %d:%02d",
						addr.vendorId(),
						addr.deviceClass().toUpperCase(),
						addr.slot(),
						0));
			}
			out.add("");
		}

		// SETUP prompt
		out.add(setupPrompt());

		return out;
	}

	@Override
	public int setupKeyCode() { return GLFW.GLFW_KEY_DELETE; }

	@Override
	public String setupPrompt() { return "Press DEL to enter SETUP, F12 for Boot Menu"; }

	@Override
	public String setupScreenId() { return "AWARD_SETUP"; }

	// Helpers
	private static String cpuName(ComputerBlockEntity machine) {
		// A real BIOS reads CPUID;
		if (machine == null || machine.getMachineType() == null) return "Generic x86";
		return machine.getMachineType().cpuName();
	}
}
