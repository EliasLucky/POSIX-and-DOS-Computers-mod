package com.eliaslucky.mc_dos.blocks.computer.processors.exec;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

import java.util.*;

public final class ExecutableRegistry {
	public interface Runner {
		/** Return the output string (may start with APP_LAUNCH:), or "" for success. */
		String run(ComputerBlockEntity computer, String args, VirtualFileSystem.Node file);
	}

	public record Entry(String magicHeader, String templateContent, Runner runner) {
		public boolean matchesHeader(String content) {
			return content != null && content.startsWith(magicHeader);
		}
		
		public String run(ComputerBlockEntity computer, String args, VirtualFileSystem.Node file) {
			return runner.run(computer, args, file);
		}
	}

	private static final Map<String, Entry> REGISTRY = new LinkedHashMap<>();

	public static Entry get(String fileName) {
		// File name is like "QBASIC.EXE" or "QBASIC.COM"
		int dot = fileName.lastIndexOf('.');
		String base = dot == -1 ? fileName : fileName.substring(0, dot);
		return REGISTRY.get(base.toUpperCase(Locale.ROOT));
	}

	public static Collection<String> names() { return REGISTRY.keySet(); }

	// A generic MZ header to make it look like a DOS executable.
	private static final String MZ = "MZ\u0090\u0000\u0003\u0000\u0000\u0000";

	static {
		register("COMMAND", MZ + "© Microsoft Corp 1981-1988\nCOMMAND.COM", (c, a, f) ->
				c.getComputerType().osVersion);

		register("QBASIC", MZ + "Microsoft QuickBASIC\nVersion 1.1\n", (c, a, f) -> {
			String requested = a.isEmpty() ? "UNTITLED.BAS" : a;
			String file = c.getFileSystem().canonicalize(a.isEmpty() ? "UNTITLED.BAS" : a);
			if (file.isEmpty()) file = "UNTITLED.BAS";
			VirtualFileSystem.Node node = c.getFileSystem().resolvePath(file);
			if (node == null) {
			    node = new VirtualFileSystem.Node(file, false);
			    node.content = "CLS\n";
			    c.getFileSystem().getCurrentDir().addChild(node);
			    c.setChanged();
			}
			return "APP_LAUNCH:QBASIC:" + file + ":" + node.content;
		});

		register("EDIT", MZ + "MS-DOS Editor\nVersion 1.1\n", (c, a, f) -> {
			if (a.isEmpty()) return "File name must be specified";
			VirtualFileSystem vfs = c.getFileSystem();
			VirtualFileSystem.Node node = vfs.resolvePath(a);
			if (node == null) {
				node = new VirtualFileSystem.Node(a.toUpperCase(Locale.ROOT), false);
				vfs.getCurrentDir().addChild(node);
				c.setChanged();
			}
			return "APP_LAUNCH:EDIT:" + a + ":" + node.content;
		});

		register("GWBASIC", MZ + "GW-BASIC 3.22\n(C) Copyright Microsoft 1983,1984,1985,1986,1987\n", (c, a, f) ->
				"APP_LAUNCH:GWBASIC:" + (a.isEmpty() ? "UNTITLED.BAS" : a) + ":");
	}

	private static void register(String name, String template, Runner r) {
		String base = name.toUpperCase(Locale.ROOT);
		REGISTRY.put(base, new Entry(MZ, template, r));
	}
}
