package com.eliaslucky.mc_dos.api.exec;

import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

/** A file format the OS can execute. */
public interface ExecutableFormat {
    String name();
    boolean matches(String fileName, VirtualFileSystem.Node node);
}
