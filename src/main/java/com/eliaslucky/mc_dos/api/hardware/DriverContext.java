package com.eliaslucky.mc_dos.api.hardware;

import java.util.Map;

public interface DriverContext {
    PeripheralBus bus();

    /** Params passed at load time. DOS: from CONFIG.SYS. Linux: from modprobe. */
    Map<String, String> loadParams();

    /**
     * Register a device in the OS's namespace.
     * DOS:  name = "MCCMD" → accessible as a device name
     * UNIX: name = "mccmd" → /dev/mccmd, major/minor allocated by kernel
     * Linux: baseName = "mccmd" → /dev/mccmd0, /dev/mccmd1, ...
     *
     * Returns the actual name allocated (may include an index).
     */
    String registerDevice(String name, DeviceHandler handler);

    /** Boot-time log. DOS prints to the console; Linux appends to dmesg. */
    void log(String message);
}