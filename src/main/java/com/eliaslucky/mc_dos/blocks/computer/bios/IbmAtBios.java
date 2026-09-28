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
 * The BIOS of an IBM Personal Computer AT (Model 5170), circa 1984-1987.
 *
 * <p>POST output matches the shape of what an actual AT printed: a
 * three-line IBM banner, the memory test line, hardware enumeration
 * for LPC-class devices, and the SETUP prompt. SETUP is reached by
 * pressing DEL and opens a black-and-white key/value table.
 *
 * <p>This BIOS reports the machine's {@link MachineConfig} verbatim —
 * floppy drives, hard disks, memory sizes, and display type all come
 * from the persisted configuration. Editing them in SETUP and saving
 * writes back through the block entity.
 *
 * @since 1.5
 */
public class IbmAtBios implements Bios {
	@Override
	public String name() { return "IBM Personal Computer AT"; }

	@Override
	public String version() { return "Version C1.00"; }
	@Override public String manufacturer() { return "IBM"; }
	@Override public String releaseDate()  { return "02/21/1987"; }
	@Override public String copyright()    { return "Copyright IBM Corp. 1981, 1984, 1986"; }
	@Override
	public List<String> runPost(ComputerBlockEntity machine, PeripheralBus bus, MachineConfig config) {
		List<String> out = new ArrayList<>();

		// Firmware banner
		out.add(name());
		out.add("IBM BIOS " + version());
		out.add("Copyright IBM Corp. 1981, 1984, 1986");
		out.add("");

		// Memory test
		int baseK = config.baseMemoryKb();
		out.add(String.format("%04d KB OK", baseK));
		out.add("");

		// Hardware enumeration
		List<PeripheralAddress> devices = bus.scan();
		if (!devices.isEmpty()) {
			out.add("Detected hardware:");
			for (PeripheralAddress addr : devices) {
				out.add(String.format("  %d:%02d  %-12s  %s",
						addr.slot(),
						0,
						addr.deviceClass().toUpperCase(),
						addr.vendorId()));
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
	public String setupPrompt() { return "Press DEL to enter SETUP"; }

	@Override
	public String setupScreenId() { return "IBM_AT_SETUP"; }
}
