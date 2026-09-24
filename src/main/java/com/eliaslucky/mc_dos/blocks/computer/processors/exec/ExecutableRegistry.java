package com.eliaslucky.mc_dos.blocks.computer.processors.exec;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

import java.util.*;

public final class ExecutableRegistry {
	/** A runner turns (computer, args, file) into terminal output. */
    public interface Runner {
        String run(ComputerBlockEntity computer, String args, VirtualFileSystem.Node file);
    }

    public record Entry(
            String osFamily,          // "dos", "posix", "unix"
            String canonicalName,     // "QBASIC.EXE", "/bin/ls"
            ExecutableFormat format,
            String templateContent,   // seeded body for default installs
            Runner runner
    ) {}

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static final Map<String, List<Entry>> BY_FAMILY = new HashMap<>();

    private ExecutableRegistry() {}

    public static void register(String osFamily, String canonicalName,
                                ExecutableFormat format, String template,
                                Runner runner) {
        Entry e = new Entry(
                osFamily.toLowerCase(Locale.ROOT),
                canonicalName.toUpperCase(Locale.ROOT),
                format, template, runner);
        ENTRIES.add(e);
        BY_FAMILY.computeIfAbsent(e.osFamily(), k -> new ArrayList<>()).add(e);
    }

    /** All entries this OS family knows about. */
    public static List<Entry> forFamily(String osFamily) {
        return BY_FAMILY.getOrDefault(osFamily.toLowerCase(Locale.ROOT), List.of());
    }

    /** Exact name lookup within a family. */
    public static Entry get(String osFamily, String name) {
        String up = name.toUpperCase(Locale.ROOT);
        for (Entry e : forFamily(osFamily)) {
            if (e.canonicalName().equals(up)) return e;
        }
        return null;
    }

    /** For seeding files at install — find any template for this name, any family. */
    public static String templateFor(String name) {
        String up = name.toUpperCase(Locale.ROOT);
        for (Entry e : ENTRIES) {
            if (e.canonicalName().equals(up) && e.templateContent() != null) {
                return e.templateContent();
            }
        }
        return null;
    }

    public static List<String> namesFor(String osFamily) {
        List<String> out = new ArrayList<>();
        for (Entry e : forFamily(osFamily)) out.add(e.canonicalName());
        return out;
    }
}
