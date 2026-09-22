package com.eliaslucky.furniture.blocks.computer.processors;

import com.eliaslucky.furniture.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.furniture.blocks.computer.VirtualFileSystem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/* TODO: MADE IT INTO BASE ABSTRACT CLASS */
// TODO: COPY ALL INTO ABSTRACTDOSCOMMANDPROCESSOR.JAVA
public class DosCommandProcessor implements ICommandProcessor {

	@Override
	public String getPrompt(String currentPath) {
		return currentPath + ">";
	}

	@Override
	public String process(ComputerBlockEntity computer, String rawInput) {
		VirtualFileSystem vfs = computer.getFileSystem();
		String input = rawInput.trim();
		if (input.isEmpty()) return "";

		String[] parts = input.split("\\s+", 2);
		String cmd = parts[0].toUpperCase(Locale.ROOT);
		String arg = parts.length > 1 ? parts[1].trim() : "";

		// MS DOS 6.0 SPECIFIC
		if (cmd.equals("QBASIC") || cmd.equals("QBASIC.EXE") || cmd.equals("EDIT") || cmd.equals("EDIT.COM")) {
			String targetFileName = arg.isEmpty() ? "UNTITLED.BAS" : arg;
			VirtualFileSystem.Node node = vfs.resolvePath(targetFileName);
			
			if (node == null) {
				// Create new virtual file if it doesn't exist
				node = new VirtualFileSystem.Node(targetFileName.toUpperCase(Locale.ROOT), false);
				vfs.getCurrentDir().addChild(node);
				computer.setChanged();
			}

			// Prefix with "APP_LAUNCH:" so packet handler knows to open interactive screen
			return "APP_LAUNCH:QBASIC:" + targetFileName + ":" + node.content;
		}

		switch (cmd) {
			// --- FILE & DIRECTORY NAVIGATION / MANAGEMENT ---
			case "DIR": {
				VirtualFileSystem.Node targetDirNode = arg.isEmpty() ? vfs.getCurrentDir() : vfs.resolvePath(arg);
				if (targetDirNode == null || !targetDirNode.isDirectory) {
					return "Invalid directory";
				}

				// clean up /\/\/\/\
				String displayPath = vfs.getAbsolutePath(targetDirNode).replace("/","\\").replaceAll("\\\\+", "\\\\");
				StringBuilder dirOutput = new StringBuilder("\n Volume in drive C has no label\n Volume Serial Number is 1337-3000\n Directory of ").append(displayPath).append("\n\n");

				// Standardized table columns: Name (12 chars), Type/Size (14 chars), Date (12 chars), Time (8 chars)
				//dirOutput.append(String.format("%-12s %14s	 %10s  %6s\n", ".", "<DIR>", "01-01-1987", "12:00"));
				//dirOutput.append(String.format("%-12s %14s	 %10s  %6s\n", "..", "<DIR>", "01-01-1987", "12:00"));
				int fileCount = 0;
				int totalBytes = 0;

				java.util.function.BiFunction<String, String, String> formatDosLine = (name, typeOrSize) -> {
						String baseName = name;
						String ext = "";

						if (!name.equals(".") && !name.equals("..")) {
								int dotIdx = name.lastIndexOf('.');
								if (dotIdx != -1) {
										baseName = name.substring(0, dotIdx);
										ext = name.substring(dotIdx + 1);
								}
						}
						if (baseName.length() > 8) baseName = baseName.substring(0, 8);
						if (ext.length() > 3) ext = ext.substring(0, 3);
						return String.format("%-8s %-3s   %10s   1-01-87  12:00p\n", baseName.toUpperCase(Locale.ROOT), ext.toUpperCase(Locale.ROOT), typeOrSize);
				};
				// Output . and .. directories if not root
				if (targetDirNode.parent != null) {
						dirOutput.append(formatDosLine.apply(".", "<DIR>"));
						dirOutput.append(formatDosLine.apply("..", "<DIR>"));
						fileCount += 2;
				}

				for (VirtualFileSystem.Node node : targetDirNode.children.values()) {
						if (node.isDirectory) {
								dirOutput.append(formatDosLine.apply(node.name, "<DIR>"));
						}
						else {
								int size = node.content.length();
								dirOutput.append(formatDosLine.apply(node.name, String.valueOf(size)));
								totalBytes += size;
						}
						fileCount++;
				}
				dirOutput.append(String.format("%5d File(s) %12d bytes free\n", fileCount, 655360 - totalBytes));
				return dirOutput.toString();

				/*
				for (VirtualFileSystem.Node node : targetDirNode.children.values()) {
						if (node.isDirectory) {
								dirOutput.append(String.format("%-12s %14s	 %10s  %6s\n", node.name, "<DIR>", "01-01-1987", "12:00"));
						}
						else {
								String sizeStr = String.format("%d bytes", node.content.length());
								dirOutput.append(String.format("%-12s %14s	 %10s  %6s\n", node.name, sizeStr, "01-01-1987", "12:00"));
								fileCount++;
								totalBytes += node.content.length();
						}
				}

				dirOutput.append(String.format("%10d File(s) %12d bytes free", fileCount, 655360 - totalBytes));

				return dirOutput.toString();*/
			}


			case "CD":
			case "CHDIR": {
				if (arg.isEmpty()) return vfs.getCurrentPath();
				VirtualFileSystem.Node targetCd = vfs.resolvePath(arg);
				if (targetCd != null && targetCd.isDirectory) {
					vfs.setCurrentPath(vfs.getAbsolutePath(targetCd));
				}
				else {
					return "Invalid directory";
				}
				return "";
			}
			case "MD":
			case "MKDIR": {
				if (arg.isEmpty()) return "Required parameter missing";
				
				String pathStr = arg.replace('/', '\\');
				int lastSlash = pathStr.lastIndexOf('\\');
				
				VirtualFileSystem.Node parentNode = vfs.getCurrentDir();
				String newDirName = pathStr;

				if (lastSlash != -1) {
					String parentPath = pathStr.substring(0, lastSlash);
					newDirName = pathStr.substring(lastSlash + 1);
					
					if (parentPath.isEmpty()) {
						parentNode = vfs.getRoot();
					} else {
						parentNode = vfs.resolvePath(parentPath);
					}
				}

				if (parentNode == null || !parentNode.isDirectory) return "Path not found";
				if (newDirName.isEmpty()) return "Invalid directory name";
				
				String upperName = newDirName.toUpperCase(Locale.ROOT);
				if (parentNode.children.containsKey(upperName)) return "Directory already exists";

				VirtualFileSystem.Node newFolder = new VirtualFileSystem.Node(upperName, true);
				parentNode.addChild(newFolder);
				computer.setChanged();
				return "";
			}
			case "RD":
			case "RMDIR": {
				if (arg.isEmpty()) return "Required parameter missing";

				VirtualFileSystem.Node targetRm = vfs.resolvePath(arg);
				
				if (targetRm == null || !targetRm.isDirectory) {
					return "Invalid path, not a directory, or directory not found.";
				}

				if (targetRm == vfs.getRoot()) {
					return "Attempt to remove root directory ignored.";
				}

				if (!targetRm.children.isEmpty()) {
					return "Directory not empty";
				}

				VirtualFileSystem.Node checkParent = vfs.getCurrentDir();
				while (checkParent != null) {
					if (checkParent == targetRm) {
						return "Attempt to remove current directory ignored.";
					}
					checkParent = checkParent.parent;
				}

				if (targetRm.parent != null) {
					targetRm.parent.children.remove(targetRm.name);
					computer.setChanged();
				}
				return "";
			}
			case "COPY": {
				if (arg.isEmpty()) return "Required parameter missing";
				String[] copyArgs = arg.split("\\s+", 2);
				if (copyArgs.length < 2) return "Required parameter missing";

				String srcPath = copyArgs[0];
				String destPath = copyArgs[1];

				VirtualFileSystem.Node srcNode = vfs.resolvePath(srcPath);
				if (srcNode == null || srcNode.isDirectory) {
					return "File not found";
				}

				VirtualFileSystem.Node destNode = vfs.resolvePath(destPath);
				VirtualFileSystem.Node destParent;
				String destFileName;

				if (destNode != null && destNode.isDirectory) {
					destParent = destNode;
					destFileName = srcNode.name;
				}
				else {
					String cleanDest = destPath.replace('/', '\\');
					int lastSlash = cleanDest.lastIndexOf('\\');

					if (lastSlash != -1) {
						String parentPath = cleanDest.substring(0, lastSlash);
						destFileName = cleanDest.substring(lastSlash + 1);
						destParent = parentPath.isEmpty() ? vfs.getRoot() : vfs.resolvePath(parentPath);
					}
					else {
						destParent = vfs.getCurrentDir();
						destFileName = cleanDest;
					}
				}

				if (destParent == null || !destParent.isDirectory) {
					return "Path not found";
				}
				if (destFileName.isEmpty()) {
					return "Invalid file name";
				}

				String upperDestName = destFileName.toUpperCase(Locale.ROOT);
				VirtualFileSystem.Node copiedFile = new VirtualFileSystem.Node(upperDestName, false);
				copiedFile.content = srcNode.content;
				destParent.addChild(copiedFile);

				computer.setChanged();
				return "		1 file(s) copied.";
			}
			case "REN":
			case "RENAME": {
				if (arg.isEmpty()) return "Required parameter missing";
				String[] renArgs = arg.split("\\s+", 2);
				if (renArgs.length < 2) return "Required parameter missing";

				String targetPath = renArgs[0];
				String newName = renArgs[1];

				// remove drive letter/path specifiers if present in destination per MS-DOS syntax rules
				if (newName.contains("\\") || newName.contains("/")) {
					int lastSlash = Math.max(newName.lastIndexOf('\\'), newName.lastIndexOf('/'));
					newName = newName.substring(lastSlash + 1);
				}

				VirtualFileSystem.Node targetNode = vfs.resolvePath(targetPath);
				if (targetNode == null) {
					return "File not found";
				}

				VirtualFileSystem.Node parentDir = targetNode.parent;
				if (parentDir == null) {
					return "Permission denied";
				}

				String upperNewName = newName.toUpperCase(Locale.ROOT);
				if (parentDir.children.containsKey(upperNewName)) {
					return "Duplicate file name or file not found";
				}

				parentDir.children.remove(targetNode.name.toUpperCase(Locale.ROOT));
				targetNode.name = upperNewName;
				parentDir.addChild(targetNode);

				computer.setChanged();
				return "";
			}
			case "DEL":
			case "ERASE": {
				if (arg.isEmpty()) return "Required parameter missing";

				VirtualFileSystem.Node targetNode = vfs.resolvePath(arg);
				
				if (targetNode == null) {
					return "File not found";
				}

				if (targetNode.isDirectory) {
					return "Access denied - target is a directory";
				}

				if (targetNode.parent != null) {
					targetNode.parent.children.remove(targetNode.name);
					computer.setChanged();
				}

				return "";
			}
			case "TYPE": {
				if (arg.isEmpty()) return "Required parameter missing";
				VirtualFileSystem.Node f = vfs.getCurrentDir().children.get(arg.toUpperCase(Locale.ROOT));
				if (f != null && !f.isDirectory) return f.content;
				return "File not found";
			}
			// --- SYSTEM & DISK COMMANDS ---
			case "VER":
				return computer.getComputerType().osVersion;

			case "VOL":
				return " Volume in drive C has no label\n Volume Serial Number is 1337-3000";

			case "LABEL":
				return "Volume C: has no label\nVolume label (11 characters, ENTER for none)?";

			case "CHKDSK":
				return "  362496 bytes total disk space\n	28672 bytes in 3 hidden files\n   20480 bytes in 2 directories\n  133120 bytes in 15 user files\n  180224 bytes available on disk\n\n  655360 total bytes memory\n	602880 bytes free";

			case "SYS":
				return "System transferred";

			case "FORMAT":
				return "Insert new diskette for drive A:\nand press ENTER when ready...\nFormatting... Format complete.\n360448 bytes total disk space\n360448 bytes available on disk";

			case "DISKCOPY":
				return "Insert SOURCE diskette in drive A:\nInsert TARGET diskette in drive B:\nPress any key to continue . . .\nCopying 40 tracks, 9 sectors/track, 2 side(s)";

			case "DISKCOMP":
				return "Comparing 40 tracks, 9 sectors per track, 2 side(s)\nCompare OK";

			case "FDISK":
				return "Fixed Disk Setup Program Version 3.00\n(C)Copyright Microsoft Corp. 1983-1987\n\nNo fixed disks present.";

			case "TREE":
				StringBuilder treeOutput = new StringBuilder("Directory PATH listing\nPath: ");
				treeOutput.append(vfs.getCurrentPath()).append("\nNo sub-directories exist");
				return treeOutput.toString();

			case "ATTRIB":
				if (arg.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					for (VirtualFileSystem.Node n : vfs.getCurrentDir().children.values()) {
						sb.append("A			").append(n.name).append("\n");
					}
					return sb.toString();
				}
				return "";

			// --- UTILITY COMMANDS ---
			case "DATE":
				String currentDate = new SimpleDateFormat("EEE MM-dd-yyyy").format(new Date());
				return "Current date is " + currentDate + "\nEnter new date (mm-dd-yy):";

			case "TIME":
				String currentTime = new SimpleDateFormat("HH:mm:ss.SS").format(new Date());
				return "Current time is " + currentTime + "\nEnter new time:";

			case "ECHO":
				return arg.isEmpty() ? "ECHO is on." : arg;

			case "PROMPT":
				return "";

			case "PATH":
				return arg.isEmpty() ? "PATH=C:\\;C:\\DOS" : "";

			case "SET":
				return "COMSPEC=C:\\COMMAND.COM\nPATH=C:\\;C:\\DOS\nPROMPT=$P$G";

			case "FIND":
				return "---------------- " + arg.toUpperCase(Locale.ROOT) + ": Line match not found.";

			case "SORT":
				return arg;

			case "MORE":
				return arg;

			case "EDLIN":
				if (arg.isEmpty()) return "File name must be specified";
				return "New file\n*";

			case "COMMAND":
				return computer.getComputerType().osVersion;

			case "PRINT":
				return "PRINT queue is empty";

			case "ASSIGN":
			case "MODE":
			case "SUBST":
			case "JOIN":
			case "BACKUP":
			case "RESTORE":
			case "RECOVER":
			case "SHARE":
			case "GRAFTABL":
			case "KEYB":
			case "CTTY":
				return "Command executed successfully.";

			case "HELP":
				return "Supported MS-DOS 3.0 Commands:\n" +
					   "ATTRIB, CD, CHKDSK, COPY, DATE, DEL, DIR, DISKCOMP, DISKCOPY, ECHO,\n" +
					   "EDLIN, FDISK, FIND, FORMAT, LABEL, MD, MORE, PATH, PRINT, PROMPT, RD,\n" +
					   "REN, SET, SORT, SYS, TIME, TREE, TYPE, VER, VOL";

			default:
				return "Bad command or file name";
		}
	}
}
