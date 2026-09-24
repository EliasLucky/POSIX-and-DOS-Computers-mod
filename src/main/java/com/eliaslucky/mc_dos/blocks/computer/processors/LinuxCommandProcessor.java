package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileNamePolicy;
import com.eliaslucky.mc_dos.blocks.computer.fs.PosixFileNamePolicy;

// TODO: SCRAP THIS
public class LinuxCommandProcessor implements ICommandProcessor {
	@Override
	public FileNamePolicy fileNamePolicy() {
		return PosixFileNamePolicy.INSTANCE;
	}
	@Override
	public String getPrompt(String currentPath) {
		return "root@p4-server:" + currentPath + "# ";
	}

	@Override
	public String process(ComputerBlockEntity computer, String rawInput) {
		VirtualFileSystem vfs = computer.getFileSystem();
		String input = rawInput.trim();
		if (input.isEmpty()) return "";

		String[] parts = input.split("\\s+", 2);
		String cmd = parts[0].toLowerCase();
		String arg = parts.length > 1 ? parts[1].trim() : "";

		switch (cmd) {
			case "ls":
				VirtualFileSystem.Node targetDir = arg.isEmpty() ? vfs.getCurrentDir() : vfs.resolvePath(arg);
				if (targetDir == null || !targetDir.isDirectory) {
					return "ls: cannot access '" + arg + "': No such file or directory";
				}

				StringBuilder sb = new StringBuilder();
				for (VirtualFileSystem.Node node : targetDir.children.values()) {
					sb.append(node.isDirectory ? node.name.toLowerCase() + "/  " : node.name.toLowerCase() + "	");
				}
				return sb.toString();

			case "cd":
				if (arg.isEmpty() || arg.equals("~")) {
					vfs.setCurrentPath("/");
					return "";
				}
				VirtualFileSystem.Node targetCd = vfs.resolvePath(arg);
				if (targetCd != null && targetCd.isDirectory) {
					vfs.setCurrentPath(vfs.getAbsolutePath(targetCd, false));
				} else {
					return "bash: cd: " + arg + ": No such file or directory";
				}
				return "";

			case "mkdir":
				if (arg.isEmpty()) return "mkdir: missing operand";

				String cleanPath = arg.replace('\\', '/');
				int lastSlash = cleanPath.lastIndexOf('/');

				VirtualFileSystem.Node parentNode = vfs.getCurrentDir();
				String newDirName = cleanPath;

				if (lastSlash != -1) {
					String parentPath = cleanPath.substring(0, lastSlash);
					newDirName = cleanPath.substring(lastSlash + 1);

					if (parentPath.isEmpty()) {
						parentNode = vfs.getRoot();
					} else {
						parentNode = vfs.resolvePath(parentPath);
					}
				}

				if (parentNode == null || !parentNode.isDirectory) {
					return "mkdir: cannot create directory '" + arg + "': No such file or directory";
				}
				if (newDirName.isEmpty()) return "mkdir: invalid directory name";

				String lowerName = newDirName.toLowerCase();
				if (parentNode.children.containsKey(lowerName)) {
					return "mkdir: cannot create directory '" + arg + "': File exists";
				}

				VirtualFileSystem.Node newFolder = new VirtualFileSystem.Node(lowerName, true);
				parentNode.addChild(newFolder);
				computer.setChanged();
				return "";

			case "rmdir":
				if (arg.isEmpty()) return "rmdir: missing operand";
				VirtualFileSystem.Node rmNode = vfs.resolvePath(arg);
				if (rmNode == null || !rmNode.isDirectory) {
					return "rmdir: failed to remove '" + arg + "': No such file or directory";
				}
				if (rmNode.parent != null) {
					rmNode.parent.children.remove(rmNode.name);
					computer.setChanged();
				}
				return "";

			case "cat":
				if (arg.isEmpty()) return "cat: missing file operand";
				VirtualFileSystem.Node file = vfs.resolvePath(arg);
				if (file != null && !file.isDirectory) return file.content;
				return "cat: " + arg + ": No such file or directory";

			case "touch":
				if (arg.isEmpty()) return "touch: missing file operand";
				VirtualFileSystem.Node existing = vfs.resolvePath(arg);
				if (existing == null) {
					VirtualFileSystem.Node newFile = new VirtualFileSystem.Node(arg.toLowerCase(), false);
					vfs.getCurrentDir().addChild(newFile);
					computer.setChanged();
				}
				return "";

			case "pwd":
				return vfs.getCurrentPath();

			case "uname":
				if ("-a".equals(arg)) return "Linux p4-server 2.4.20-8 #1 SMP Mon Mar 13 i686 GNU/Linux";
				return "Linux";

			default:
				return "bash: " + cmd + ": command not found";
		}
	}

	@Override
	public String defaultPath() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String defaultFileContent(String fileName) {
		return switch (fileName.toUpperCase(java.util.Locale.ROOT)) {
			case "ETC/PASSWD" ->
					"root:x:0:0:root:/root:/bin/bash\n" +
					"daemon:x:1:1:daemon:/usr/sbin:/bin/sh\n" +
					"bin:x:2:2:bin:/bin:/bin/sh";
			case "ETC/FSTAB" ->
					"/dev/hda1	/	   ext3  defaults,errors=remount-ro  0	1\n" +
					"/dev/hda2	none   swap  sw							 0	0";
			default -> null;
		};
	}
}
