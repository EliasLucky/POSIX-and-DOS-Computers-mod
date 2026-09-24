package com.eliaslucky.mc_dos.blocks.computer.processors.posix;

import com.eliaslucky.mc_dos.api.shell.PipelineExecutor;
import com.eliaslucky.mc_dos.api.shell.ShellDialect;
import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;
import com.eliaslucky.mc_dos.blocks.computer.VirtualFileSystem;

/**
 * Runs a script file as a sequence of shell lines. This is the entry point
 * for `#!/bin/sh script` and for interactive `sh` invocations.
 *
 * NOTE: control flow (if/while/for) is not implemented yet. Everything
 * is line-oriented: each non-blank, non-comment line is parsed as a
 * pipeline and executed.
 */
public final class ShellRunner {

    private final ShellDialect dialect;

    public ShellRunner(ShellDialect dialect) { this.dialect = dialect; }

    public String run(ComputerBlockEntity computer, String args) {
        String[] parts = args.trim().split("\\s+", 2);
        if (parts.length == 0 || parts[0].isEmpty()) {
            return dialect.name() + ": no script specified";
        }

        String scriptPath = parts[0];
        VirtualFileSystem vfs = computer.getFileSystem();
        VirtualFileSystem.Node script = vfs.resolvePath(scriptPath);
        if (script == null || script.isDirectory) {
            return dialect.name() + ": " + scriptPath + ": No such file or directory";
        }

        StringBuilder out = new StringBuilder();
        int lineNumber = 0;
        for (String rawLine : script.content.split("\n", -1)) {
            lineNumber++;
            String line = rawLine.trim();
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;    // comments

            try {
                var pipeline = dialect.parse(line);
                if (pipeline.isEmpty()) continue;
                String result = new PipelineExecutor(
                        com.eliaslucky.mc_dos.blocks.computer.shell.posix.PosixStreamResolver.INSTANCE)
                        .execute(pipeline, computer);
                if (result != null && !result.isEmpty()) {
                    out.append(result);
                    if (!result.endsWith("\n")) out.append('\n');
                }
            } catch (RuntimeException ex) {
                out.append(dialect.name()).append(": line ").append(lineNumber)
                   .append(": ").append(ex.getMessage()).append('\n');
            }
        }
        return out.toString();
    }
}
