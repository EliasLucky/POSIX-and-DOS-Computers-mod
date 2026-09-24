package com.eliaslucky.mc_dos.blocks.computer.kernel.dos;

import com.eliaslucky.mc_dos.api.hardware.*;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

import java.util.*;

public class DosKernel implements Kernel {
    private final DosDeviceTable deviceTable = new DosDeviceTable();
    private final List<String> bootLog = new ArrayList<>();
    private final List<Driver> loadedDrivers = new ArrayList<>();

    @Override
    public void boot(PeripheralBus bus, VirtualFileSystem vfs) {
        deviceTable.clear();
        bootLog.clear();
        for (Driver d : loadedDrivers) d.shutdown();
        loadedDrivers.clear();

        // Built-in pseudo-devices always present in DOS.
        deviceTable.register("CON", new NullHandler("Console"));
        deviceTable.register("NUL", new NullHandler("Null device"));
        deviceTable.register("PRN", new NullHandler("Printer (unbacked)"));
        deviceTable.register("AUX", new NullHandler("Auxiliary"));

        VirtualFileSystem.Node config = vfs.resolvePath("C:\\CONFIG.SYS");
        if (config == null || config.isDirectory) {
            bootLog.add("Warning: CONFIG.SYS not found");
            return;
        }

        for (String rawLine : config.content.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith(";")) continue;
            if (line.toUpperCase(Locale.ROOT).startsWith("REM")) continue;
            if (!line.toUpperCase(Locale.ROOT).startsWith("DEVICE=")) continue;
            loadDeviceLine(bus, line.substring("DEVICE=".length()).trim());
        }
    }

    private void loadDeviceLine(PeripheralBus bus, String spec) {
        String[] parts = spec.split("\\s+");
        if (parts.length == 0) return;

        String path = parts[0];
        String fileName = path.substring(path.lastIndexOf('\\') + 1).toUpperCase(Locale.ROOT);
        if (fileName.endsWith(".SYS")) fileName = fileName.substring(0, fileName.length() - 4);

        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (!p.startsWith("/")) continue;
            p = p.substring(1);
            int eq = p.indexOf('=');
            if (eq >= 0) params.put(p.substring(0, eq).toUpperCase(Locale.ROOT), p.substring(eq + 1));
            else         params.put(p.toUpperCase(Locale.ROOT), "1");
        }

        Driver driver = DriverRegistry.load("dos", fileName);
        if (driver == null) {
            bootLog.add("Bad or missing " + fileName + ".SYS");
            return;
        }

        DosDriverContext ctx = new DosDriverContext(bus, params, deviceTable, bootLog);
        DriverInitResult r = driver.init(ctx);
        if (r == DriverInitResult.OK) loadedDrivers.add(driver);
        else bootLog.add(driver.name() + ": driver init failed");
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