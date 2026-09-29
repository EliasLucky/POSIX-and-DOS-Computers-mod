package com.eliaslucky.mc_dos.client.apps;

/**
 * A {@link TerminalApplication} that saves files and wants to be told
 * the outcome.
 *
 * <p>The server is the authority on every write. When an app requests
 * a save, the result comes back asynchronously on the next packet. An
 * app that implements this interface receives the outcome and can
 * display it (a status bar, a dialog, a footer message) in whatever
 * way matches its own UI.
 *
 * <p>Apps that don't save files don't implement this. Apps that save
 * but don't care about errors (a scratchpad, say) can return silently
 * from the callback.
 *
 * @since 1.5
 */
public interface FileAwareApp {
	/**
	 * Called when a file-write request completes.
	 *
	 * @param path	  the path that was written
	 * @param success {@code true} if the write succeeded
	 * @param message an error message from the OS's {@link
	 *		  com.eliaslucky.mc_dos.blocks.computer.fs.FileError}
	 *		  taxonomy; empty string on success
	 */
	void onFileWriteResult(String path, boolean success, String message);
}
