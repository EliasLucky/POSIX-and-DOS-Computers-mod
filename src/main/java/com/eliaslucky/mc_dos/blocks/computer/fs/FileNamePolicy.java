package com.eliaslucky.mc_dos.blocks.computer.fs;

/**
 * Per-OS rules for turning a user-typed name into the canonical form
 * stored in the VFS, and for comparing names during lookup.
 */
public interface FileNamePolicy {
	/**
	 * Convert user-typed text into the canonical name stored on disk.
	 * Return "" if the input cannot be a valid name under this policy.
	 * A returned name is expected to be safe as a map key and as
	 * the Node.name field.
	 */
	String canonicalize(String rawName);

	/**
	 * Map a path segment to the key used in {@code children}.
	 * Defaults to canonicalize. Override only if lookup needs
	 * different normalization than storage.
	 */
	default String lookupKey(String segment) {
		return canonicalize(segment);
	}

	/** Does this policy treat ABC and abc as different names? */
	boolean caseSensitive();

	/** Short description — used by HELP and diagnostics. */
	String describe();
}
