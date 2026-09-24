package com.eliaslucky.mc_dos.blocks.computer.kernel.dos;

import com.eliaslucky.mc_dos.api.hardware.*;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

import java.util.*;

public class DosKernel {
    private final PeripheralBus bus;
    private final DosDeviceTable deviceTable = new DosDeviceTable();
    private final List<String> bootLog = new ArrayList<>();
    private final List<Driver> loadedDrivers = new ArrayList<>();

    public DosKernel(PeripheralBus bus) { this.bus = bus; }

    public void boot(VirtualFileSystem vfs) {
        deviceTable.clear();
        bootLog.clear();
        loadedDrivers.clear();

        for (Driver d : loadedDrivers) d.shutdown();

        // Minimal DOS always has CON, PRN, AUX, NUL as built-in pseudo-devices.
        deviceTable.register("CON", new NullDeviceHandler("Console"));
        deviceTable.register("NUL", new NullDeviceHandler("Null device"));
        deviceTable.register("PRN", new NullDeviceHandler("Printer (unbacked)"));

        VirtualFileSystem.Node config = vfs.resolvePath("C:\\CONFIG.SYS");
        if (config == null || config.isDirectory) {
            bootLog.add("Warning: CONFIG.SYS not found");
            return;
        }

        for (String rawLine : config.content.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("REM")) continue;
            if (!line.toUpperCase(Locale.ROOT).startsWith("DEVICE=")) continue;

            String spec = line.substring("DEVICE=".length()).trim();
            loadDeviceLine(spec);
        }
    }

    private void loadDeviceLine(String spec) {
        String[] parts = spec.split("\\s+");
        if (parts.length == 0) return;

        String path = parts[0];
        String fileName = path.substring(path.lastIndexOf('\\') + 1).toUpperCase(Locale.ROOT);

        // Strip ".SYS" if present — DriverRegistry keys on the bare name.
        if (fileName.endsWith(".SYS")) {
            fileName = fileName.substring(0, fileName.length() - 4);
        }

        Map<String, String> params = new HashMap<>();
        for (int i = 1; i < parts.length; i++) {
            String p = parts[i];
            if (p.startsWith("/")) {
                p = p.substring(1);
                int eq = p.indexOf('=');
                if (eq >= 0) params.put(p.substring(0, eq).toUpperCase(Locale.ROOT),
                                       p.substring(eq + 1));
                else params.put(p.toUpperCase(Locale.ROOT), "1");
            }
        }

        Driver driver = DriverRegistry.load("dos", fileName);
        if (driver == null) {
            bootLog.add("Bad or missing " + fileName + ".SYS");
            return;
        }

        DosDriverContext ctx = new DosDriverContext(bus, params, deviceTable, bootLog);
        DriverInitResult result = driver.init(ctx);
        if (result == DriverInitResult.OK) {
            loadedDrivers.add(driver);
        } else {
            bootLog.add(driver.name() + ": driver init failed");
        }
    }

    public DosDeviceTable getDeviceTable() { return deviceTable; }
    public List<String> getBootLog()       { return bootLog; }
    public void shutdown() {
        for (Driver d : loadedDrivers) d.shutdown();
        loadedDrivers.clear();
        deviceTable.clear();
    }

    private static class NullDeviceHandler implements DeviceHandler {
        private final String desc;
        NullDeviceHandler(String desc) { this.desc = desc; }
        @Override public void onWrite(byte[] data) {}
        @Override public byte[] onRead(int maxBytes) { return new byte[0]; }
        @Override public int onIoctl(int cmd, byte[] arg) { return 0; }
        @Override public boolean hasData() { return false; }
        @Override public String description() { return desc; }
    }
}
