package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.api.exec.ExecutableRegistry;
import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;
import com.eliaslucky.mc_dos.api.shell.StreamResolver;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileNamePolicy;
import com.eliaslucky.mc_dos.blocks.computer.fs.PosixFileNamePolicy;
import com.eliaslucky.mc_dos.blocks.computer.kernel.unix.UnixV7Kernel;
import com.eliaslucky.mc_dos.blocks.computer.processors.posix.ShellRunner;
import com.eliaslucky.mc_dos.blocks.computer.shell.posix.PosixStreamResolver;
import com.eliaslucky.mc_dos.blocks.computer.shell.unix.BourneV7Dialect;
import com.eliaslucky.mc_dos.blocks.computer.shell.unix.UnixV7Dialect;

import java.text.SimpleDateFormat;
import java.util.*;

public class UnixV7CommandProcessor implements ICommandProcessor {
    @Override public String defaultPath() { return "/bin:/usr/bin:/usr/local/bin"; }
    @Override public FileNamePolicy fileNamePolicy() { return PosixFileNamePolicy.INSTANCE; }
    @Override public String osFamily() { return "unix"; }

    @Override public Kernel createKernel() { return new UnixV7Kernel(); }

    @Override public ShellDialect shellDialect(Kernel kernel) {
        return new UnixV7Dialect(kernel);
    }

    @Override public StreamResolver createStreamResolver() {
        return PosixStreamResolver.INSTANCE;
    }

    @Override public String getPrompt(String currentPath) {
        // Bourne shell on v7 was typically PS1="% " or "# " for root.
        return "# ";
    }

    @Override
    public String process(ComputerBlockEntity computer, String rawInput) {
        return processWithStdin(computer, rawInput, "");
    }

    @Override
    public String processWithStdin(ComputerBlockEntity computer,
                                   String rawInput, String stdin) {
        VirtualFileSystem vfs = computer.getFileSystem();
        String input = rawInput.trim();
        if (input.isEmpty()) return "";

        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String arg = parts.length > 1 ? parts[1].trim() : "";

        // Built-ins the shell owns (real v7 sh has these internal).
        switch (cmd) {
            case "cd":    return doCd(vfs, arg);
            case "pwd":   return vfs.getCurrentPath();
            case "echo":  return arg;
            case "exit":  return "__EXIT__";
            case "clear": return "__CLEAR__";
        }

        // External commands — walk PATH and dispatch.
        String external = tryLaunchExternal(computer, vfs, cmd, arg, stdin);
        if (external != null) return external;

        // Inline built-ins that don't ship as binaries in our VFS.
        switch (cmd) {
            case "ls":    return doLs(vfs, arg);
            case "cat":   return doCat(vfs, stdin, arg);
            case "mkdir": return doMkdir(vfs, computer, arg);
            case "rmdir": return doRmdir(vfs, computer, arg);
            case "rm":    return doRm(vfs, computer, arg);
            case "cp":    return doCp(vfs, computer, arg);
            case "mv":    return doMv(vfs, computer, arg);
            case "chmod": return doChmod(vfs, computer, arg);
            case "grep":  return doGrep(stdin, arg);
            case "sort":  return doSort(stdin);
            case "wc":    return doWc(stdin);
            case "date":  return new Date().toString();
            case "who":   return "root     tty0     Jan  1 00:00";
            case "ps":    return "  PID TTY      TIME CMD\n    1 tty0     0:01 sh";
            case "uname": return "UNIX";
            case "man":   return doMan(vfs, arg);
            case "sh":    return new ShellRunner(BourneV7Dialect.INSTANCE).run(computer, arg);
        }

        return cmd + ": not found";
    }

    //── External resolution (PATH + shebang)
    private String tryLaunchExternal(ComputerBlockEntity computer, VirtualFileSystem vfs, String name, String args, String stdin) {
        String path = computer.getEnvironment().getOrDefault("PATH", defaultPath());

        for (String dir : path.split(":")) {
            if (dir.isEmpty()) continue;
            VirtualFileSystem.Node dirNode = vfs.resolvePath(dir);
            if (dirNode == null || !dirNode.isDirectory) continue;

            VirtualFileSystem.Node file = dirNode.children.get(name);
            if (file == null || file.isDirectory) continue;

            // Shebang?
            if (file.content != null && file.content.startsWith("#!")) {
                int nl = file.content.indexOf('\n');
                String shebang = nl < 0 ? file.content.substring(2)
                                        : file.content.substring(2, nl);
                String[] sh = shebang.trim().split("\\s+");
                String interpreter = sh[0];

                ExecutableRegistry.Entry entry =
                        ExecutableRegistry.get(osFamily(), interpreter.toUpperCase());
                if (entry == null) {
                    return dir + "/" + name + ": bad interpreter: " + interpreter;
                }
                String full = dir + "/" + name + (args.isEmpty() ? "" : " " + args);
                return entry.runner().run(computer, full, file);
            }

            ExecutableRegistry.Entry entry =
                    ExecutableRegistry.get(osFamily(), (dir + "/" + name).toUpperCase());
            if (entry == null) continue;
            if (!entry.format().matches(name, file)) continue;
            return entry.runner().run(computer, args, file);
        }
        return null;
    }

