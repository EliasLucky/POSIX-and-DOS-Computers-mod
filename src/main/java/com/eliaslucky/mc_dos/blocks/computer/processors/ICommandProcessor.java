package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;
import com.eliaslucky.mc_dos.api.shell.StreamResolver;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileNamePolicy;
import com.eliaslucky.mc_dos.blocks.computer.shell.dos.DosStreamResolver;

/**
 * The command language of an OS. Implementations provide the built-in
 * command set, the shell syntax, and the kernel factory.
 *
 * <p>This is the primary extension point for a new OS family. Most
 * addons will not implement this interface; they will instead register
 * individual executables via {@code ExecutableRegistry} and individual
 * drivers via {@code DriverRegistry}.
 *
 * <h2>Typical implementations</h2>
 * <ul>
 *   <li>{@code Dos3CommandProcessor} / {@code Dos6CommandProcessor}</li>
 *   <li>{@code UnixV7CommandProcessor}</li>
 *   <li>{@code LinuxCommandProcessor}</li>
 * </ul>
 */
public interface ICommandProcessor {
	/**
     * Execute a command line and return terminal output.
     *
     * @param computer the machine executing the command
     * @param rawInput the raw text as typed by the user
     * @return the output to display, or {@code ""} for none
     */
	String process(ComputerBlockEntity computer, String rawInput);
	/**
     * The prompt shown before the user's cursor.
     *
     * @param currentPath the current directory
     * @return the prompt string, never {@code null}
     */
	String getPrompt(String currentPath);
	
	/**
     * Default search path for executables.
     *
     * @return a PATH-style string, never {@code null}
     */
	String defaultPath();
	
	/**
     * Naming rules for files and directories.
     *
     * @return the policy, never {@code null}
     */
	FileNamePolicy fileNamePolicy();
	
	/**
     * Content for well-known files seeded at install time. Called for
     * each file in {@code ComputerType.defaultFiles} that is not an
     * executable.
     *
     * @param fileName the canonical filename, e.g. {@code "CONFIG.SYS"}
     * @return the file body, or {@code null} if this processor doesn't
     *         define content for that file
     */
	default String defaultFileContent(String fileName) { return null; }
	
	/**
     * Create the OS kernel. Called on power-on. Return {@code null}
     * for a minimal shell without a kernel (e.g. a ROM BASIC).
     *
     * @return a fresh kernel instance, or {@code null}
     */
    default Kernel createKernel() { return null; }
    /**
     * The shell syntax this OS uses.
     *
     * @param kernel the current kernel, may be {@code null}
     * @return the dialect, or {@code null} for no shell features
     */
    default ShellDialect shellDialect(Kernel kernel) { return null; }
    /**
     * OS family identifier. Used to bucket executables and drivers.
     * Common values: {@code "dos"}, {@code "unix"}, {@code "linux"}.
     *
     * @return the family key, never {@code null}
     */
    default String osFamily() { return "dos"; }

    /**
     * Factory for the stream resolver that handles redirection and
     * pipe carry-over.
     *
     * @return the resolver, never {@code null}
     */
    default StreamResolver createStreamResolver() {
        return DosStreamResolver.INSTANCE;
    }
    /**
     * Same as {@link #process(ComputerBlockEntity, String)} but with
     * stdin supplied by a pipe or {@code <} redirect. Commands that
     * read stdin override this; the default ignores stdin.
     *
     * @param computer the machine executing the command
     * @param rawInput the command line
     * @param stdin the piped or redirected input, never {@code null}
     * @return the output, never {@code null}
     */
    default String processWithStdin(ComputerBlockEntity computer, String rawInput, String stdin) {
        return process(computer, rawInput);   // default: ignore stdin
    }
}
