package com.eliaslucky.mc_dos.blocks.computer.kernel.dos;

import com.eliaslucky.mc_dos.api.hardware.DeviceHandler;
import com.eliaslucky.mc_dos.api.hardware.DeviceLookup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DosDeviceTable implements DeviceLookup {

    private final Map<String, DeviceHandler> devices = new LinkedHashMap<>();

    private static final Set<String> RESERVED = Set.of(
            "CON", "PRN", "AUX", "NUL", "CLOCK$",
            "LPT1", "LPT2", "LPT3",
            "COM1", "COM2", "COM3", "COM4");

    public boolean register(String name, DeviceHandler h) {
        String upper = name.toUpperCase(Locale.ROOT);
        if (upper.isEmpty() || upper.length() > 8) return false;
        if (RESERVED.contains(upper)) return false;
        if (devices.containsKey(upper)) return false;
        devices.put(upper, h);
        return true;
    }

    @Override public boolean isDevice(String name) {
        return devices.containsKey(name.toUpperCase(Locale.ROOT));
    }

    @Override public DeviceHandler lookup(String name) {
        return devices.get(name.toUpperCase(Locale.ROOT));
    }

    @Override public List<String> names() { return List.copyOf(devices.keySet()); }

    public void clear() { devices.clear(); }
}