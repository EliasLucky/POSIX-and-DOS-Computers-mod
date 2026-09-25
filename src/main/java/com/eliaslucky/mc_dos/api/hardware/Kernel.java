package com.eliaslucky.mc_dos.api.hardware;

import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import java.util.List;

/**
 * The lowest layer of an operating system. Owns device tables, runs
 * the boot sequence, and exposes a lookup that the shell uses to
 * route device-name commands.
 *
 * <p>One kernel per powered-on machine. Created by
 * {@link com.eliaslucky.mc_dos.blocks.computer.processors.ICommandProcessor#createKernel}
 * at boot.
 */
public interface Kernel {

	/**
     * Boot the kernel. Called once when the machine powers on.
     *
     * @param bus the peripheral bus for this computer; never {@code null}
     * @param vfs the machine's virtual filesystem; never {@code null}
     */
    void boot(PeripheralBus bus, VirtualFileSystem vfs);

    /**
     * Shut down cleanly. Called when the machine powers off or the
     * computer type changes. Must not throw.
     */
    void shutdown();

    /**
     * Boot messages to display on the terminal. Rendered after the
     * BIOS banner, before the shell prompt.
     *
     * @return an ordered list of log lines, possibly empty, never {@code null}
     */
    List<String> getBootLog();

    /**
     * Device namespace. Used by the shell to check whether a typed
     * command names a device, and by {@code StreamResolver} to
     * resolve redirection targets.
     *
     * @return the device lookup, never {@code null}
     */
    DeviceLookup getDevices();
}
