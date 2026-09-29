package com.eliaslucky.mc_dos.blocks.computer.diag;

import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.hardware.PeripheralAddress;
import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the IRQ table and port listings a diagnostic tool like MSD
 * would print.
 *
 * <p>Every PC-compatible machine has the same fixed assignments for
 * IRQs 0, 1, 2, 8, and 13. Those lines are wired into the chipset and
 * the operating system depends on them being where they are. Every
 * other IRQ line is claimed by whatever hardware is actually
 * installed: a machine with no floppy drive leaves IRQ 6 free, a
 * machine with one claims it for the floppy controller.
 *
 * <p>Sources of truth, in order of precedence:
 * <ul>
 *	 <li>{@link MachineConfig} — drives, math coprocessor</li>
 *	 <li>{@link Kernel} — installed device drivers ({@code COM1},
 *		 {@code COM2}, {@code PRN})</li>
 *	 <li>{@link PeripheralBus} — attached LPC-class hardware</li>
 * </ul>
 *
 * @see MsdDataCollector
 * @since 1.5
 */
public final class IrqCatalog {
	private IrqCatalog() {}

	// IRQ table

	/**
	 * Build the current IRQ table for a machine.
	 *
	 * <p>The returned map is ordered by IRQ number, from 0 to 15.
	 * Every IRQ has a non-null description. Unclaimed lines read as
	 * {@code "Available"} rather than being omitted, matching how real
	 * diagnostic tools presented the full table.
	 *
	 * @param config the machine's BIOS configuration; may be {@code null}
	 * @param kernel the running kernel; may be {@code null}
	 * @param bus	 the peripheral bus; never {@code null}
	 * @return an ordered map from IRQ number to description
	 */
	public static Map<Integer, String> build(MachineConfig config, Kernel kernel, PeripheralBus bus) {
		Map<Integer, String> irqs = new LinkedHashMap<>();

		// Fixed base IRQs
		// These three are hardwired into the chipset.
		irqs.put(0,  "System Timer");
		irqs.put(1,  "Keyboard");
		irqs.put(2,  "Cascade from IRQ 9");

		// Hardware-dependent IRQs
		boolean hasFloppy = config != null
				&& (config.floppyA() != MachineConfig.FloppyType.NONE
				 || config.floppyB() != MachineConfig.FloppyType.NONE);
		boolean hasHardDisk = config != null
				&& config.hardDisk1() != MachineConfig.DiskType.NONE;
		boolean hasCoprocessor = config != null && config.mathCoprocessor();

		// COM ports: claimed only if the kernel registered a driver.
		boolean hasCOM2 = kernel != null && kernel.getDevices().isDevice("COM2");
		boolean hasCOM1 = kernel != null && kernel.getDevices().isDevice("COM1");
		boolean hasLPT1 = kernel != null && kernel.getDevices().isDevice("PRN");

		irqs.put(3,  hasCOM2 ? "COM2" : "Available");
		irqs.put(4,  hasCOM1 ? "COM1" : "Available");
		irqs.put(5,  "Available");							  // free — usually sound/LPT2
		irqs.put(6,  hasFloppy ? "Floppy Disk Controller"  : "Available");
		irqs.put(7,  hasLPT1   ? "LPT1"	                   : "Available");
		irqs.put(8,  "Real-Time Clock");
		irqs.put(9,  "Available");							  // often cascades to a card
		irqs.put(10, "Available");
		irqs.put(11, "Available");
		irqs.put(12, "Available");
		irqs.put(13, hasCoprocessor ? "Math Coprocessor"	  : "Available");
		irqs.put(14, hasHardDisk    ? "Fixed Disk Controller"     : "Available");
		irqs.put(15, "Available");

		// Attached peripherals claim the remaining IRQs
		// We hand out the first free slots in order. Real hardware used
		// jumper settings or PCI autoconfig to do this; the mod simply
		// assigns in scan order.
		int[] freeIrqs = { 5, 9, 10, 11, 12, 15 };
		int nextFree = 0;
		for (PeripheralAddress addr : bus.scan()) {
			if (nextFree >= freeIrqs.length) break;
			String label = addr.deviceClass().toUpperCase(Locale.ROOT) + " adapter";
			irqs.put(freeIrqs[nextFree++], label);
		}

		return irqs;
	}

	/**
	 * Format the IRQ table as a list of display lines, one per IRQ.
	 * Used by {@link MsdDataCollector}'s IRQ STATUS section.
	 *
	 * @param config the machine's BIOS configuration; may be {@code null}
	 * @param kernel the running kernel; may be {@code null}
	 * @param bus	 the peripheral bus; never {@code null}
	 * @return formatted lines in IRQ order
	 */
	public static List<String> irqContent(MachineConfig config, Kernel kernel, PeripheralBus bus) {
		List<String> out = new ArrayList<>();
		for (Map.Entry<Integer, String> e : build(config, kernel, bus).entrySet()) {
			out.add(String.format("IRQ %-2d  %s", e.getKey(), e.getValue()));
		}
		return out;
	}

	// LPT ports

	/**
	 * LPT (parallel port) listing.
	 *
	 * <p>A machine only has LPT1 if a printer driver is loaded. LPT2
	 * and LPT3 required extra hardware in real life, so they are almost
	 * always "Not installed" — matching MSD's output on most machines.
	 *
	 * @param kernel the running kernel; may be {@code null}
	 * @return formatted lines for the LPT PORTS section
	 */
	public static List<String> lptContent(Kernel kernel) {
		List<String> out = new ArrayList<>();
		boolean hasLPT1 = kernel != null && kernel.getDevices().isDevice("PRN");

		if (hasLPT1) {
			out.add("LPT1:	 I/O Port 0378h");
			out.add("        Status: Ready");
		} else {
			out.add("LPT1:   Not installed");
		}
		out.add("");
		out.add("LPT2:   Not installed");
		out.add("LPT3:   Not installed");
		return out;
	}

	// COM ports

	/**
	 * COM (serial port) listing.
	 *
	 * <p>A machine has COM1 and/or COM2 if matching drivers are
	 * registered. Real machines defaulted COM1 to I/O port {@code 03F8h}
	 * and IRQ 4, COM2 to {@code 02F8h} and IRQ 3. COM3 and COM4 needed
	 * additional hardware and shared IRQs with the first pair, which
	 * caused conflicts — the mod doesn't model them.
	 *
	 * @param kernel the running kernel; may be {@code null}
	 * @return formatted lines for the COM PORTS section
	 */
	public static List<String> comContent(Kernel kernel) {
		List<String> out = new ArrayList<>();
		boolean hasCOM1 = kernel != null && kernel.getDevices().isDevice("COM1");
		boolean hasCOM2 = kernel != null && kernel.getDevices().isDevice("COM2");

		if (hasCOM1) {
			out.add("COM1:	 I/O Port 03F8h  IRQ 4");
			out.add("        9600 Baud, 8-N-1");
			out.add("        Status: Available");
		} else {
			out.add("COM1:   Not installed");
		}
		out.add("");

		if (hasCOM2) {
			out.add("COM2:   I/O Port 02F8h  IRQ 3");
			out.add("        9600 Baud, 8-N-1");
			out.add("        Status: Available");
		} else {
			out.add("COM2:   Not installed");
		}
		out.add("");
		out.add("COM3:   Not installed");
		out.add("COM4:   Not installed");
		return out;
	}
}
