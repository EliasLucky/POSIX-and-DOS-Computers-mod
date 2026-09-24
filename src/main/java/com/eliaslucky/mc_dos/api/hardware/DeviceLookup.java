package com.eliaslucky.mc_dos.api.hardware;

import java.util.List;

/**
 * Read-only view of a kernel's device namespace. The shell calls this
 * to decide whether a typed command names a device. It never knows
 * which devices exist — that's the kernel's business.
 */
public interface DeviceLookup {

    boolean isDevice(String name);

    /** Returns the handler or null if not a device. */
    DeviceHandler lookup(String name);

    /** Names visible to the shell, for HELP listings. */
    List<String> names();
}
