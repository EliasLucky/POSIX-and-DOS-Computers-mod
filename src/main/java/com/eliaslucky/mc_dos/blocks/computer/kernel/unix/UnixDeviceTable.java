package com.eliaslucky.mc_dos.blocks.computer.kernel.unix;

import com.eliaslucky.mc_dos.api.hardware.DeviceHandler;
import com.eliaslucky.mc_dos.api.hardware.DeviceLookup;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UnixDeviceTable implements DeviceLookup {

    private final Map<String, DeviceHandler> devices = new LinkedHashMap<>();
    private final Map<Integer, String> majorToPath = new HashMap<>();
    private int nextMajor = 0;

    public String register(String path, DeviceHandler h) {
        String normalized = path.startsWith("/dev/") ? path : "/dev/" + path;
        int major = nextMajor++;
        majorToPath.put(major, normalized);
        devices.put(normalized, h);
        return normalized;
    }

    private static String normalize(String name) {
        return name.startsWith("/dev/") ? name : "/dev/" + name;
    }

    @Override public boolean isDevice(String name) {
        return devices.containsKey(normalize(name));
    }

    @Override public DeviceHandler lookup(String name) {
        return devices.get(normalize(name));
    }

    @Override public List<String> names() { return List.copyOf(devices.keySet()); }

    public void clear() {
        devices.clear();
        majorToPath.clear();
        nextMajor = 0;
    }
}
