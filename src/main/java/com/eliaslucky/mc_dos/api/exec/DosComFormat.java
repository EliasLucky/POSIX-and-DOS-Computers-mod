package com.eliaslucky.mc_dos.api.exec;

import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

/** MS-DOS .COM — raw binary, no magic. Matched by extension only. */
public final class DosComFormat implements ExecutableFormat {
    public static final DosComFormat INSTANCE = new DosComFormat();
    private DosComFormat() {}
    @Override public String name() { return "MS-DOS COM"; }
    @Override
    public boolean matches(String fileName, VirtualFileSystem.Node node) {
        return fileName.toUpperCase().endsWith(".COM")
                && node.content != null
                && node.content.length() <= 65536;
    }
}
