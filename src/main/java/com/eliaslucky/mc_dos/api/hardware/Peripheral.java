package com.eliaslucky.mc_dos.api.hardware;

public interface Peripheral {
    /** Class identifier for auto-matching. e.g. "printer", "serial", "mccmd" */
    String deviceClass();

    /** Vendor / product identifiers, PCI-style. */
    String vendorId();       // "mc_dos"
    String productId();      // "mccmd_v1"
    
    /** Human-readable name for /proc/devices, DOS LIST, etc. */
    String description();

    /** Push bytes to the device. */
    void write(byte[] data);

    /** Pull up to maxBytes. Returns empty array if nothing available. */
    byte[] read(int maxBytes);

    /** Device-specific control call. Driver-defined command codes. */
    int ioctl(int cmd, byte[] arg);

    boolean isReady();
    boolean hasData();
}