    // Built-in commands
    private String doCd(VirtualFileSystem vfs, String arg) {
        if (arg.isEmpty()) return "";
        VirtualFileSystem.Node target = vfs.resolvePath(arg);
        if (target != null && target.isDirectory) {
            vfs.setCurrentPath(vfs.getAbsolutePath(target));
            return "";
        }
        return arg + ": bad directory";
    }

    private String doLs(VirtualFileSystem vfs, String arg) {
        VirtualFileSystem.Node dir = arg.isEmpty() ? vfs.getCurrentDir() : vfs.resolvePath(arg);
        if (dir == null || !dir.isDirectory) return arg + " unreadable";
        List<String> names = new ArrayList<>(dir.children.keySet());
        Collections.sort(names);
        StringBuilder sb = new StringBuilder();
        for (String n : names) {
            VirtualFileSystem.Node child = dir.children.get(n);
            sb.append(child.isDirectory ? n + "/" : n).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    private String doCat(VirtualFileSystem vfs, String stdin, String arg) {
        if (arg.isEmpty()) return stdin;
        VirtualFileSystem.Node file = vfs.resolvePath(arg);
        if (file == null || file.isDirectory) return "cat: can't open " + arg;
        return file.content;
    }

    private String doMkdir(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        if (arg.isEmpty()) return "mkdir: arg count";
        String canonical = vfs.canonicalize(arg);
        if (canonical.isEmpty()) return "mkdir: bad name";
        if (vfs.getCurrentDir().children.containsKey(canonical)) return "mkdir: " + arg + ": File exists";
        vfs.getCurrentDir().addChild(new VirtualFileSystem.Node(canonical, true));
        c.setChanged();
        return "";
    }

    private String doRmdir(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        if (arg.isEmpty()) return "rmdir: arg count";
        VirtualFileSystem.Node target = vfs.resolvePath(arg);
        if (target == null || !target.isDirectory) return "rmdir: " + arg + ": No such directory";
        if (!target.children.isEmpty()) return "rmdir: " + arg + ": Directory not empty";
        if (target.parent != null) { target.parent.children.remove(target.name); c.setChanged(); }
        return "";
    }

    private String doRm(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        if (arg.isEmpty()) return "rm: arg count";
        VirtualFileSystem.Node target = vfs.resolvePath(arg);
        if (target == null) return "rm: " + arg + ": No such file";
        if (target.isDirectory) return "rm: " + arg + ": is a directory";
        if (target.parent != null) { target.parent.children.remove(target.name); c.setChanged(); }
        return "";
    }

    private String doCp(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        String[] parts = arg.split("\\s+");
        if (parts.length != 2) return "cp: arg count";
        VirtualFileSystem.Node src = vfs.resolvePath(parts[0]);
        if (src == null || src.isDirectory) return "cp: can't open " + parts[0];
        String destName = vfs.canonicalize(parts[1]);
        if (destName.isEmpty()) return "cp: bad name";
        VirtualFileSystem.Node dest = vfs.resolvePath(parts[1]);
        if (dest != null && dest.isDirectory) {
            VirtualFileSystem.Node copy = new VirtualFileSystem.Node(src.name, false);
            copy.content = src.content;
            dest.addChild(copy);
        } else {
            VirtualFileSystem.Node copy = new VirtualFileSystem.Node(destName, false);
            copy.content = src.content;
            vfs.getCurrentDir().addChild(copy);
        }
        c.setChanged();
        return "";
    }

    private String doMv(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        String[] parts = arg.split("\\s+");
        if (parts.length != 2) return "mv: arg count";
        VirtualFileSystem.Node src = vfs.resolvePath(parts[0]);
        if (src == null) return "mv: can't open " + parts[0];
        String newName = vfs.canonicalize(parts[1]);
        if (newName.isEmpty()) return "mv: bad name";
        if (src.parent != null) src.parent.children.remove(src.name);
        src.name = newName;
        vfs.getCurrentDir().addChild(src);
        c.setChanged();
        return "";
    }

    private String doChmod(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        String[] parts = arg.split("\\s+");
        if (parts.length != 2) return "chmod: arg count";
        // For now, treat "+x" as enabling execute.
        if (parts[0].equals("+x")) {
            VirtualFileSystem.Node f = vfs.resolvePath(parts[1]);
            if (f == null) return "chmod: can't access " + parts[1];
            f.executeBit = true;
            c.setChanged();
            return "";
        }
        return "";   // silently accept other modes
    }

    private String doGrep(String stdin, String arg) {
        if (arg.isEmpty()) return "grep: no pattern";
        String needle = arg.replace("\"", "");
        StringBuilder out = new StringBuilder();
        for (String line : stdin.split("\n", -1)) {
            if (line.contains(needle)) out.append(line).append('\n');
        }
        return out.toString();
    }

    private String doSort(String stdin) {
        String[] lines = stdin.split("\n", -1);
        Arrays.sort(lines);
        return String.join("\n", lines);
    }

    private String doWc(String stdin) {
        int lines = stdin.isEmpty() ? 0 : stdin.split("\n", -1).length;
        int words = stdin.trim().isEmpty() ? 0 : stdin.trim().split("\\s+").length;
        return String.format("%7d %7d %7d", lines, words, stdin.length());
    }

    private String doMan(VirtualFileSystem vfs, String arg) {
        if (arg.isEmpty()) return "What manual page do you want?";
        VirtualFileSystem.Node page = vfs.resolvePath("/usr/man/man1/" + arg + ".1");
        if (page == null) return "No manual entry for " + arg;
        return page.content;
    }
}
