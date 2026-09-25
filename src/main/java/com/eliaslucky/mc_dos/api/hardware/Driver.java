package com.eliaslucky.mc_dos.api.hardware;

/**
 * OS-side code that adapts a {@link Peripheral} to that OS's device model.
 *
 * <p>A driver is the bridge between hardware and software. The hardware
 * exposes bytes ({@link Peripheral#write(byte[])}, {@link Peripheral#read(int)});
 * the OS exposes a named device that applications can open. The driver
 * translates between the two and, during {@link #init(DriverContext)},
 * declares the device to the kernel.
 *
 * <h2>One driver per (OS family, hardware class)</h2>
 * Every OS family handles device discovery differently. The same piece
 * of hardware therefore needs a driver per family:
 * <ul>
 *   <li><b>DOS</b> — loads from {@code DEVICE=} lines in {@code CONFIG.SYS},
 *       registers a bare 1–8 character name ({@code MCCMD}, {@code PRN}).</li>
 *   <li><b>UNIX</b> — compiled into the kernel; registers a {@code /dev}
 *       path with a major/minor pair.</li>
 *   <li><b>Linux</b> — a loadable module; matches by device class on the
 *       bus and registers {@code /dev/name0}, {@code /dev/name1}, ...</li>
 * </ul>
 * Do <em>not</em> try to share one driver class across families. The
 * byte plumbing is identical ({@link DeviceHandler#of(Peripheral)} handles
 * it), but the naming and loading conventions differ enough that a shared
 * driver becomes unreadable.
 *
 * <h2>Registering</h2>
 * Register your driver in mod setup, one entry per family:
 * <pre>{@code
 * DriverRegistry.register("dos",   "MCCMD", DosMccmdDriver::new);
 * DriverRegistry.register("unix",  "MCCMD", UnixMccmdDriver::new);
 * DriverRegistry.register("linux", "MCCMD", LinuxMccmdDriver::new);
 * }</pre>
 * The name (second argument) is the family-specific identifier: for DOS
 * it is the {@code .SYS} basename, for Linux the module name, and for
 * UNIX the C source basename. See the kernel's loader for the exact key
 * format.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Mod setup: {@link DriverRegistry#register} records the factory.</li>
 *   <li>Boot: the kernel calls {@link DriverRegistry#load} and then
 *       {@link #init(DriverContext)}.</li>
 *   <li>Running: the driver owns a {@link DeviceHandler}, and the shell
 *       routes commands to it.</li>
 *   <li>Shutdown: the kernel calls {@link #shutdown()}.</li>
 * </ol>
 *
 * @see DriverContext
 * @see DriverInitResult
 * @see DriverRegistry
 * @see DeviceHandler#of(Peripheral)
 */
public interface Driver {
	/**
     * Load the driver into a running kernel.
     *
     * <p>The driver should:
     * <ol>
     *   <li>Scan the bus for matching hardware
     *       ({@link DriverContext#bus()}.{@link PeripheralBus#scan()}).</li>
     *   <li>Bind to one or more {@link Peripheral} instances.</li>
     *   <li>Wrap each with {@link DeviceHandler#of(Peripheral)}.</li>
     *   <li>Register each via {@link DriverContext#registerDevice}.</li>
     *   <li>Log significant events with {@link DriverContext#log}.</li>
     * </ol>
     *
     * <p>If no matching hardware is present, return
     * {@link DriverInitResult#FAILED} and let the kernel unload the
     * driver. Do not leave half-bound state.
     *
     * @param ctx the driver's runtime environment; never {@code null}
     * @return {@link DriverInitResult#OK} if the driver is now active,
     *         {@link DriverInitResult#FAILED} otherwise
     */
    DriverInitResult init(DriverContext ctx);

    /**
     * Release any state held by the driver. Called on kernel shutdown,
     * world unload, or computer type change.
     *
     * <p>Implementations should drop peripheral references and clear any
     * collections, but should <em>not</em> try to unregister devices —
     * the kernel clears its device table itself.
     *
     * <p>Must not throw. Log errors if something goes wrong.
     */
    void shutdown();

    /**
     * Human-readable name. Convention by family:
     * <ul>
     *   <li>DOS — {@code "MCCMD.SYS"}</li>
     *   <li>UNIX — {@code "mccmd.c"}</li>
     *   <li>Linux — {@code "mccmd.ko"}</li>
     * </ul>
     *
     * @return the driver name, never {@code null}
     */
    String name();
}
