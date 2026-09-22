package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

public interface ICommandProcessor {
	String process(ComputerBlockEntity computer, String rawInput);
	String getPrompt(String currentPath);
	
	/** Default PATH / search path for this OS. E.g. "C:\\;C:\\DOS" or "/usr/bin:/bin". */
    String defaultPath();
}
