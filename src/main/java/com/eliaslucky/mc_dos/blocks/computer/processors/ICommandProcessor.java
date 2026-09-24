package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileNamePolicy;

public interface ICommandProcessor {
	String process(ComputerBlockEntity computer, String rawInput);
	String getPrompt(String currentPath);
	
	/** Default PATH / search path for this OS. E.g. "C:\\;C:\\DOS" or "/usr/bin:/bin". */
	String defaultPath();
	
	/** Naming rules for files and directories under this OS. */
	FileNamePolicy fileNamePolicy();
	
	/**
	 * Content for a well-known file seeded at install time, or null if this
	 * OS doesn't define content for that file. The block entity calls this
	 * for every file in ComputerType.defaultFiles that isn't an executable.
	 */
	default String defaultFileContent(String fileName) { return null; }
	
	/**
     * Create this OS's kernel. Called by the block entity on power-on.
     * Return null for a minimal shell with no kernel (e.g. a ROM BASIC).
     */
    default Kernel createKernel() { return null; }
    default ShellDialect shellDialect(Kernel kernel) { return null; }
    /**
     * Process with stdin available. The default forwards to `process`,
     * ignoring stdin — correct for shells that don't yet support pipes.
     */
    default String processWithStdin(ComputerBlockEntity computer, String rawInput, String stdin) {
        return process(computer, rawInput);   // default: ignore stdin
    }
}
