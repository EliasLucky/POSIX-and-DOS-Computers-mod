package com.eliaslucky.mc_dos.blocks.computer.processors;

import com.eliaslucky.mc_dos.api.exec.ExecutableRegistry;
import com.eliaslucky.mc_dos.api.hardware.Kernel;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;
import com.eliaslucky.mc_dos.api.shell.StreamResolver;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileError;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileNamePolicy;
import com.eliaslucky.mc_dos.blocks.computer.fs.FileOpResult;
import com.eliaslucky.mc_dos.blocks.computer.fs.PosixFileNamePolicy;
import com.eliaslucky.mc_dos.blocks.computer.kernel.linux.LinuxKernel;
import com.eliaslucky.mc_dos.blocks.computer.shell.linux.BashDialect;
import com.eliaslucky.mc_dos.blocks.computer.shell.posix.PosixStreamResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Debian GNU/Linux 3.0 (woody), bash shell.
 *
 * <p>POSIX conventions throughout: case-sensitive names, forward-slash
 * paths, GNU-style error strings. Where the base class hierarchy is
 * DOS-specific, this class does <em>not</em> extend it — POSIX and DOS
 * have different error wording, different built-in commands, and
 * different shell syntax. Sharing an abstract base between them would
 * force every override to undo something.
 *
 * @since 1.5
 */
public class LinuxCommandProcessor implements ICommandProcessor {
    @Override public String osFamily()       { return "posix"; }
    @Override public String defaultPath()    { return "/bin:/usr/bin:/usr/local/bin"; }
    @Override public FileNamePolicy fileNamePolicy() { return PosixFileNamePolicy.INSTANCE; }

    @Override public Kernel createKernel()   { return new LinuxKernel(); }
    @Override public ShellDialect shellDialect(Kernel kernel) { return new BashDialect(); }
    @Override public StreamResolver createStreamResolver()    { return PosixStreamResolver.INSTANCE; }

    @Override public String getPrompt(String currentPath) {
        return "root@p4-server:" + currentPath + "# ";
    }

    @Override
    public String process(ComputerBlockEntity computer, String rawInput) {
        return processWithStdin(computer, rawInput, "");
    }

    @Override
    public String processWithStdin(ComputerBlockEntity computer, String rawInput, String stdin) {
        VirtualFileSystem vfs = computer.getFileSystem();
        String input = rawInput.trim();
        if (input.isEmpty()) return "";

        String[] parts = input.split("\\s+", 2);
        String cmd = parts[0];
        String arg = parts.length > 1 ? parts[1].trim() : "";

        // Shell built-ins (owned by bash, not by a /bin/ executable)
        switch (cmd) {
            case "cd":    return doCd(vfs, arg);
            case "pwd":   return vfs.getCurrentPath();
            case "echo":  return arg;
            case "exit":  return "__EXIT__";
            case "clear": return "__CLEAR__";
        }

        // External command search
        String external = tryLaunchExternal(computer, vfs, cmd, arg, stdin);
        if (external != null) return external;

        // Inline built-ins (shipped as Java rather than a binary)
        switch (cmd) {
            case "ls":    return doLs(vfs, arg);
            case "cat":   return doCat(vfs, stdin, arg);
            case "mkdir": return doMkdir(vfs, computer, arg);
            case "rmdir": return doRmdir(vfs, computer, arg);
            case "rm":    return doRm(vfs, computer, arg);
            case "cp":    return doCp(vfs, computer, arg);
            case "mv":    return doMv(vfs, computer, arg);
            case "touch": return doTouch(vfs, computer, arg);
            case "chmod": return doChmod(vfs, computer, arg);
            case "grep":  return doGrep(stdin, arg);
            case "sort":  return doSort(stdin);
            case "wc":    return doWc(stdin);
            case "df":    return doDf(vfs);
            case "date":  return new java.util.Date().toString();
            case "who":   return "root     tty0     2003-08-14 12:00";
            case "uname": return "Linux p4-server 2.4.20-8 #1 SMP Mon Mar 13 i686 GNU/Linux";
            case "man":   return doMan(vfs, arg);
        }

        return "bash: " + cmd + ": command not found";
    }

