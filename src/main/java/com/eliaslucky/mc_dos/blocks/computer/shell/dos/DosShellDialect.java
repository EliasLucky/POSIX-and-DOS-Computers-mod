package com.eliaslucky.mc_dos.blocks.computer.shell.dos;

import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.shell.Redirect;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;

/**
 * MS-DOS COMMAND.COM syntax. Supports > >> < | and ; (batch).
 * No &&, ||, &, or heredocs — those never existed in DOS.
 *
 * Device redirection: if the kernel says a bare name is a device
 * (NUL, PRN, CON, MCCMD), the redirect targets the device instead
 * of a file. Otherwise it's a file.
 */
public class DosShellDialect extends ShellDialect {

    private final Kernel kernel;

    public DosShellDialect(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override public String name() { return "MS-DOS COMMAND.COM"; }

    @Override
    protected Redirect makeRedirect(String target, Redirect.Mode mode) {
        if (kernel != null && kernel.getDevices().isDevice(target)) {
            return new Redirect.Device(target.toUpperCase(), mode);
        }
        return new Redirect.File(target, mode);
    }
}