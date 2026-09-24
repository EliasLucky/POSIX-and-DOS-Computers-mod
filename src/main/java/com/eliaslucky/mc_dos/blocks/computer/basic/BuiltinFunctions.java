package com.eliaslucky.mc_dos.blocks.computer.basic;

import java.util.List;
import java.util.Random;

/**
 * Built-in functions callable from any expression. Kept stateless except
 * for RND, whose seed lives here (one per JVM). Real QBASIC seeds per
 * program run; we accept a shared Random for simplicity.
 */
public final class BuiltinFunctions {
    private BuiltinFunctions() {}

    private static Random rng = new Random();

    /** Called by RANDOMIZE. */
    public static void seed(long s) { rng = new Random(s); }

    public static Value call(String name, List<Value> args) {
        String n = name.toUpperCase();
        // Strip trailing $ so CHR$ parses as CHR
        if (n.endsWith("$")) n = n.substring(0, n.length() - 1);

        return switch (n) {
            // Numeric
            case "RND"   -> Value.of(rng.nextDouble());
            case "INT"   -> Value.of(Math.floor(num(args, 0)));
            case "FIX"   -> Value.of((long) num(args, 0));
            case "ABS"   -> Value.of(Math.abs(num(args, 0)));
            case "SGN"   -> Value.of(Math.signum(num(args, 0)));
            case "SQR"   -> Value.of(Math.sqrt(Math.max(0, num(args, 0))));
            case "SIN"   -> Value.of(Math.sin(num(args, 0)));
            case "COS"   -> Value.of(Math.cos(num(args, 0)));
            case "TAN"   -> Value.of(Math.tan(num(args, 0)));
            case "ATN"   -> Value.of(Math.atan(num(args, 0)));
            case "EXP"   -> Value.of(Math.exp(num(args, 0)));
            case "LOG"   -> Value.of(Math.log(Math.max(1e-300, num(args, 0))));
            case "TIMER" -> Value.of((System.currentTimeMillis() / 1000.0) % 86400.0);

            // String
            case "LEN"   -> Value.of(str(args, 0).length());
            case "ASC"   -> {
                String s = str(args, 0);
                yield Value.of(s.isEmpty() ? 0 : s.charAt(0));
            }
            case "CHR"   -> Value.of(String.valueOf((char)(int) num(args, 0)));
            case "STR"   -> Value.of(strNum(num(args, 0)));
            case "VAL"   -> Value.of(parseVal(str(args, 0)));
            case "LEFT"  -> {
                String s = str(args, 0);
                int k = Math.min((int) num(args, 1), s.length());
                yield Value.of(s.substring(0, Math.max(0, k)));
            }
            case "RIGHT" -> {
                String s = str(args, 0);
                int k = Math.min((int) num(args, 1), s.length());
                yield Value.of(s.substring(s.length() - Math.max(0, k)));
            }
            case "MID"   -> {
                String s = str(args, 0);
                int start = (int) num(args, 1) - 1;   // QBASIC is 1-based
                int len   = args.size() >= 3 ? (int) num(args, 2) : s.length() - start;
                if (start < 0) start = 0;
                if (start >= s.length()) yield Value.of("");
                int end = Math.min(s.length(), start + len);
                yield Value.of(s.substring(start, end));
            }
            case "UCASE" -> Value.of(str(args, 0).toUpperCase());
            case "LCASE" -> Value.of(str(args, 0).toLowerCase());

            default -> Value.of(0);
        };
    }

    private static double num(List<Value> a, int i) { return i < a.size() ? a.get(i).asNumber() : 0; }
    private static String str(List<Value> a, int i) { return i < a.size() ? a.get(i).asString() : ""; }

    private static String strNum(double d) {
        // QBASIC's STR$ prepends a space for non-negative numbers.
        String body = (d == Math.floor(d) && !Double.isInfinite(d))
                ? String.valueOf((long) d)
                : String.valueOf(d);
        return d >= 0 ? " " + body : body;
    }

    private static double parseVal(String s) {
        String t = s.trim();
        int end = 0;
        while (end < t.length()) {
            char c = t.charAt(end);
            if (Character.isDigit(c) || c == '.' || c == '-' || c == '+'
                    || c == 'E' || c == 'e') end++;
            else break;
        }
        if (end == 0) return 0;
        try { return Double.parseDouble(t.substring(0, end)); }
        catch (NumberFormatException e) { return 0; }
    }
}
