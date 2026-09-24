package com.eliaslucky.mc_dos.blocks.computer.kernel.dos;

import com.eliaslucky.mc_dos.api.hardware.DeviceHandler;
import com.eliaslucky.mc_dos.api.hardware.DriverContext;
import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DosDriverContext implements DriverContext {
    private final PeripheralBus bus;
    private final Map<String, String> loadParams;
    private final DosDeviceTable table;
    private final List<String> bootLog;

    public DosDriverContext(PeripheralBus bus,  Map<String, String> loadParams, DosDeviceTable table, List<String> bootLog) {
        this.bus = bus;
        this.loadParams = loadParams;
        this.table = table;
        this.bootLog = bootLog;
    }

    @Override public PeripheralBus bus()          { return bus; }
    @Override public Map<String, String> loadParams() { return Collections.unmodifiableMap(loadParams); }

    @Override
    public String registerDevice(String name, DeviceHandler handler) {
        String upper = name.toUpperCase(java.util.Locale.ROOT);
        if (!table.register(upper, handler)) return null;
        return upper;
    }

    @Override
    public void log(String message) { bootLog.add(message); }

    public Map<String, String> rawParams() { return loadParams; }
    public List<String> getBootLog()       { return bootLog; }
}