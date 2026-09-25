package com.eliaslucky.mc_dos.api.exec;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

import java.util.*;

/**
 * Registry of executable files. Register an entry to make
 * a name runnable from the shell.
 *
 * <p>Entries are keyed by (OS family, canonical name). Multiple OS
 * families can register the same name; each resolves independently.
 *
 * <h2>Example</h2>
 * <pre>{@code
 * ExecutableRegistry.register(
 *     "dos",                              // OS family
 *     "HELLO.EXE",                        // canonical name
 *     DosMZFormat.INSTANCE,               // file format
 *     "MZ\u0090\u0000...Hello World",     // seed content
 *     (computer, args, file) -> "Hello, " + args);
 * }</pre>
 */
public final class ExecutableRegistry {
	/**
     * The runtime side of an executable. Receives the computer, the
     * command-line arguments as a single string, and the file node.
     *
     * @since 1.0
     */
    public interface Runner {
    	/**
         * Run the executable.
         *
         * @param computer the machine executing the file
         * @param args everything after the command name on the line
         * @param file the VFS node for the executable
         * @return terminal output; may start with {@code "APP_LAUNCH:"}
         *         to trigger a client-side TUI program
         */
        String run(ComputerBlockEntity computer, String args, VirtualFileSystem.Node file);
    }

    /**
     * A registered executable.
     *
     * @param osFamily  bucket key: {@code "dos"}, {@code "unix"}, {@code "posix"}
     * @param canonicalName the uppercase filename the shell matches against
     * @param format    the format matcher used to validate the file body
     * @param templateContent seed body for a fresh install, may be {@code null}
     * @param runner    the code to run when the file is invoked
     */
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

    /**
     * Register an executable. Safe to call twice; the first entry wins
     * and a warning is logged.
     *
     * @param osFamily  the OS family key
     * @param canonicalName the uppercase filename
     * @param format    the format matcher
     * @param template  seed content, may be {@code null}
     * @param runner    the run handler
     */
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

    /**
     * All entries a specified OS family knows about.
     *
     * @param osFamily the family key
     * @return an immutable list, possibly empty
     */
    public static List<Entry> forFamily(String osFamily) {
        return BY_FAMILY.getOrDefault(osFamily.toLowerCase(Locale.ROOT), List.of());
    }

    /**
     * Look up an executable by the specified family and name.
     *
     * @param osFamily the family key
     * @param name     the filename (case-insensitive)
     * @return the entry, or {@code null} if not registered
     */
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
