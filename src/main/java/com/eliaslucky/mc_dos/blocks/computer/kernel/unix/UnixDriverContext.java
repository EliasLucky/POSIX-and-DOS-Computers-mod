package com.eliaslucky.mc_dos.blocks.computer.kernel.unix;

import com.eliaslucky.mc_dos.api.hardware.DeviceHandler;
import com.eliaslucky.mc_dos.api.hardware.DriverContext;
import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;

import java.util.*;

public class UnixDriverContext implements DriverContext {
    private final PeripheralBus bus;
    private final UnixDeviceTable table;
    private final List<String> bootLog;

    public UnixDriverContext(PeripheralBus bus, UnixDeviceTable table, List<String> bootLog) {
        this.bus = bus;
        this.table = table;
        this.bootLog = bootLog;
    }

    @Override public PeripheralBus bus() { return bus; }
    @Override public Map<String, String> loadParams() { return Collections.emptyMap(); }

    @Override
    public String registerDevice(String name, DeviceHandler handler) {
        return table.register(name, handler);
    }

    @Override public void log(String message) { bootLog.add(message); }
}