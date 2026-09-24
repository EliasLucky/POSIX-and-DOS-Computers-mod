package com.eliaslucky.mc_dos.registry;

import com.eliaslucky.mc_dos.api.hardware.DriverRegistry;
import com.eliaslucky.mc_dos.blocks.computer.kernel.dos.drivers.DosMccmdDriver;
import com.eliaslucky.mc_dos.blocks.computer.kernel.posix.drivers.LinuxMccmdDriver;
import com.eliaslucky.mc_dos.blocks.computer.kernel.unix.drivers.UnixV7MccmdDriver;

public final class ModDrivers {
    private ModDrivers() {}

    public static void register() {
        DriverRegistry.register("dos",   "MCCMD", DosMccmdDriver::new);
        DriverRegistry.register("posix", "MCCMD", LinuxMccmdDriver::new);
        DriverRegistry.register("unix",  "MCCMD", UnixV7MccmdDriver::new);
    }
}
