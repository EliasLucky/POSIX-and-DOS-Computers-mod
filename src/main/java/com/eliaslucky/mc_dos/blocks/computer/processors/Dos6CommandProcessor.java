package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

// MS-DOS 6.0
public class Dos6CommandProcessor extends AbstractDosCommandProcessor {
	@Override public String defaultPath() { return "C:\\DOS;C:\\"; }

	@Override protected boolean supportsSlashQuestionHelp() { return true; }

	@Override protected String newFileTemplate(String ext) {
		return ext.equalsIgnoreCase("BAS") ? "CLS\n" : "";
	}

	@Override
	protected String helpFor(String cmd) {
		return switch (cmd) {
			case "DIR"	-> "Displays a list of files and subdirectories in a directory.\n\nDIR [drive:][path][filename] [/P] [/W]";
			case "CD"	-> "Displays the name of or changes the current directory.\n\nCHDIR [/D] [drive:][path]\nCD [..]";
			case "DEL"	-> "Deletes one or more files.\n\nDEL [drive:][path]filename [/P]";
			case "MD"	-> "Creates a directory.\n\nMKDIR [drive:]path\nMD [drive:]path";
			case "PATH" -> "Displays or sets a search path for executable files.\n\nPATH [[drive:]path[;...]]";
			default -> null;
		};
	}

	@Override
	protected String handleVersionSpecific(ComputerBlockEntity c, VirtualFileSystem vfs, String cmd, String arg, String rawArg) {
		switch (cmd) {
			case "MOVE":	/* new in 6.0 */ break;
			case "DELTREE": /* new in 6.0 */ break;
		}
		return null;
	}

	@Override
	public String getPrompt(String currentPath) {
		return currentPath + ">";
	}
	
	@Override
	public String defaultFileContent(String fileName) {
	    return switch (fileName.toUpperCase(java.util.Locale.ROOT)) {
	        case "AUTOEXEC.BAT" ->
	                "@ECHO OFF\n" +
	                "PROMPT $P$G\n" +
	                "PATH C:\\DOS;C:\\\n" +
	                "SET TEMP=C:\\DOS";
	        case "CONFIG.SYS" ->
	                "FILES=30\n" +
	                "BUFFERS=20\n" +
	                "DEVICE=C:\\DOS\\HIMEM.SYS";
	        default -> null;
	    };
	}
}
