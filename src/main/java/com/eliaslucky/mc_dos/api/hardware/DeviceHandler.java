package com.eliaslucky.mc_dos.api.hardware;

/**
 * The runtime side of a registered device. Applications reach the
 * peripheral through this interface; it hides the driver that owns
 * the binding.
 *
 * <p>Almost all drivers should use {@link #of(Peripheral)} to construct
 * a handler. Custom handlers exist only when the driver needs to
 * transform data on the way through — for example, wrapping bytes
 * with a line protocol or maintaining a scrollback buffer.
 */
public interface DeviceHandler {
	/**
     * Called when an application writes to the device.
     *
     * @param data the bytes written; never {@code null}
     */
    void onWrite(byte[] data);

    /**
     * Called when an application reads from the device.
     *
     * @param maxBytes maximum bytes to return
     * @return the bytes read, possibly empty, never {@code null}
     */
    byte[] onRead(int maxBytes);

    /**
     * Device-specific control.
     *
     * @param cmd the command code
     * @param arg argument bytes, may be {@code null}
     * @return a device-defined result code, or {@code -1} on failure
     */
    int onIoctl(int cmd, byte[] arg);
    /**
     * @return {@code true} if {@link #onRead(int)} would return data
     */
    boolean hasData();

    /**
     * @return a human-readable description, never {@code null}
     */
    String description();
    
    /**
     * Adapts a {@link Peripheral} to a {@link DeviceHandler} with a
     * 1:1 byte pass-through. This is what almost every driver uses.
     *
     * <p>The returned handler is safe to call when the peripheral
     * is {@code null}; I/O becomes a no-op and reads return empty.
     *
     * @param peripheral the peripheral to wrap; may be {@code null}
     * @return a handler delegating to the peripheral
     */
    static DeviceHandler of(Peripheral peripheral) {
        return new DeviceHandler() {
            @Override public void onWrite(byte[] data) {
                if (peripheral != null) peripheral.write(data);
            }
            @Override public byte[] onRead(int maxBytes) {
                return peripheral == null ? new byte[0] : peripheral.read(maxBytes);
            }
            @Override public int onIoctl(int cmd, byte[] arg) {
                return peripheral == null ? -1 : peripheral.ioctl(cmd, arg);
            }
            @Override public boolean hasData() {
                return peripheral != null && peripheral.hasData();
            }
            @Override public String description() {
                return peripheral == null ? "(detached)" : peripheral.description();
            }
        };
    }
}
