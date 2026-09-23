package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

public interface ICommandProcessor {
	String process(ComputerBlockEntity computer, String rawInput);
	String getPrompt(String currentPath);
	
	/** Default PATH / search path for this OS. E.g. "C:\\;C:\\DOS" or "/usr/bin:/bin". */
    String defaultPath();
    
    /**
     * Content for a well-known file seeded at install time, or null if this
     * OS doesn't define content for that file. The block entity calls this
     * for every file in ComputerType.defaultFiles that isn't an executable.
     */
    default String defaultFileContent(String fileName) { return null; }
}
