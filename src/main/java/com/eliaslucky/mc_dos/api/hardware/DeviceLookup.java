package com.eliaslucky.mc_dos.api.hardware;

import java.util.List;

/**
 * A read-only view of a kernel's device namespace. The shell uses this
 * to decide whether a typed command names a device (e.g. {@code MCCMD}
 * on DOS, {@code /dev/lp0} on Linux).
 *
 * <p>Drivers do not implement this. They call
 * {@link DriverContext#registerDevice} and the kernel's device table
 * does the lookup.
 *
 * @see Kernel#getDevices()
 */
public interface DeviceLookup {
    /**
     * Whether a name is a registered device.
     *
     * @param name the name to check; matching is case-insensitive on DOS
     * @return {@code true} if the name is registered
     */
    boolean isDevice(String name);

    /**
     * Resolve a device name to its handler.
     *
     * @param name the device name
     * @return the handler, or {@code null} if not registered
     */
    DeviceHandler lookup(String name);

    /**
     * All registered device names, for {@code HELP} listings and
     * directory enumeration.
     *
     * @return an immutable list, possibly empty
     */
    List<String> names();
}