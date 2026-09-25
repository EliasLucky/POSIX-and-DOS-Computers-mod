package com.eliaslucky.mc_dos.api.hardware;

/**
 * The outcome of {@link Driver#init(DriverContext)}.
 */
public enum DriverInitResult {
    /**
     * The driver bound to hardware and registered its devices. The
     * kernel keeps the driver loaded and will call {@link Driver#shutdown()}
     * when the machine powers off.
     */
    OK,

    /**
     * The driver could not find matching hardware, or bound but failed
     * to register a device name (e.g. the name was already taken). The
     * kernel drops the driver.
     *
     * <p>Common causes:
     * <ul>
     *   <li>No peripheral of the expected {@link Peripheral#deviceClass()}
     *       is adjacent to the computer.</li>
     *   <li>The device name was claimed by an earlier driver.</li>
     *   <li>A {@code SLOT} parameter in {@code CONFIG.SYS} pointed at a
     *       slot with no peripheral.</li>
     * </ul>
     */
    FAILED,

    /**
     * The driver needs more time before it can finish initialization.
     * Reserved for future use — currently treated as {@link #FAILED} by
     * all kernels.
     */
    DEFERRED
}