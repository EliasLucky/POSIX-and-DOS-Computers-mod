package com.eliaslucky.mc_dos.blocks.computer.kernel.unix;

import com.eliaslucky.mc_dos.api.hardware.*;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

import java.util.*;

public class UnixV7Kernel implements Kernel {
    private final UnixDeviceTable deviceTable = new UnixDeviceTable();
    private final List<String> bootLog = new ArrayList<>();
    private final List<Driver> loadedDrivers = new ArrayList<>();

    @Override
    public void boot(PeripheralBus bus, VirtualFileSystem vfs) {
        deviceTable.clear();
        bootLog.clear();
        for (Driver d : loadedDrivers) d.shutdown();
        loadedDrivers.clear();

        // Built-in devices the v7 kernel always provides.
        deviceTable.register("/dev/null",    new NullHandler("Null device"));
        deviceTable.register("/dev/tty",     new NullHandler("Controlling terminal"));
        deviceTable.register("/dev/console", new NullHandler("System console"));
        deviceTable.register("/dev/mem",     new NullHandler("Physical memory"));

        // Scan the bus. If a driver is registered for a device class
        // under the "unix" family, load it.
        for (PeripheralAddress addr : bus.scan()) {
            String driverName = addr.deviceClass().toUpperCase(Locale.ROOT);
            Driver d = DriverRegistry.load("unix", driverName);
            if (d == null) continue;

            UnixDriverContext ctx = new UnixDriverContext(bus, deviceTable, bootLog);
            DriverInitResult r = d.init(ctx);
            if (r == DriverInitResult.OK) {
                loadedDrivers.add(d);
                bootLog.add(d.name() + " linked into kernel at " + addr.slot());
            } else {
                bootLog.add(d.name() + ": probe failed at " + addr.slot());
            }
        }
    }

    @Override public List<String> getBootLog() { return bootLog; }
    @Override public DeviceLookup getDevices() { return deviceTable; }

    @Override
    public void shutdown() {
        for (Driver d : loadedDrivers) d.shutdown();
        loadedDrivers.clear();
        deviceTable.clear();
    }

    private static class NullHandler implements DeviceHandler {
        private final String desc;
        NullHandler(String d) { this.desc = d; }
        @Override public void onWrite(byte[] data) {}
        @Override public byte[] onRead(int maxBytes) { return new byte[0]; }
        @Override public int onIoctl(int cmd, byte[] arg) { return 0; }
        @Override public boolean hasData() { return false; }
        @Override public String description() { return desc; }
    }
}