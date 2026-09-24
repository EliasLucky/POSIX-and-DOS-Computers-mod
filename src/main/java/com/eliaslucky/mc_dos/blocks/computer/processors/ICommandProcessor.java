package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;
import com.eliaslucky.mc_dos.api.shell.StreamResolver;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileNamePolicy;
import com.eliaslucky.mc_dos.blocks.computer.shell.dos.DosStreamResolver;

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
    /** OS family key — "dos", "unix", "posix". Used to bucket executables. */
    default String osFamily() { return "dos"; }

    /** Resolver for redirects and pipe carry-over. */
    default StreamResolver createStreamResolver() {
        return DosStreamResolver.INSTANCE;
    }
    /**
     * Same as process(), but with stdin provided. Commands that read
     * from stdin (find, sort, more, grep) consume it; everything else
     * ignores it. The default just calls process().
     */
    default String processWithStdin(ComputerBlockEntity computer, String rawInput, String stdin) {
        return process(computer, rawInput);   // default: ignore stdin
    }
}
