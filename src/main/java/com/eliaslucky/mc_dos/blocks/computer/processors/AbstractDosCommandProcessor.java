package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import com.eliaslucky.mc_dos.blocks.computer.processors.exec.ExecutableRegistry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class AbstractDosCommandProcessor implements ICommandProcessor {
	// Default PATH used on a fresh machine, e.g. "C:\\DOS;C:\\".
	public abstract String defaultPath();

	// Does this DOS understand `cmd /?` ? (5.0+ yes, 3.x no).
	protected boolean supportsSlashQuestionHelp() { return false; }

	// Command names this version adds on top of the shared set.
	protected String handleVersionSpecific(ComputerBlockEntity computer, VirtualFileSystem vfs, String cmd, String arg, String rawArg) {
		return null; // default: nothing extra
	}

	// Template contents for a brand new file created by an editor.
	protected String newFileTemplate(String extension) { return ""; }

	// Command name used for "list files": both use DIR but keep it a hook.
	protected String dirCommandName() { return "DIR"; }

	@Override
	public String process(ComputerBlockEntity computer, String rawInput) {
		VirtualFileSystem vfs = computer.getFileSystem();
		String input = rawInput.trim();
		if (input.isEmpty()) return "";

		String[] parts = input.split("\\s+", 2);
		String cmd = parts[0].toUpperCase(Locale.ROOT);
		String argRaw = parts.length > 1 ? parts[1].trim() : "";
		String arg = argRaw;
   
		String specific = handleVersionSpecific(computer, vfs, cmd, arg, argRaw);
		if (specific != null) return specific;

		if (supportsSlashQuestionHelp() && arg.equals("/?")) {
			String help = helpFor(cmd);
			if (help != null) return help;
			return "Bad command or file name";
		}

		String launch = tryLaunchExecutable(computer, vfs, cmd, argRaw);
		if (launch != null) return launch;
 
		switch (cmd) {
			case "DIR":  return doDir(vfs, computer, arg);
			case "CD":
			case "CHDIR": return doCd(vfs, arg);
			case "MD":
			case "MKDIR": return doMd(vfs, computer, arg);
			case "RD":
			case "RMDIR": return doRd(vfs, computer, arg);
			case "COPY": return doCopy(vfs, computer, arg);
			case "REN":
			case "RENAME": return doRen(vfs, computer, arg);
			case "DEL":
			case "ERASE": return doDel(vfs, computer, arg);
			case "TYPE": return doType(vfs, arg);
			case "VER": return doVer(computer);
			case "VOL": return " Volume in drive C has no label\n Volume Serial Number is 1337-3000";
			case "DATE": return doDate();
			case "TIME": return doTime();
			case "ECHO": return arg.isEmpty() ? "ECHO is on." : arg;
			case "PROMPT": return "";
			case "PATH": return doPath(computer, arg);
			case "SET": return doSet(computer, argRaw);
			case "TREE": return doTree(vfs);
			case "CLS": return "__CLEAR__"; // client
			case "EXIT": return doExit();
			// ... the rest of the shared switch ...
		}

		// Version error wording.
		String fallback = fallbackUnknown(computer, cmd);
		if (fallback != null) return fallback;

		return unknownCommandMessage();
	}

	protected String unknownCommandMessage() { return "Bad command or file name"; }

	protected String fallbackUnknown(ComputerBlockEntity c, String cmd) { return null; }

	protected String doDir(VirtualFileSystem vfs, ComputerBlockEntity computer, String arg) {
		VirtualFileSystem.Node targetDirNode = arg.isEmpty()
				? vfs.getCurrentDir()
				: vfs.resolvePath(arg);
		if (targetDirNode == null || !targetDirNode.isDirectory) {
			return "Invalid directory";
		}

		String displayPath = vfs.getAbsolutePath(targetDirNode);

		StringBuilder out = new StringBuilder()
			.append("\n Volume in drive C has no label\n")
			.append(" Volume Serial Number is 1337-3000\n")
			.append(" Directory of ").append(displayPath).append("\n\n");

		int fileCount = 0;
		int totalBytes = 0;

		java.util.function.Function<VirtualFileSystem.Node, String> row = (node) -> {
			String baseName = node.name;
			String ext = "";
			if (!node.name.equals(".") && !node.name.equals("..")) {
				int dot = node.name.lastIndexOf('.');
				if (dot != -1) {
					baseName = node.name.substring(0, dot);
					ext = node.name.substring(dot + 1);
				}
			}
			if (baseName.length() > 8) baseName = baseName.substring(0, 8);
			if (ext.length() > 3)	   ext = ext.substring(0, 3);

			String size = node.isDirectory ? "<DIR>" : String.valueOf(node.content.length());
			String date = formatDosDate(node.modifiedTime);
			String time = formatDosTime(node.modifiedTime);

			return String.format("%-8s %-3s   %10s	 %s  %s%n",
					baseName.toUpperCase(Locale.ROOT),
					ext.toUpperCase(Locale.ROOT),
					size, date, time);
		};

		if (targetDirNode.parent != null) {
			VirtualFileSystem.Node dot	  = new VirtualFileSystem.Node(".",  true);
			VirtualFileSystem.Node dotdot = new VirtualFileSystem.Node(".", true);
			dot.modifiedTime	= targetDirNode.modifiedTime;
			dotdot.modifiedTime = targetDirNode.parent.modifiedTime;
			out.append(row.apply(dot));
			out.append(row.apply(dotdot));
			fileCount += 2;
		}

		for (VirtualFileSystem.Node node : targetDirNode.children.values()) {
			out.append(row.apply(node));
			if (!node.isDirectory) totalBytes += node.content.length();
			fileCount++;
		}

		out.append(String.format("%5d File(s) %12d bytes free\n",
				fileCount, 655360 - totalBytes));
		return out.toString();
	}
	
	protected String doCd(VirtualFileSystem vfs, String arg) {
		if (arg.isEmpty()) return vfs.getCurrentPath();
		VirtualFileSystem.Node target = vfs.resolvePath(arg);
		if (target != null && target.isDirectory) {
			vfs.setCurrentPath(vfs.getAbsolutePath(target));
			return "";
		}
		return "Invalid directory";
	}

	protected String doMd(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
		if (arg.isEmpty()) return "Required parameter missing";
		String pathStr = arg.replace('/', '\\');
		int lastSlash  = pathStr.lastIndexOf('\\');
		VirtualFileSystem.Node parent = vfs.getCurrentDir();
		String name = pathStr;
		if (lastSlash != -1) {
			String parentPath = pathStr.substring(0, lastSlash);
			name = pathStr.substring(lastSlash + 1);
			parent = parentPath.isEmpty() ? vfs.getRoot() : vfs.resolvePath(parentPath);
		}
		if (parent == null || !parent.isDirectory) return "Path not found";
		if (name.isEmpty()) return "Invalid directory name";
		String upper = vfs.canonicalize(name);
		if (upper.isEmpty()) return "Invalid directory name";
		if (parent.children.containsKey(upper)) return "Directory already exists";
		VirtualFileSystem.Node folder = new VirtualFileSystem.Node(upper, true);
		parent.addChild(folder);
		c.setChanged();
		return "";
	}
	
	protected String doRd(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
		if (arg.isEmpty()) return "Required parameter missing";

		VirtualFileSystem.Node target = vfs.resolvePath(arg);
		if (target == null || !target.isDirectory) {
			return "Invalid path, not a directory, or directory not found.";
		}
		if (target == vfs.getRoot()) {
			return "Attempt to remove root directory ignored.";
		}
		if (!target.children.isEmpty()) {
			return "Directory not empty";
		}

		// Can't remove a directory we're standing in.
		VirtualFileSystem.Node check = vfs.getCurrentDir();
		while (check != null) {
			if (check == target) return "Attempt to remove current directory ignored.";
			check = check.parent;
		}

		if (target.parent != null) {
			target.parent.children.remove(target.name);
			c.setChanged();
		}
		return "";
	}
	
	protected String doCopy(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
		if (arg.isEmpty()) return "Required parameter missing";
		String[] parts = arg.split("\\s+", 2);
		if (parts.length < 2) return "Required parameter missing";

		String srcPath	= parts[0];
		String destPath = parts[1];

		VirtualFileSystem.Node srcNode = vfs.resolvePath(srcPath);
		if (srcNode == null || srcNode.isDirectory) return "File not found";

		VirtualFileSystem.Node destNode = vfs.resolvePath(destPath);
		VirtualFileSystem.Node destParent;
		String destFileName;

		if (destNode != null && destNode.isDirectory) {
			destParent	 = destNode;
			destFileName = srcNode.name;
		} else {
			String clean = destPath.replace('/', '\\');
			int lastSlash = clean.lastIndexOf('\\');
			if (lastSlash != -1) {
				String parentPath = clean.substring(0, lastSlash);
				destFileName = clean.substring(lastSlash + 1);
				destParent	 = parentPath.isEmpty() ? vfs.getRoot() : vfs.resolvePath(parentPath);
			} else {
				destParent	 = vfs.getCurrentDir();
				destFileName = clean;
			}
		}

		if (destParent == null || !destParent.isDirectory) return "Path not found";
		if (destFileName.isEmpty()) return "Invalid file name";

		String upperDest = vfs.canonicalize(destFileName);
		if (upperDest.isEmpty()) return "Invalid file name";
		VirtualFileSystem.Node copied = new VirtualFileSystem.Node(upperDest, false);
		copied.content = srcNode.content;
		copied.modifiedTime = System.currentTimeMillis();
		destParent.addChild(copied);

		c.setChanged();
		return "\t\t1 file(s) copied.";
	}

	protected String doRen(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
		if (arg.isEmpty()) return "Required parameter missing";
		String[] parts = arg.split("\\s+", 2);
		if (parts.length < 2) return "Required parameter missing";

		String targetPath = parts[0];
		String newName	  = parts[1];

		// REN only accepts a bare name as the destination.
		if (newName.contains("\\") || newName.contains("/")) {
			int last = Math.max(newName.lastIndexOf('\\'), newName.lastIndexOf('/'));
			newName = newName.substring(last + 1);
		}

		VirtualFileSystem.Node target = vfs.resolvePath(targetPath);
		if (target == null) return "File not found";

		VirtualFileSystem.Node parent = target.parent;
		if (parent == null) return "Permission denied";

		String upperNew = vfs.canonicalize(newName);
		if (upperNew.isEmpty()) return "Invalid file name";
		if (parent.children.containsKey(upperNew)) {
			return "Duplicate file name or file not found";
		}

		parent.children.remove(target.name.toUpperCase(Locale.ROOT));
		target.name = upperNew;
		target.modifiedTime = System.currentTimeMillis();
		parent.addChild(target);

		c.setChanged();
		return "";
	}

	protected String doDel(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
		if (arg.isEmpty()) return "Required parameter missing";
		VirtualFileSystem.Node target = vfs.resolvePath(arg);
		if (target == null) return "File not found";
		if (target.isDirectory) return "Access denied - target is a directory";
		if (target.parent == null) return "File not found";

		c.getFileSystem().getCurrentDir(); // no-op, keeps linters happy
		boolean removed = target.parent.children.remove(target.name) != null;
		if (removed) c.setChanged();
		return removed ? "" : "File not found";
	}

	protected String doType(VirtualFileSystem vfs, String arg) {
		if (arg.isEmpty()) return "Required parameter missing";
		VirtualFileSystem.Node file = vfs.resolvePath(arg);
		if (file == null || file.isDirectory) return "File not found";
		return file.content;
	}

	protected String doAttrib(VirtualFileSystem vfs, String arg) {
		if (!arg.isEmpty()) return "";
		StringBuilder sb = new StringBuilder();
		for (VirtualFileSystem.Node n : vfs.getCurrentDir().children.values()) {
			sb.append("A\t\t").append(n.name).append('\n');
		}
		return sb.toString();
	}

	protected String doTree(VirtualFileSystem vfs) {
		return "Directory PATH listing\nPath: " + vfs.getCurrentPath() + "\nNo sub-directories exist";
	}

	protected String doEdlin(String arg) {
		if (arg.isEmpty()) return "File name must be specified";
		return "New file\n*";
	}
	
	protected String doVer(ComputerBlockEntity computer) {
		return computer.getComputerType().osVersion;
	}
	
	protected String doDate() {
		String now = new SimpleDateFormat("EEE MM-dd-yyyy").format(new Date());
		return "Current date is " + now + "\nEnter new date (mm-dd-yy):";
	}

	protected String doTime() {
		String now = new SimpleDateFormat("HH:mm:ss.SS").format(new Date());
		return "Current time is " + now + "\nEnter new time:";
	}
	
	protected String doExit() {
		return "__EXIT__";
	}

	// PATH / SET / environment

	protected String doPath(ComputerBlockEntity c, String arg) {
		var env = c.getEnvironment();
		if (arg.isEmpty()) {
			String p = env.getOrDefault("PATH", "");
			return p.isEmpty() ? "No Path" : "PATH=" + p;
		}
		// Strip leading PATH= if provided
		String value = arg.toUpperCase(Locale.ROOT).startsWith("PATH=") ? arg.substring(5) : arg;
		env.put("PATH", value);
		c.setChanged();
		return "";
	}

	protected String doSet(ComputerBlockEntity c, String arg) {
		var env = c.getEnvironment();
		if (arg.isEmpty()) {
			StringBuilder sb = new StringBuilder();
			sb.append("COMSPEC=").append(env.getOrDefault("COMSPEC","C:\\COMMAND.COM")).append('\n');
			sb.append("PATH=").append(env.getOrDefault("PATH", defaultPath())).append('\n');
			sb.append("PROMPT=").append(env.getOrDefault("PROMPT", "$P$G"));
			return sb.toString();
		}
		String[] kv = arg.split("=", 2);
		if (kv.length == 2) {
			env.put(kv[0].toUpperCase(Locale.ROOT), kv[1]);
			c.setChanged();
		}
		return "";
	}

	// Executable resolution

	/**
	 * Search PATH for NAME.EXE / NAME.COM / NAME.BAT and launch the registered
	 * Java handler. Returns an output string, or null if nothing matched.
	 */
	protected String tryLaunchExecutable(ComputerBlockEntity computer,
										 VirtualFileSystem vfs,
										 String name, String args) {
		var env = computer.getEnvironment();
		String path = env.getOrDefault("PATH", defaultPath());

		for (String dir : path.split(";")) {
			if (dir.isEmpty()) continue;
			VirtualFileSystem.Node dirNode = vfs.resolvePath(dir);
			if (dirNode == null || !dirNode.isDirectory) continue;

			for (String ext : new String[]{ ".EXE", ".COM", ".BAT" }) {
				VirtualFileSystem.Node file = dirNode.children.get(name + ext);
				if (file == null || file.isDirectory) continue;

				ExecutableRegistry.Entry entry = ExecutableRegistry.get(name + ext);
				if (entry == null) continue;

				// Validate the file header — a corrupted executable must not run.
				if (!entry.matchesHeader(file.content)) {
					return "Bad " + ext.substring(1) + " header";
				}

				String output = entry.run(computer, args, file);
				if (output != null && output.startsWith("APP_LAUNCH:")) {
					return output;
				}
				return output == null ? "" : output;
			}
		}
		return null;
	}

	protected abstract String helpFor(String cmd);

	protected static String formatDosDate(long millis) {
		return new SimpleDateFormat("MM-dd-yy").format(new Date(millis));
	}

	protected static String formatDosTime(long millis) {
		String t = new SimpleDateFormat("hh:mma").format(new Date(millis)).toLowerCase(Locale.ROOT);
		// "06:00am" → "6:00a"
		return t.replace(":00", ":00").replace("am", "a").replace("pm", "p");
	}
}
