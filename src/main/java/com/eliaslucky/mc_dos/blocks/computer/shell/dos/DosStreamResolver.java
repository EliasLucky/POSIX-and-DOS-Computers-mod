package com.eliaslucky.mc_dos.blocks.computer.shell.dos;

import com.eliaslucky.mc_dos.api.hardware.DeviceHandler;
import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.shell.Redirect;
import com.eliaslucky.mc_dos.api.shell.StreamResolver;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

import java.nio.charset.StandardCharsets;

public class DosStreamResolver implements StreamResolver {
    private static final int MAX_READ = 1 << 20;   // 1 MiB ceiling

    @Override
    public byte[] readAll(Redirect source, ComputerBlockEntity computer) {
        if (source == null) return new byte[0];

        if (source instanceof Redirect.Device dev) {
            Kernel k = computer.getKernel();
            if (k == null) return new byte[0];
            DeviceHandler h = k.getDevices().lookup(dev.name());
            if (h == null || !h.hasData()) return new byte[0];
            return h.onRead(MAX_READ);
        }

        if (source instanceof Redirect.File file) {
            VirtualFileSystem vfs = computer.getFileSystem();
            VirtualFileSystem.Node node = vfs.resolvePath(file.path());
            if (node == null || node.isDirectory) return new byte[0];
            return node.content.getBytes(StandardCharsets.UTF_8);
        }

        return new byte[0];
    }

    @Override
    public void writeAll(Redirect sink, byte[] data, ComputerBlockEntity computer) {
        if (sink == null) return;

        if (sink instanceof Redirect.Device dev) {
            Kernel k = computer.getKernel();
            if (k == null) return;
            DeviceHandler h = k.getDevices().lookup(dev.name());
            if (h != null) h.onWrite(data);
            return;
        }

        if (sink instanceof Redirect.File file) {
            VirtualFileSystem vfs = computer.getFileSystem();
            String canonical = vfs.canonicalize(file.path());
            if (canonical.isEmpty()) return;

            VirtualFileSystem.Node node = vfs.resolvePath(canonical);
            if (node == null) {
                node = new VirtualFileSystem.Node(canonical, false);
                vfs.getCurrentDir().addChild(node);
            }
            if (node.isDirectory) return;

            String text = new String(data, StandardCharsets.UTF_8);
            switch (file.mode()) {
                case WRITE  -> node.content = text;
                case APPEND -> node.content = node.content + text;
                case READ   -> { /* not valid on a sink */ }
            }
            node.modifiedTime = System.currentTimeMillis();
            computer.setChanged();
        }
    }
}