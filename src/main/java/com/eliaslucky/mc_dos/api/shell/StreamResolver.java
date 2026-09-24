package com.eliaslucky.mc_dos.api.shell;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

/** Reads and writes the byte streams behind redirects. */
public interface StreamResolver {

    /** Read all bytes from a source redirect. Returns empty if nothing. */
    byte[] readAll(Redirect source, ComputerBlockEntity computer);

    /** Write bytes to a sink redirect. */
    void writeAll(Redirect sink, byte[] data, ComputerBlockEntity computer);
}