package com.eliaslucky.mc_dos.registry;

import com.eliaslucky.mc_dos.api.exec.DosComFormat;
import com.eliaslucky.mc_dos.api.exec.DosMZFormat;
import com.eliaslucky.mc_dos.api.exec.ExecutableRegistry;
import com.eliaslucky.mc_dos.api.exec.PosixElfFormat;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import com.eliaslucky.mc_dos.blocks.computer.processors.posix.ShellRunner;
import com.eliaslucky.mc_dos.blocks.computer.shell.unix.BourneV7Dialect;

public final class ModExecutables {
    private ModExecutables() {}

    public static void register() {

        // MS-DOS
        ExecutableRegistry.register("dos", "QBASIC.EXE", DosMZFormat.INSTANCE,
                "MZ\u0090\u0000\u0003\u0000\u0000\u0000Microsoft QuickBASIC\n",
                (computer, args, file) -> {
                    // Find or create the target file.
                    String requested = args.isEmpty() ? "UNTITLED.BAS" : args;
                    String name = computer.getFileSystem().canonicalize(requested);
                    if (name.isEmpty()) name = "UNTITLED.BAS";

                    VirtualFileSystem vfs = computer.getFileSystem();
                    VirtualFileSystem.Node node = vfs.resolvePath(name);
                    if (node == null) {
                        node = new VirtualFileSystem.Node(name, false);
                        node.content = "CLS\n";
                        vfs.getCurrentDir().addChild(node);
                        computer.setChanged();
                    }
                    // The client resolves this string into a TerminalApplication.
                    return "APP_LAUNCH:QBASIC:" + name + ":" + node.content;
                });

        ExecutableRegistry.register("dos", "EDIT.COM", DosComFormat.INSTANCE,
                "\u00B4MS-DOS Editor\n",
                (computer, args, file) -> {
                    if (args.isEmpty()) return "File name must be specified";
                    String name = computer.getFileSystem().canonicalize(args);
                    VirtualFileSystem vfs = computer.getFileSystem();
                    VirtualFileSystem.Node node = vfs.resolvePath(name);
                    if (node == null) {
                        node = new VirtualFileSystem.Node(name, false);
                        vfs.getCurrentDir().addChild(node);
                        computer.setChanged();
                    }
                    return "APP_LAUNCH:EDIT:" + name + ":" + node.content;
                });

        ExecutableRegistry.register("dos", "GWBASIC.EXE", DosMZFormat.INSTANCE,
                "MZ\u0090\u0000\u0003\u0000\u0000\u0000GW-BASIC 3.22\n",
                (computer, args, file) -> {
                    String name = args.isEmpty() ? "UNTITLED.BAS"
                            : computer.getFileSystem().canonicalize(args);
                    return "APP_LAUNCH:GWBASIC:" + name + ":";
                });

        // POSIX (Linux / UNIX)
        // /bin/sh and /bin/bash take a script path and run it through ShellRunner.
        ExecutableRegistry.register("posix", "/BIN/SH", PosixElfFormat.INSTANCE,
                "\u007fELF /bin/sh\n",
                (computer, args, file) ->
                        new ShellRunner(BourneV7Dialect.INSTANCE).run(computer, args));

        // ... ls, cat, grep as needed later
    }
}