package com.eliaslucky.mc_dos.blocks.computer.shell.unix;

import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.shell.Redirect;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;

/**
 * UNIX v7 /bin/sh syntax (Bourne shell). Supports > >> < | ; &.
 * Does NOT support && || (those arrived with ksh in the 80s).
 *
 * Device redirection: /dev/... is always a device path.
 */
public class UnixV7Dialect extends ShellDialect {
    private final Kernel kernel;

    public UnixV7Dialect(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override public String name()             { return "UNIX v7 /bin/sh"; }
    @Override public boolean supportsBackground() { return true; }

    @Override
    protected Redirect makeRedirect(String target, Redirect.Mode mode) {
        if (kernel != null && kernel.getDevices().isDevice(target)) {
            return new Redirect.Device(target, mode);
        }
        // Also treat absolute /dev/... paths as devices even if unregistered.
        if (target.startsWith("/dev/")) {
            return new Redirect.Device(target, mode);
        }
        return new Redirect.File(target, mode);
    }
}