    private String tryLaunchExternal(ComputerBlockEntity computer, VirtualFileSystem vfs, String name, String args, String stdin) {
        String path = computer.getEnvironment().getOrDefault("PATH", defaultPath());

        for (String dir : path.split(":")) {
            if (dir.isEmpty()) continue;
            VirtualFileSystem.Node dirNode = vfs.resolvePath(dir);
            if (dirNode == null || !dirNode.isDirectory) continue;

            VirtualFileSystem.Node file = dirNode.children.get(name);
            if (file == null || file.isDirectory) continue;

            // Shebang dispatch.
            if (file.content != null && file.content.startsWith("#!")) {
                int nl = file.content.indexOf('\n');
                String shebang = nl < 0 ? file.content.substring(2)
                                        : file.content.substring(2, nl);
                String interpreter = shebang.trim().split("\\s+")[0];
                var entry = ExecutableRegistry.get(osFamily(), interpreter.toUpperCase(Locale.ROOT));
                if (entry == null) {
                    return dir + "/" + name + ": bad interpreter: " + interpreter;
                }
                String full = dir + "/" + name + (args.isEmpty() ? "" : " " + args);
                return entry.runner().run(computer, full, file);
            }

            // Registered binary.
            var entry = ExecutableRegistry.get(osFamily(), (dir + "/" + name).toUpperCase(Locale.ROOT));
            if (entry == null) continue;
            if (!entry.format().matches(name, file)) continue;
            return entry.runner().run(computer, args, file);
        }
        return null;
    }

    // Shell built-ins

    private String doCd(VirtualFileSystem vfs, String arg) {
        if (arg.isEmpty() || arg.equals("~")) {
            return vfs.setCurrentPath("/") ? "" : "cd: /: unreachable";
        }
        if (!vfs.setCurrentPath(arg)) {
            return "bash: cd: " + arg + ": No such file or directory";
        }
        return "";
    }

    private String doLs(VirtualFileSystem vfs, String arg) {
        VirtualFileSystem.Node dir = arg.isEmpty()
                ? vfs.getCurrentDir()
                : vfs.resolvePath(arg);
        if (dir == null || !dir.isDirectory) {
            return "ls: cannot access '" + arg + "': No such file or directory";
        }
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
        if (file == null || file.isDirectory) {
            return "cat: " + arg + ": No such file or directory";
        }
        return file.content;
    }

    private String doMan(VirtualFileSystem vfs, String arg) {
        if (arg.isEmpty()) return "What manual page do you want?";
        VirtualFileSystem.Node page = vfs.resolvePath("/usr/share/man/man1/" + arg + ".1");
        if (page == null || page.isDirectory) return "No manual entry for " + arg;
        return page.content;
    }

    private String doDf(VirtualFileSystem vfs) {
        var mount = vfs.findMountFor(vfs.getCurrentDir());
        if (mount == null) {
            return "Filesystem  1K-blocks  Used  Available  Use%  Mounted on\n"
                 + "/dev/hda1     2000000  500    1999500    1%   /";
        }
        long cap = mount.capacityBytes();
        long used = mount.usage().bytesUsed();
        long avail = mount.freeBytes();
        int pct = cap == 0 ? 0 : (int) (used * 100 / cap);
        return String.format(
                "Filesystem  1K-blocks   Used  Available  Use%%  Mounted on%n"
              + "%-11s  %9d  %5d  %9d  %3d%%  %s",
                "removable", cap / 1024, used / 1024, avail / 1024, pct, mount.id());
    }

    // Mutating built-ins — all through the VFS funnel

    private String doMkdir(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        if (arg.isEmpty()) return "mkdir: missing operand";
        FileOpResult r = vfs.createDirectory(arg);
        if (!r.success()) {
            // GNU mkdir uses specific wording per error type.
            return switch (r.error()) {
                case DIRECTORY_PROBLEM -> "mkdir: cannot create directory '" + arg
                        + "': File exists";
                case INVALID_NAME      -> "mkdir: invalid directory name";
                default                -> "mkdir: " + r.messageFor("posix") + ": " + arg;
            };
        }
        c.setChanged();
        return "";
    }

    private String doRmdir(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        if (arg.isEmpty()) return "rmdir: missing operand";
        VirtualFileSystem.Node target = vfs.resolvePath(arg);
        if (target == null || !target.isDirectory) {
            return "rmdir: failed to remove '" + arg + "': No such file or directory";
        }
        if (!target.children.isEmpty()) {
            return "rmdir: failed to remove '" + arg + "': Directory not empty";
        }
        FileOpResult r = vfs.removeDirectory(arg);
        if (!r.success()) return "rmdir: " + r.messageFor("posix") + ": " + arg;
        c.setChanged();
        return "";
    }

    private String doRm(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        if (arg.isEmpty()) return "rm: missing operand";
        VirtualFileSystem.Node target = vfs.resolvePath(arg);
        if (target == null) return "rm: cannot remove '" + arg + "': No such file or directory";
        if (target.isDirectory) {
            return "rm: cannot remove '" + arg + "': Is a directory";
        }
        FileOpResult r = vfs.deleteFile(arg);
        if (!r.success()) return "rm: " + r.messageFor("posix") + ": " + arg;
        c.setChanged();
        return "";
    }

