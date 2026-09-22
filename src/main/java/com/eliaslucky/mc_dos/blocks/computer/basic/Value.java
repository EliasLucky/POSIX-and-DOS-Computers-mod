package com.eliaslucky.mc_dos.blocks.computer.basic;

public final class Value {
    private final double number;
    private final String text;
    private final boolean isString;

    private Value(double n, String t, boolean str) { number = n; text = t; isString = str; }

    public static Value of(double d) { return new Value(d, null, false); }
    public static Value of(String s) { return new Value(0, s == null ? "" : s, true); }

    public boolean isString() { return isString; }
    public boolean isNumber() { return !isString; }

    public double asNumber() {
        if (isString) {
            try { return Double.parseDouble(text); } catch (NumberFormatException e) { return 0; }
        }
        return number;
    }

    public String asString() { return isString ? text : formatNum(number); }
    public String toPrintString() { return isString ? text : formatNum(number); }

    private static String formatNum(double d) {
        if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
            return Long.toString((long) d);
        }
        return Double.toString(d);
    }

    @Override public String toString() { return isString ? "\"" + text + "\"" : formatNum(number); }
}
