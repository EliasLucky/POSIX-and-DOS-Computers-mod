package com.eliaslucky.mc_dos.api.shell;

import java.util.ArrayList;
import java.util.List;

/**
 * A shell's syntax. Subclasses enable or disable operators and tune the
 * separator character. The base class implements one common parser that
 * behaves according to the enabled features.
 */
public abstract class ShellDialect {

    // ── Feature flags ───────────────────────────────────────────────────
    public boolean supportsPipes()         { return true; }
    public boolean supportsLogicalOps()    { return false; }   // && ||
    public boolean supportsBackground()    { return false; }   // &
    public boolean supportsHereDoc()       { return false; }   // <<
    public char    separatorChar()         { return ';'; }

    /** Human name; used by HELP. */
    public abstract String name();

    // ── Parse ───────────────────────────────────────────────────────────
    public Pipeline parse(String line) {
        List<Pipeline.Stage> stages = new ArrayList<>();
        List<LogicalOp> between = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        boolean inSingle = false, inDouble = false;
        LogicalOp pending = null;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            // Quoting: everything inside quotes is literal.
            if (c == '\'' && !inDouble) { inSingle = !inSingle; current.append(c); continue; }
            if (c == '"'  && !inSingle) { inDouble = !inDouble; current.append(c); continue; }
            if (inSingle || inDouble)   { current.append(c); continue; }

            // Separator `;`
            if (c == separatorChar()) {
                if (current.length() > 0) {
                    stages.add(parseStage(current.toString()));
                    current.setLength(0);
                }
                between.add(LogicalOp.SEQ);
                continue;
            }

            // Pipe `|` or logical OR `||`
            if (c == '|' && supportsPipes()) {
                boolean isOr = supportsLogicalOps()
                        && i + 1 < line.length() && line.charAt(i + 1) == '|';
                if (current.length() > 0) {
                    stages.add(parseStage(current.toString()));
                    current.setLength(0);
                }
                between.add(isOr ? LogicalOp.OR : LogicalOp.PIPE);
                if (isOr) i++;
                continue;
            }

            // Logical AND `&&`
            if (c == '&' && supportsLogicalOps()) {
                boolean isAnd = i + 1 < line.length() && line.charAt(i + 1) == '&';
                if (isAnd) {
                    if (current.length() > 0) {
                        stages.add(parseStage(current.toString()));
                        current.setLength(0);
                    }
                    between.add(LogicalOp.AND);
                    i++;
                    continue;
                }
                if (supportsBackground()) {
                    if (current.length() > 0) {
                        stages.add(parseStage(current.toString()));
                        current.setLength(0);
                    }
                    between.add(LogicalOp.BACKGROUND);
                    continue;
                }
            }

            current.append(c);
        }
        if (current.length() > 0) stages.add(parseStage(current.toString()));

        while (between.size() >= stages.size()) between.remove(between.size() - 1);
        return new Pipeline(stages, between);
    }

    /** Parse one stage: command, args, and any redirections. */
    protected Pipeline.Stage parseStage(String raw) {
        List<String> tokens = tokenize(raw);

        String command = "";
        StringBuilder args = new StringBuilder();
        Redirect stdin = null, stdout = null, stderr = null;

        int i = 0;
        if (!tokens.isEmpty()) { command = tokens.get(0); i = 1; }

        while (i < tokens.size()) {
            String tok = tokens.get(i);

            switch (tok) {
                case ">": {
                    String target = (i + 1 < tokens.size()) ? tokens.get(++i) : "";
                    stdout = makeRedirect(target, Redirect.Mode.WRITE);
                    break;
                }
                case ">>": {
                    String target = (i + 1 < tokens.size()) ? tokens.get(++i) : "";
                    stdout = makeRedirect(target, Redirect.Mode.APPEND);
                    break;
                }
                case "<": {
                    String target = (i + 1 < tokens.size()) ? tokens.get(++i) : "";
                    stdin = makeRedirect(target, Redirect.Mode.READ);
                    break;
                }
                default: {
                    if (args.length() > 0) args.append(' ');
                    args.append(tok);
                }
            }
            i++;
        }

        return new Pipeline.Stage(raw, command, args.toString(), stdin, stdout, stderr);
    }

    /**
     * Decide whether a redirection target names a device or a file.
     * DOS: bare names like "NUL", "PRN" — but so is "FOO.TXT". The dialect
     *      can't know without the kernel. Default: assume file. Override
     *      in DOS to consult a device predicate supplied at parse time.
     * UNIX: "/dev/..." is unambiguously a device.
     */
    protected Redirect makeRedirect(String target, Redirect.Mode mode) {
        return new Redirect.File(target, mode);
    }

    // ── Tokenizer ───────────────────────────────────────────────────────
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
                if (sb.length() > 0) { out.add(unquote(sb.toString())); sb.setLength(0); }
                continue;
            }
            if (c == '>' || c == '<') {
                if (sb.length() > 0) { out.add(unquote(sb.toString())); sb.setLength(0); }
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
        if (sb.length() > 0) out.add(unquote(sb.toString()));
        return out;
    }

    private static String unquote(String s) {
        if (s.length() >= 2) {
            char a = s.charAt(0), b = s.charAt(s.length() - 1);
            if ((a == '"' && b == '"') || (a == '\'' && b == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }
}