package com.eliaslucky.mc_dos.api.hardware;

import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import java.util.List;

/**
 * An OS kernel. One instance per powered-on machine.
 * Owns device tables, runs boot sequences, and exposes a lookup that the
 * shell uses to route device-name commands.
 */
public interface Kernel {

    /** Boot the kernel. Called once when the machine powers on. */
    void boot(PeripheralBus bus, VirtualFileSystem vfs);

    /** Shut down cleanly. Called when the machine powers off or the type changes. */
    void shutdown();

    /** Boot messages to display on the terminal. */
    List<String> getBootLog();

    /** Device namespace — DOS's device chain, UNIX's /dev entries, etc. */
    DeviceLookup getDevices();
}