    private String doCp(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        String[] parts = arg.split("\\s+");
        if (parts.length != 2) return "cp: missing destination file operand";

        VirtualFileSystem.Node src = vfs.resolvePath(parts[0]);
        if (src == null || src.isDirectory) {
            return "cp: cannot stat '" + parts[0] + "': No such file or directory";
        }

        // If destination is a directory, copy inside it.
        VirtualFileSystem.Node destNode = vfs.resolvePath(parts[1]);
        String actualDest = (destNode != null && destNode.isDirectory)
                ? parts[1] + "/" + src.name
                : parts[1];

        FileOpResult r = vfs.copyFile(parts[0], actualDest);
        if (!r.success()) return "cp: " + r.messageFor("posix") + ": " + actualDest;
        c.setChanged();
        return "";
    }

    private String doMv(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        String[] parts = arg.split("\\s+");
        if (parts.length != 2) return "mv: missing destination file operand";

        FileOpResult r = vfs.renameFile(parts[0], parts[1]);
        if (!r.success()) {
            return switch (r.error()) {
                case FILE_NOT_FOUND     -> "mv: cannot stat '" + parts[0] + "': No such file";
                case DIRECTORY_PROBLEM  -> "mv: cannot move '" + parts[0] + "' to '"
                        + parts[1] + "': File exists";
                default                 -> "mv: " + r.messageFor("posix");
            };
        }
        c.setChanged();
        return "";
    }

    private String doTouch(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        if (arg.isEmpty()) return "touch: missing file operand";

        VirtualFileSystem.Node existing = vfs.resolvePath(arg);
        if (existing != null) {
            existing.modifiedTime = System.currentTimeMillis();
            c.setChanged();
            return "";
        }

        FileOpResult r = vfs.writeFile(arg, "");
        if (!r.success()) return "touch: " + r.messageFor("posix") + ": " + arg;
        c.setChanged();
        return "";
    }

    private String doChmod(VirtualFileSystem vfs, ComputerBlockEntity c, String arg) {
        String[] parts = arg.split("\\s+");
        if (parts.length != 2) return "chmod: missing operand";

        VirtualFileSystem.Node f = vfs.resolvePath(parts[1]);
        if (f == null) return "chmod: cannot access '" + parts[1] + "': No such file or directory";

        String mode = parts[0];
        if (mode.equals("+x") || mode.equals("a+x") || mode.equals("755") || mode.equals("u+x")) {
            f.executeBit = true;
        } else if (mode.equals("-x") || mode.equals("a-x") || mode.equals("644")) {
            f.executeBit = false;
        }
        c.setChanged();
        return "";
    }

    // Pipe filters

    private String doGrep(String stdin, String arg) {
        if (arg.isEmpty()) return "grep: missing pattern";
        // Strip surrounding quotes if present.
        String needle = arg;
        if (needle.length() >= 2) {
            char a = needle.charAt(0), b = needle.charAt(needle.length() - 1);
            if ((a == '"' && b == '"') || (a == '\'' && b == '\'')) {
                needle = needle.substring(1, needle.length() - 1);
            }
        }
        StringBuilder out = new StringBuilder();
        for (String line : stdin.split("\n", -1)) {
            if (line.contains(needle)) out.append(line).append('\n');
        }
        return out.toString();
    }

    private String doSort(String stdin) {
        String[] lines = stdin.split("\n", -1);
        java.util.Arrays.sort(lines);
        return String.join("\n", lines);
    }

    private String doWc(String stdin) {
        int lines = stdin.isEmpty() ? 0 : stdin.split("\n", -1).length;
        int words = stdin.trim().isEmpty() ? 0 : stdin.trim().split("\\s+").length;
        return String.format("%7d %7d %7d", lines, words, stdin.length());
    }

    @Override
    public String defaultFileContent(String fileName) {
        return switch (fileName) {
            case "etc/passwd" ->
                    "root:x:0:0:root:/root:/bin/bash\n" +
                    "daemon:x:1:1:daemon:/usr/sbin:/bin/sh\n" +
                    "bin:x:2:2:bin:/bin:/bin/sh";
            case "etc/fstab" ->
                    "/dev/hda1\t/\text3\tdefaults,errors=remount-ro\t0\t1\n" +
                    "/dev/hda2\tnone\tswap\tsw\t0\t0";
            case "etc/hostname" -> "p4-server\n";
            case "root/.bashrc" ->
                    "# ~/.bashrc\nexport PS1='\\u@\\h:\\w\\$ '\n";
            default -> null;
        };
    }
}