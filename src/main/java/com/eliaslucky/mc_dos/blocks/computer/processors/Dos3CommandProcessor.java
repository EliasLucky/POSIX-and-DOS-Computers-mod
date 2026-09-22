package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

// MS-DOS 3.3
public class Dos3CommandProcessor extends AbstractDosCommandProcessor {
	@Override public String defaultPath() { return "C:\\DOS"; }

	@Override protected boolean supportsSlashQuestionHelp() { return false; }

	@Override protected String newFileTemplate(String ext) {
		return ""; // no editor is shipped
	}

	@Override
	protected String helpFor(String cmd) {
		return null;
	}

	@Override
	protected String handleVersionSpecific(ComputerBlockEntity c, VirtualFileSystem vfs, String cmd, String arg, String rawArg) {
		switch (cmd) {
			case "MOVE":
	        case "DELTREE":
	        case "UNDELETE":
	        case "EDIT":
	        case "QBASIC":
	        case "DOSKEY":
	        case "MEM":
	        case "CHOICE":
	        case "DEFRAG":
	        case "HELP":
	        case "MSAV":
	        case "MSBACKUP":
	        	return "Bad command or file name";
		}
		return null;
	}

	@Override
	public String getPrompt(String currentPath) {
		return currentPath + ">";
	}
}
