package com.eliaslucky.mc_dos.api.hardware;

import java.util.Map;

/**
 * The environment a {@link Driver} runs in. One instance is created per
 * driver per boot; drivers should not retain it beyond {@link Driver#init}.
 *
 * <p>Everything a driver needs during initialization is reachable
 * through this interface:
 * <ul>
 *   <li>The {@linkplain #bus() peripheral bus} for scanning hardware.</li>
 *   <li>The {@linkplain #loadParams() load parameters} for OS-specific
 *       configuration ({@code SLOT=0}, {@code IRQ=5}, ...).</li>
 *   <li>{@link #registerDevice} to declare the device to the kernel.</li>
 *   <li>{@link #log} for boot-time diagnostics.</li>
 * </ul>
 *
 * <h2>Load parameters by OS family</h2>
 * <ul>
 *   <li><b>DOS</b> — parsed from the {@code DEVICE=} line in
 *       {@code CONFIG.SYS}. Example:
 *       {@code DEVICE=C:\DRIVERS\MCCMD.SYS /SLOT=0 /IRQ=5} yields
 *       {@code {"SLOT":"0", "IRQ":"5"}}.</li>
 *   <li><b>UNIX</b> — always empty. Drivers were compiled into the
 *       kernel; there were no runtime parameters.</li>
 *   <li><b>Linux</b> — populated from the module's {@code modprobe}
 *       arguments. Not yet implemented.</li>
 * </ul>
 *
 * @see Driver#init(DriverContext)
 */
public interface DriverContext {
    /**
     * The peripheral bus of the machine being booted. Use this to scan
     * for hardware.
     *
     * @return the bus, never {@code null}
     */
    PeripheralBus bus();

    /**
     * Parameters passed at load time. Keys are uppercased. The map may
     * be empty but is never {@code null}.
     *
     * @return an unmodifiable map of load parameters
     */
    Map<String, String> loadParams();

    /**
     * Register a device with the kernel's device table.
     *
     * <p>The naming convention depends on the family:
     * <ul>
     *   <li><b>DOS</b> — 1–8 character uppercase bare name, no
     *       extension, not in the reserved set ({@code CON}, {@code PRN},
     *       {@code NUL}, {@code LPT1}.., {@code COM1}..). Example:
     *       {@code "MCCMD"}.</li>
     *   <li><b>UNIX</b> — a path under {@code /dev}. The kernel allocates
     *       a major number automatically. Example: {@code "mccmd"} becomes
     *       {@code /dev/mccmd}.</li>
     *   <li><b>Linux</b> — a base name; the kernel appends a per-class
     *       index. Example: {@code "mccmd"} becomes {@code /dev/mccmd0},
     *       {@code /dev/mccmd1}, ...</li>
     * </ul>
     *
     * @param name    the requested device name or base name
     * @param handler the handler that will service reads and writes
     * @return the actual registered name (may differ from {@code name}
     *         if the kernel added an index), or {@code null} on failure
     *         (name conflict, reserved name, invalid length)
     */
    String registerDevice(String name, DeviceHandler handler);

    /**
     * Log a boot-time message. Where it appears depends on the family:
     * DOS prints to the terminal above the prompt; UNIX and Linux append
     * to a kernel log that is shown during boot.
     *
     * @param message the message; do not include a trailing newline
     */
    void log(String message);
}