package com.eliaslucky.mc_dos.blocks.computer.kernel.linux.drivers;

import com.eliaslucky.mc_dos.api.hardware.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Linux kernel module for the Minecraft command translator.
 *
 * Matches every mccmd-class peripheral on the bus, up to
 * MAX_DEVICES. Each gets registered as /dev/mccmdN where N is a
 * per-driver-instance counter. Mirrors Linux's behaviour of one
 * driver binding to many devices of the same class.
 *
 * In real Linux, udev creates /dev entries after the kernel allocates
 * major/minor numbers. Our LinuxDeviceTable does the equivalent
 * allocation, and the driver names the base — the "0"/"1" suffix
 * comes from the table.
 */
public class LinuxMccmdDriver implements Driver {
    /** Linux drivers typically cap the number of devices they bind. */
    private static final int MAX_DEVICES = 8;

    private final List<Peripheral> bound = new ArrayList<>();

    @Override public String name() { return "mccmd.ko"; }

    @Override
    public DriverInitResult init(DriverContext ctx) {
        List<PeripheralAddress> matches = ctx.bus().scan().stream()
                .filter(a -> a.deviceClass().equals("mccmd"))
                .limit(MAX_DEVICES)
                .toList();

        if (matches.isEmpty()) {
            // Linux would return -ENODEV and unload the module.
            ctx.log("mccmd: no matching devices");
            return DriverInitResult.FAILED;
        }

        for (PeripheralAddress addr : matches) {
            Peripheral p = ctx.bus().get(addr);
            if (p == null) continue;

            // Linux naming: base + index, allocated by the kernel.
            String devPath = ctx.registerDevice("mccmd", DeviceHandler.of(p));
            if (devPath == null) {
                ctx.log("mccmd: failed to register device at slot " + addr.slot());
                continue;
            }
            bound.add(p);
            ctx.log("mccmd: registered " + devPath + " (slot " + addr.slot() + ")");
        }

        return bound.isEmpty() ? DriverInitResult.FAILED : DriverInitResult.OK;
    }

    @Override
    public void shutdown() {
        bound.clear();
    }
}