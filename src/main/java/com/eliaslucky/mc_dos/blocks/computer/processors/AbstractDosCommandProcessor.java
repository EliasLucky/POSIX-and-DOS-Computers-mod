package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import com.eliaslucky.mc_dos.blocks.computer.processors.exec.ExecutableRegistry;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class AbstractDosCommandProcessor implements ICommandProcessor {

    // ── Version hooks ────────────────────────────────────────────────────

    /** Default PATH used on a fresh machine, e.g. "C:\\DOS;C:\\". */
    protected abstract String defaultPath();

    /** Does this DOS understand `cmd /?` ? (5.0+ yes, 3.x no). */
    protected boolean supportsSlashQuestionHelp() { return false; }

    /** Command names this version adds on top of the shared set. */
    protected boolean handleVersionSpecific(ComputerBlockEntity computer,
                                            VirtualFileSystem vfs,
                                            String cmd, String arg, String rawArg) {
        return false; // default: nothing extra
    }

    /** Template contents for a brand new file created by an editor. */
    protected String newFileTemplate(String extension) { return ""; }

    /** Command name used for "list files": both use DIR but keep it a hook. */
    protected String dirCommandName() { return "DIR"; }

    // ── Shared command entry point ───────────────────────────────────────

    @Override
    public String process(ComputerBlockEntity computer, String rawInput) {
        VirtualFileSystem vfs = computer.getFileSystem();
        String input = rawInput.trim();
        if (input.isEmpty()) return "";

        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toUpperCase(Locale.ROOT);
        String argRaw = parts.length > 1 ? parts[1].trim() : "";
        String arg = argRaw;
   
        if (handleVersionSpecific(computer, vfs, cmd, arg, argRaw)) {
            return "";
        }

        if (supportsSlashQuestionHelp() && arg.equals("/?")) {
            String help = helpFor(cmd);
            if (help != null) return help;
            return "Bad command or file name";
        }

        String launch = tryLaunchExecutable(computer, vfs, cmd, argRaw);
        if (launch != null) return launch;
 
        switch (cmd) {
            case "DIR":  return doDir(vfs, computer, arg);
            case "CD":
            case "CHDIR": return doCd(vfs, arg);
            case "MD":
            case "MKDIR": return doMd(vfs, computer, arg);
            case "RD":
            case "RMDIR": return doRd(vfs, computer, arg);
            case "COPY": return doCopy(vfs, computer, arg);
            case "REN":
            case "RENAME": return doRen(vfs, computer, arg);
            case "DEL":
            case "ERASE": return doDel(vfs, computer, arg);
            case "TYPE": return doType(vfs, arg);
            case "VER": return doVer(computer);
            case "VOL": return " Volume in drive C has no label\n Volume Serial Number is 1337-3000";
            case "DATE": return doDate();
            case "TIME": return doTime();
            case "ECHO": return arg.isEmpty() ? "ECHO is on." : arg;
            case "PROMPT": return "";
            case "PATH": return doPath(computer, arg);
            case "SET": return doSet(computer, argRaw);
            case "TREE": return doTree(vfs);
            case "CLS": return "\u000C"; // client
            case "EXIT": return doExit();
            // ... the rest of the shared switch ...
        }

        // Version error wording.
        String fallback = fallbackUnknown(computer, cmd);
        if (fallback != null) return fallback;

        return unknownCommandMessage();
    }

    protected String unknownCommandMessage() { return "Bad command or file name"; }

    protected String fallbackUnknown(ComputerBlockEntity c, String cmd) { return null; }

    // ── Shared implementations (ports of your existing code) ─────────────

    protected String doCd(VirtualFileSystem vfs, String arg) {
        if (arg.isEmpty()) return vfs.getCurrentPath();
        VirtualFileSystem.Node target = vfs.resolvePath(arg);
        if (target != null && target.isDirectory) {
            vfs.setCurrentPath(vfs.getAbsolutePath(target));
            return "";
        }
        return "Invalid directory";
    }

    protected String doMd(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        if (arg.isEmpty()) return "Required parameter missing";
        String pathStr = arg.replace('/', '\\');
        int lastSlash  = pathStr.lastIndexOf('\\');
        VirtualFileSystem.Node parent = vfs.getCurrentDir();
        String name = pathStr;
        if (lastSlash != -1) {
            String parentPath = pathStr.substring(0, lastSlash);
            name = pathStr.substring(lastSlash + 1);
            parent = parentPath.isEmpty() ? vfs.getRoot() : vfs.resolvePath(parentPath);
        }
        if (parent == null || !parent.isDirectory) return "Path not found";
        if (name.isEmpty()) return "Invalid directory name";
        String upper = name.toUpperCase(Locale.ROOT);
        if (parent.children.containsKey(upper)) return "Directory already exists";
        VirtualFileSystem.Node folder = new VirtualFileSystem.Node(upper, true);
        parent.addChild(folder);
        c.setChanged();
        return "";
    }

    protected String doDel(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        if (arg.isEmpty()) return "Required parameter missing";
        VirtualFileSystem.Node target = vfs.resolvePath(arg);
        if (target == null) return "File not found";
        if (target.isDirectory) return "Access denied - target is a directory";

        // Try to prevent deleting an in-use executable that the running shell is currently executing.
        // We don't block: DOS lets you delete the file even while it runs. But note the side effect.
        if (target.parent != null) {
            target.parent.children.remove(target.name);
            c.setChanged();
        }
        return "";
    }

    // ... port the rest of your switch bodies here unchanged ...

    // ── PATH / SET / environment ─────────────────────────────────────────

    protected String doPath(ComputerBlockEntity c, String arg) {
        var env = c.getEnvironment();
        if (arg.isEmpty()) {
            String p = env.getOrDefault("PATH", "");
            return p.isEmpty() ? "No Path" : "PATH=" + p;
        }
        // Strip leading PATH= if provided
        String value = arg.toUpperCase(Locale.ROOT).startsWith("PATH=") ? arg.substring(5) : arg;
        env.put("PATH", value);
        c.setChanged();
        return "";
    }

    protected String doSet(ComputerBlockEntity c, String arg) {
        var env = c.getEnvironment();
        if (arg.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("COMSPEC=").append(env.getOrDefault("COMSPEC","C:\\COMMAND.COM")).append('\n');
            sb.append("PATH=").append(env.getOrDefault("PATH", defaultPath())).append('\n');
            sb.append("PROMPT=").append(env.getOrDefault("PROMPT", "$P$G"));
            return sb.toString();
        }
        String[] kv = arg.split("=", 2);
        if (kv.length == 2) {
            env.put(kv[0].toUpperCase(Locale.ROOT), kv[1]);
            c.setChanged();
        }
        return "";
    }

    // ── Executable resolution ────────────────────────────────────────────

    /**
     * Search PATH for NAME.EXE / NAME.COM / NAME.BAT and launch the registered
     * Java handler. Returns an output string, or null if nothing matched.
     */
    protected String tryLaunchExecutable(ComputerBlockEntity computer,
                                         VirtualFileSystem vfs,
                                         String name, String args) {
        var env = computer.getEnvironment();
        String path = env.getOrDefault("PATH", defaultPath());

        for (String dir : path.split(";")) {
            if (dir.isEmpty()) continue;
            VirtualFileSystem.Node dirNode = vfs.resolvePath(dir);
            if (dirNode == null || !dirNode.isDirectory) continue;

            for (String ext : new String[]{ ".EXE", ".COM", ".BAT" }) {
                VirtualFileSystem.Node file = dirNode.children.get(name + ext);
                if (file == null || file.isDirectory) continue;

                ExecutableRegistry.Entry entry = ExecutableRegistry.get(name + ext);
                if (entry == null) continue;

                // Validate the file header — a corrupted executable must not run.
                if (!entry.matchesHeader(file.content)) {
                    return "Bad " + ext.substring(1) + " header";
                }

                String output = entry.run(computer, args, file);
                if (output != null && output.startsWith("APP_LAUNCH:")) {
                    return output;
                }
                return output == null ? "" : output;
            }
        }
        return null;
    }

    // ── Help system ──────────────────────────────────────────────────────

    protected abstract String helpFor(String cmd);

    // ── Small utilities reused by subclasses ────────────────────────────

    protected static String formatDosDate(long millis) {
        return new SimpleDateFormat("MM-dd-yy").format(new Date(millis));
    }

    protected static String formatDosTime(long millis) {
        String t = new SimpleDateFormat("hh:mma").format(new Date(millis)).toLowerCase(Locale.ROOT);
        // "06:00am" → "6:00a"
        return t.replace(":00", ":00").replace("am", "a").replace("pm", "p");
    }
}
