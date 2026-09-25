package com.eliaslucky.mc_dos.blocks.computer.kernel.dos.drivers;

import com.eliaslucky.mc_dos.api.hardware.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class DosMccmdDriver implements Driver {
    private Peripheral peripheral;

    @Override public String name() { return "MCCMD.SYS"; }

    @Override
    public DriverInitResult init(DriverContext ctx) {
        int slot = Integer.parseInt(ctx.loadParams().getOrDefault("SLOT", "0"));

        List<PeripheralAddress> matches = ctx.bus().scan().stream()
                .filter(a -> a.deviceClass().equals("mccmd") && a.slot() == slot)
                .toList();

        if (matches.isEmpty()) {
            ctx.log("MCCMD.SYS: no MCCMD device at slot " + slot);
            return DriverInitResult.FAILED;
        }

        this.peripheral = ctx.bus().get(matches.get(0));
        if (peripheral == null) return DriverInitResult.FAILED;

        String devName = ctx.registerDevice("MCCMD", DeviceHandler.of(peripheral));
        if (devName == null) {
            ctx.log("MCCMD.SYS: name MCCMD already in use");
            return DriverInitResult.FAILED;
        }

        ctx.log("MCCMD.SYS installed at slot " + slot);
        return DriverInitResult.OK;
    }

    @Override public void shutdown() { peripheral = null; }

    private class Handler implements DeviceHandler {

        @Override public void onWrite(byte[] data) {
            if (peripheral != null) peripheral.write(data);
        }
        @Override public byte[] onRead(int maxBytes) {
            return peripheral == null ? new byte[0] : peripheral.read(maxBytes);
        }
        @Override public int onIoctl(int cmd, byte[] arg) {
            return peripheral == null ? -1 : peripheral.ioctl(cmd, arg);
        }
        @Override public boolean hasData() {
            return peripheral != null && peripheral.hasData();
        }
        @Override public String description() { return "Minecraft command translator"; }
    }

    // Convenience for the command processor — prints to terminal via handler.
    public static byte[] encode(String s) { return s.getBytes(StandardCharsets.UTF_8); }
}