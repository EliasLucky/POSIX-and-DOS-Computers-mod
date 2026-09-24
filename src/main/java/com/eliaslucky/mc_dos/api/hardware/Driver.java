package com.eliaslucky.mc_dos.api.hardware;

public interface Driver {
    /** Called when the OS loads this driver. Returns whether to keep it loaded. */
    DriverInitResult init(DriverContext ctx);

    /** Called on shutdown / unload. */
    void shutdown();

    /** Human name for /proc/devices, DOS LIST, etc. */
    String name();
}
