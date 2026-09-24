package com.eliaslucky.mc_dos.blocks.computer.fs;

/**
 * POSIX/Linux naming: case-sensitive, no length limit, dots anywhere,
 * spaces allowed. Only '/' and NUL are forbidden — the user can't type
 * NUL anyway, so '/' is the only real filter.
 *
 * "." and ".." are handled by path resolution, not by this policy.
 */
public final class PosixFileNamePolicy implements FileNamePolicy {
	public static final PosixFileNamePolicy INSTANCE = new PosixFileNamePolicy();

	private PosixFileNamePolicy() {}

	@Override
	public String canonicalize(String rawName) {
		if (rawName == null) return "";
		String s = rawName.replace('/', '_').replace('\0', '_');
		if (s.equals(".") || s.equals("..")) return "";
		if (s.trim().isEmpty()) return "";
		return s;
	}

	@Override public boolean caseSensitive() { return true; }
	@Override public String describe() { return "POSIX case-sensitive"; }
	@Override public String pathSeparator() { return "/"; }
	@Override public String rootPrefix()	{ return ""; }
}
