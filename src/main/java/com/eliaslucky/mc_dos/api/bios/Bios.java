package com.eliaslucky.mc_dos.api.bios;

import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import java.util.List;

/**
 * The firmware layer of a machine. Runs before any operating system.
 *
 * <p>A BIOS has three states:
 * <ol>
 *   <li><b>POST</b> — power-on self test. Detects hardware, reports
 *       memory, prints the banner. Everything POST produces is a
 *       sequence of text lines.</li>
 *   <li><b>SETUP</b> — the configuration screen, reachable by pressing
 *       a BIOS-specific key during a short window at the end of POST.
 *       Reads and edits a {@link MachineConfig}.</li>
 *   <li><b>Handoff</b> — signalling to the machine that the OS can
 *       take over.</li>
 * </ol>
 *
 * <h2>Client vs server</h2>
 * The BIOS interface itself is common code and lives on both sides.
 * The SETUP screen is a TUI application that exists only on the
 * client, so the BIOS does not create it directly. Instead it returns
 * a {@linkplain #setupScreenId() screen ID} that the client resolves
 * through a registry. This mirrors the {@code APP_LAUNCH:} call
 * the terminal uses for applications
 * free of client imports.
 *
 * <h2>Per-machine variation</h2>
 * A 1984 IBM AT and a 2003 Award BIOS look nothing alike. Each
 * machine's {@code ICommandProcessor} picks the BIOS it wants from
 * {@link com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor#createBios()}.
 * Addons can create their own BIOS for a custom machine.
 *
 * @see MachineConfig
 * @since 1.5
 */
public interface Bios {
    /**
     * Human name shown at the top of POST.
     *
     * @return the machine name, e.g. {@code "IBM Personal Computer AT"}
     */
    String name();

    /**
     * Firmware version string, typically including a date.
     *
     * @return the version, e.g. {@code "Version C1.00"}
     */
    String version();
    String manufacturer();
    String releaseDate();
    String copyright();

    /**
     * Run power-on self test.
     *
     * <p>The returned lines are displayed by the terminal in order.
     * Implementations should produce output resembling what the real
     * firmware printed — memory test lines, hardware enumeration, and
     * the "Press DEL to enter SETUP" prompt.
     *
     * <p>The method runs on the server. It may read from {@code bus}
     * (to enumerate peripherals) and from {@code config} (for memory
     * size and display type), but should not mutate either.
     *
     * @param machine the block entity being powered on; never {@code null}
     * @param bus     the peripheral bus; never {@code null}
     * @param config  the persisted machine configuration; never {@code null}
     * @return an ordered list of text lines; may be empty, never {@code null}
     */
    List<String> runPost(ComputerBlockEntity machine, PeripheralBus bus, MachineConfig config);

    /**
     * The GLFW key that opens SETUP during POST. Common values:
     * {@code GLFW_KEY_DELETE} (IBM AT, Award), {@code GLFW_KEY_F2}
     * (later Phoenix), {@code GLFW_KEY_F1} (older machines).
     *
     * <p>The client polls this during the POST countdown. Returning
     * {@code -1} disables SETUP for this BIOS.
     *
     * @return the GLFW key code, or {@code -1} for none
     */
    int setupKeyCode();

    /**
     * The line printed during POST telling the user how to enter SETUP.
     *
     * @return the prompt, e.g. {@code "Press DEL to enter SETUP"}
     */
    String setupPrompt();

    /**
     * Identifier of the client-side SETUP screen for this BIOS.
     *
     * <p>Resolved by the client's BIOS setup registry. Addons register
     * their screens under matching IDs. If the ID has no registration,
     * pressing the setup key does nothing.
     *
     * @return the screen ID, e.g. {@code "IBM_AT_SETUP"}
     */
    String setupScreenId();
}
