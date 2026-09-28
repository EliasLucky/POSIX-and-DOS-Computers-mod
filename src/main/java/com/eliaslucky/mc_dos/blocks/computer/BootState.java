package com.eliaslucky.mc_dos.blocks.computer;

/**
 * The state of a computer block.
 *
 * <p>A machine moves through these states:
 * <ol>
 *	 <li>{@link #OFF} powered down</li>
 *	 <li>{@link #POST} running the BIOS power-on self test</li>
 *	 <li>{@link #SETUP} the user is in BIOS SETUP</li>
 *	 <li>{@link #RUNNING} the operating system is at a prompt</li>
 * </ol>
 *
 * @since 1.
 */
public enum BootState {
	/** Never booted, or explicitly powered down. */
	OFF,

	/** Running BIOS power-on self test. Shell prompt is inactive. */
	POST,

	/** Inside BIOS SETUP. Shell prompt is inactive. */
	SETUP,

	/** Operating system is loaded. Shell prompt is active. */
	RUNNING
}
