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
}
