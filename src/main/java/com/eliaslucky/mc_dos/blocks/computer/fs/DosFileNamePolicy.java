package com.eliaslucky.mc_dos.blocks.computer.fs;

import java.util.Locale;

/**
 * MS-DOS 8.3 naming: uppercase, base truncated to 8, extension to 3,
 * illegal characters stripped, last dot separates base from extension.
 * Lookups are case-insensitive because everything is stored uppercase.
 */
public final class DosFileNamePolicy implements FileNamePolicy {
	public static final DosFileNamePolicy INSTANCE = new DosFileNamePolicy();

	private DosFileNamePolicy() {}

	@Override
	public String canonicalize(String rawName) {
		if (rawName == null) return "";
		String s = rawName.trim();
		if (s.isEmpty()) return "";

		// A single "name" is one path component; drop any leading directories.
		s = s.replace('\\', '/');
		int slash = s.lastIndexOf('/');
		if (slash >= 0) s = s.substring(slash + 1);

		s = s.toUpperCase(Locale.ROOT);

		String base, ext = "";
		int dot = s.lastIndexOf('.');
		if (dot >= 0) {
			base = s.substring(0, dot);
			ext  = s.substring(dot + 1);
		} else {
			base = s;
		}

		// DOS-legal filename characters only.
		// Allowed: A–Z 0–9 ! # $ % & ' ( ) - @ ^ _ ` { } ~
		String legal = "[^A-Z0-9!#$%&'()\\-@^_`{}~]";
		base = base.replaceAll(legal, "");
		ext  = ext.replaceAll(legal, "");

		if (base.length() > 8) base = base.substring(0, 8);
		if (ext.length()  > 3) ext	= ext.substring(0, 3);

		if (base.isEmpty() && ext.isEmpty()) return "";
		return ext.isEmpty() ? base : base + "." + ext;
	}

	@Override public boolean caseSensitive() { return false; }
	@Override public String describe() { return "MS-DOS 8.3 uppercase"; }
}
