package com.eliaslucky.mc_dos.registry;

import com.eliaslucky.mc_dos.api.hardware.DriverRegistry;
import com.eliaslucky.mc_dos.blocks.computer.kernel.dos.drivers.DosMccmdDriver;

public final class ModDrivers {
    private ModDrivers() {}

    public static void register() {
        DriverRegistry.register("dos", "MCCMD", "C:\\DRIVERS\\MCCMD.SYS", "MZ\u0090\u0000\u0003\u0000\u0000\u0000MCCMD driver\n", DosMccmdDriver::new);
        //DriverRegistry.register("posix", "mccmd", "/lib/modules/2.4.20-8/kernel/drivers/char/mccmd.ko", "\u007fELF mccmd.ko\n", LinuxMccmdDriver::new);
        //DriverRegistry.register("unix",  "MCCMD", UnixV7MccmdDriver::new);
    }
}
