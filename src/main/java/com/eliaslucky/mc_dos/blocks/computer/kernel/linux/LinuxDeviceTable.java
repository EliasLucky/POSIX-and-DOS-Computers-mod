package com.eliaslucky.mc_dos.blocks.computer.kernel.linux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.eliaslucky.mc_dos.api.hardware.DeviceHandler;

public class LinuxDeviceTable {
    /** Linux device names are Unix filenames. Driver registers a base name;
     *  the kernel appends a per-class index (lp0, lp1, mccmd0, ...). */
    private final Map<String, DeviceHandler> devices = new LinkedHashMap<>();

    public String register(String baseName, DeviceHandler h) {
        // Find next free index for this base name.
        int i = 0;
        while (devices.containsKey(baseName + i)) i++;
        String name = baseName + i;
        devices.put(name, h);
        return name;
    }

    public DeviceHandler lookup(String devPath) {
        // Called with "/dev/mccmd0" → strip prefix
        String name = devPath.startsWith("/dev/") ? devPath.substring(5) : devPath;
        return devices.get(name);
    }

    public List<String> names() { return List.copyOf(devices.keySet()); }
}
