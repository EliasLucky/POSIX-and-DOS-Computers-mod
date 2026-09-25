package com.eliaslucky.mc_dos.api.hardware;

/**
 * A block that can be attached to a computer as a device.
 *
 * <p>Implementations are OS-agnostic. A {@code Peripheral} exposes a
 * byte stream ({@link #write(byte[])}, {@link #read(int)}) and a
 * control channel ({@link #ioctl(int, byte[])}). The driver layer
 * adapts it to whatever device model the running OS uses.
 *
 * <p>Typical implementation is a {@code BlockEntity}. The bus scans
 * blocks adjacent to the computer and surfaces any that implement
 * this interface.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class PrinterBlockEntity extends BlockEntity implements Peripheral {
 *     @Override public String deviceClass() { return "printer"; }
 *     @Override public String vendorId()    { return "myaddon"; }
 *     @Override public String productId()   { return "printer_v1"; }
 *     @Override public String description() { return "Thermal printer"; }
 *     @Override public void write(byte[] data) { buffer.addLast(data); }
 *     @Override public byte[] read(int n)      { return new byte[0]; }
 *     @Override public int ioctl(int c, byte[] a) { return -1; }
 *     @Override public boolean isReady()       { return true; }
 *     @Override public boolean hasData()       { return false; }
 * }
 * }</pre>
 */
public interface Peripheral {
	/**
     * The hardware class. Used by drivers to find matching hardware.
     * Convention: lowercase, short, no spaces. Examples: {@code "mccmd"},
     * {@code "printer"}, {@code "serial"}.
     *
     * @return the device class identifier, never {@code null}
     */
    String deviceClass();

    /**
     * Vendor identifier. Convention: the mod id of the addon that
     * provides the peripheral.
     *
     * @return the vendor id, never {@code null}
     */
    String vendorId();       // "mc_dos"
    /**
     * Product identifier. Distinguishes hardware revisions.
     *
     * @return the product id, never {@code null}
     */
    String productId();      // "mccmd_v1"
    
    /**
     * Human-readable description shown in device listings.
     *
     * @return a description, never {@code null}
     */
    String description();

    /**
     * Push data to the device. Non-blocking.
     *
     * @param data the bytes to send; never {@code null}, may be empty
     */
    void write(byte[] data);

    /**
     * Pull up to {@code maxBytes} of pending output. Returns an empty
     * array if nothing is available.
     *
     * @param maxBytes the maximum number of bytes to read; must be positive
     * @return the bytes read, possibly empty, never {@code null}
     */
    byte[] read(int maxBytes);

    /**
     * Device-specific control call. Command codes are defined by the
     * driver that owns the device.
     *
     * @param cmd the command code
     * @param arg argument bytes, may be {@code null}
     * @return a device-defined result code, or {@code -1} on failure
     */
    int ioctl(int cmd, byte[] arg);

    /**
     * Whether the peripheral is ready to accept I/O. A block entity
     * typically returns {@code false} on the client side.
     *
     * @return {@code true} if the device can process bytes
     */
    boolean isReady();
    /**
     * Whether there is pending output to read.
     *
     * @return {@code true} if {@link #read(int)} would return data
     */
    boolean hasData();
}
