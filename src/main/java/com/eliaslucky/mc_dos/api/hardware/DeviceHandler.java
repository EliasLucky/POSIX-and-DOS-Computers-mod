package com.eliaslucky.mc_dos.api.hardware;

public interface DeviceHandler {
    /** Called when an application writes to this device. */
    void onWrite(byte[] data);

    /** Called when an application reads from this device. */
    byte[] onRead(int maxBytes);

    /** Called for device-specific control. */
    int onIoctl(int cmd, byte[] arg);

    boolean hasData();

    /** Human description, e.g. "Line printer 0" */
    String description();
    
    /**
     * Wraps a Peripheral as a DeviceHandler. This is the 1:1 adapter
     * used by every driver; addons almost never need to write their own.
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
