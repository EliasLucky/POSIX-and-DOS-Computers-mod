package com.eliaslucky.mc_dos.api.exec;

import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

/** MS-DOS .EXE — "MZ" magic, or "ZM" for a few reverse-written tools. */
public final class DosMZFormat implements ExecutableFormat {
    public static final DosMZFormat INSTANCE = new DosMZFormat();
    private DosMZFormat() {}

    @Override public String name() { return "MS-DOS MZ"; }

    @Override
    public boolean matches(String fileName, VirtualFileSystem.Node node) {
        if (!fileName.toUpperCase().endsWith(".EXE")) return false;
        String c = node.content;
        return c != null && c.length() >= 2
                && (c.startsWith("MZ") || c.startsWith("ZM"));
    }
}
