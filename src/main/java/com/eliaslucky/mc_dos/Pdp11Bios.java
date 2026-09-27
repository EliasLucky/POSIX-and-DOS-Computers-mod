package com.example.computersaddon.bios;

import com.eliaslucky.mc_dos.api.bios.Bios;
import com.eliaslucky.mc_dos.api.bios.MachineConfig;
import com.eliaslucky.mc_dos.api.hardware.PeripheralAddress;
import com.eliaslucky.mc_dos.api.hardware.PeripheralBus;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * PDP-11/70 firmware.
 *
 * <p>Unlike a PC BIOS, PDP-11s had a bootstrap loader that read a
 * small block from disk and jumped to it. There was no interactive
 * SETUP screen the "configuration" was physical switches on the
 * front panel. This BIOS reports that bootstrap sequence.
 */
public class Pdp11Bios implements Bios {
    @Override public String name()        { return "PDP-11/70"; }
    @Override public String version()     { return "CPU 11/70 Rev 4"; }
    @Override public String manufacturer(){ return "Digital Equipment Corporation"; }
    @Override public String releaseDate() { return "1975"; }
    @Override public String copyright()   {
        return "Copyright (c) 1975, Digital Equipment Corporation";
    }

    @Override
    public List<String> runPost(ComputerBlockEntity machine,
                                PeripheralBus bus,
                                MachineConfig config) {
        List<String> out = new ArrayList<>();

        out.add("PDP-11/70 BOOT");
        out.add("");
        out.add(String.format("CPU: 11/70, %d KW memory",
                config.baseMemoryKb() / 4));
        out.add("");

        List<PeripheralAddress> devices = bus.scan();
        if (!devices.isEmpty()) {
            out.add("UNIBUS devices:");
            for (PeripheralAddress a : devices) {
                out.add(String.format("  %d  %-12s  %s",
                        a.slot(),
                        a.deviceClass().toUpperCase(),
                        a.vendorId()));
            }
            out.add("");
        }

        out.add("Boot device: RK0 (disk)");
        out.add("Loading /boot...");
        out.add("");
        out.add("UNIX v7 (pdp11/70)");
        out.add("login: root");
        out.add("");
        return out;
    }

    @Override public int setupKeyCode()     { return -1; }  // no SETUP
    @Override public String setupPrompt()   { return ""; }
    @Override public String setupScreenId() { return ""; }
}
