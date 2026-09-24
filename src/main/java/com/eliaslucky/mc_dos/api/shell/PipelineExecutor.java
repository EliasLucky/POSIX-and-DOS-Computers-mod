package com.eliaslucky.mc_dos.api.shell;

import com.eliaslucky.mc_dos.blocks.computer.ComputerBlockEntity;

import java.nio.charset.StandardCharsets;

/**
 * Runs a parsed pipeline against a computer. Handles buffered pipes and
 * redirections; the command processor's `process` is called once per
 * stage with the accumulated stdin (empty string if none).
 */
public class PipelineExecutor {
    private final StreamResolver resolver;

    public PipelineExecutor(StreamResolver resolver) {
        this.resolver = resolver;
    }

    /** Returns the output that should be shown on the terminal. */
    public String execute(Pipeline pipeline, ComputerBlockEntity computer) {
        StringBuilder terminalOutput = new StringBuilder();
        byte[] carryover = null;    // pipe buffer between stages

        for (int i = 0; i < pipeline.stages.size(); i++) {
            Pipeline.Stage stage = pipeline.stages.get(i);

            LogicalOp incoming = (i > 0) ? pipeline.between.get(i - 1) : null;

            // `;` boundary resets the pipe buffer.
            if (incoming == LogicalOp.SEQ) carryover = null;

            // Determine stdin: explicit `<` beats pipe carryover.
            String stdin;
            if (stage.stdin != null) {
                stdin = new String(resolver.readAll(stage.stdin, computer), StandardCharsets.UTF_8);
            } else if (carryover != null) {
                stdin = new String(carryover, StandardCharsets.UTF_8);
            } else {
                stdin = "";
            }

            // Run the command. The processor decides whether to consume stdin.
            String fullLine = stage.args.isEmpty()
                    ? stage.command
                    : stage.command + " " + stage.args;

            String output = computer.getComputerType().commandProcessor
                    .processWithStdin(computer, fullLine, stdin);
            if (output == null) output = "";

            byte[] outputBytes = output.getBytes(StandardCharsets.UTF_8);

            // Determine stdout: explicit `>` beats piping to next stage.
            if (stage.stdout != null) {
                resolver.writeAll(stage.stdout, outputBytes, computer);
                carryover = null;
            } else if (i < pipeline.stages.size() - 1
                    && pipeline.between.get(i) == LogicalOp.PIPE) {
                carryover = outputBytes;
            } else {
                terminalOutput.append(output);
                carryover = null;
            }
        }
        return terminalOutput.toString();
    }
}