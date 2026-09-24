package com.eliaslucky.mc_dos.api.shell;

import java.util.ArrayList;
import java.util.List;

import com.eliaslucky.mc_dos.api.shell.Pipeline.Stage;

public abstract class ShellParser {
    /** Implemented by dialects to say which operator chars exist. */
    protected abstract boolean isRedirectionChar(char c);
    protected abstract boolean isPipeChar(char c);
    protected abstract boolean isSeparatorChar(char c);
    protected abstract boolean isLogicalOpChar(char c);

    /** Parse a full line into stages connected by operators. */
    public Pipeline parse(String line) {
        List<Stage> stages = new ArrayList<>();
        List<LogicalOp> between = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        LogicalOp pendingOp = null;
        boolean inSingle = false, inDouble = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            // Quoting
            if (c == '\'' && !inDouble) { inSingle = !inSingle; current.append(c); continue; }
            if (c == '"' && !inSingle)  { inDouble = !inDouble; current.append(c); continue; }
            if (inSingle || inDouble)   { current.append(c); continue; }

            // Separators / pipes / logical ops
            if (isSeparatorChar(c)) {
                if (current.length() > 0) { stages.add(parseStage(current.toString())); current.setLength(0); }
                between.add(LogicalOp.SEQ);
                continue;
            }
            if (isPipeChar(c)) {
                if (current.length() > 0) { stages.add(parseStage(current.toString())); current.setLength(0); }
                between.add(LogicalOp.PIPE);
                continue;
            }
            if (c == '&' && i + 1 < line.length() && line.charAt(i+1) == '&') {
                if (current.length() > 0) { stages.add(parseStage(current.toString())); current.setLength(0); }
                between.add(LogicalOp.AND);
                i++;
                continue;
            }
            if (c == '|' && i + 1 < line.length() && line.charAt(i+1) == '|') {
                if (current.length() > 0) { stages.add(parseStage(current.toString())); current.setLength(0); }
                between.add(LogicalOp.OR);
                i++;
                continue;
            }

            current.append(c);
        }
        if (current.length() > 0) stages.add(parseStage(current.toString()));

        // Trim trailing `between` if line ended with a separator
        while (between.size() >= stages.size()) between.remove(between.size() - 1);
        return new Pipeline(stages, between);
    }

    /** Split one stage into command / args / redirections. */
    protected Stage parseStage(String text) {
        List<String> tokens = tokenize(text);

        String command = "";
        StringBuilder args = new StringBuilder();
        Redirect stdin = null, stdout = null, stderr = null;

        int i = 0;
        if (!tokens.isEmpty()) { command = tokens.get(0); i = 1; }

        while (i < tokens.size()) {
            String tok = tokens.get(i);

            if (tok.equals(">")) {
                i++;
                if (i >= tokens.size()) break;
                stdout = new Redirect.File(tokens.get(i), Redirect.Mode.TRUNCATE);
            } else if (tok.equals(">>")) {
                i++;
                if (i >= tokens.size()) break;
                stdout = new Redirect.File(tokens.get(i), Redirect.Mode.APPEND);
            } else if (tok.equals("<")) {
                i++;
                if (i >= tokens.size()) break;
                stdin = new Redirect.File(tokens.get(i), Redirect.Mode.READ);
            } else {
                if (args.length() > 0) args.append(' ');
                args.append(tok);
            }
            i++;
        }
        return new Stage(text, command, args.toString(), stdin, stdout, stderr);
    }

    /** Naive whitespace tokenizer that respects quotes and keeps operators as tokens. */
    protected List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inSingle = false, inDouble = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\'' && !inDouble) { inSingle = !inSingle; sb.append(c); continue; }
            if (c == '"'  && !inSingle) { inDouble = !inDouble; sb.append(c); continue; }
            if (inSingle || inDouble)   { sb.append(c); continue; }

            if (Character.isWhitespace(c)) {
                if (sb.length() > 0) { out.add(sb.toString()); sb.setLength(0); }
                continue;
            }
            if (c == '>' || c == '<') {
                if (sb.length() > 0) { out.add(sb.toString()); sb.setLength(0); }
                // Check for `>>`
                if (c == '>' && i + 1 < text.length() && text.charAt(i + 1) == '>') {
                    out.add(">>");
                    i++;
                } else {
                    out.add(String.valueOf(c));
                }
                continue;
            }
            sb.append(c);
        }
        if (sb.length() > 0) out.add(sb.toString());
        return out;
    }
}
