package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.blocks.computer.basic.Host;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Host} implementation that captures text output into a list
 * of lines instead of painting to a display surface.
 *
 * <p>Two uses:
 * <ul>
 *   <li><b>The Immediate pane.</b> When the user types a statement and
 *       presses Enter, the program runs against a {@code HeadlessHost}
 *       so its output appears inline in the pane rather than taking
 *       over the screen the way F5 would.</li>
 *   <li><b>Unit tests.</b> A headless host lets an interpreter test
 *       assert on printed output without spinning up a Minecraft
 *       client or a display mode.</li>
 * </ul>
 *
 * <h2>What it does</h2>
 * <ul>
 *   <li>{@link #print(String)} accumulates characters into a running
 *       buffer; on newline, the buffer is committed to {@link #getOutput()}.</li>
 *   <li>{@link #cls()} clears both the committed lines and the buffer.</li>
 *   <li>{@link #runtimeError(int, String, int)} records the error code
 *       and message but does not print them, so callers can inspect the
 *       failure programmatically via {@link #hadError()} and
 *       {@link #getLastErrorCode()}.</li>
 * </ul>
 *
 * <h2>What ignores</h2>
 * Everything graphics-related is a no-op: {@code setPixel},
 * {@code drawLine}, {@code circle}, {@code setScreenMode},
 * {@code setViewport}, {@code color}, {@code locate}.
 *
 * {@link #hasKey()} always returns {@code false}. The immediate pane
 * does not feed keystrokes into a running program, so a program that
 * polls {@code INKEY$} in a loop would spin forever if it ran through
 * a headless host. Callers are expected to guard against that - the
 * immediate pane's own interpreter loop has a step limit for exactly
 * this reason.
 */
public class HeadlessHost implements Host {
    private final List<String> lines = new ArrayList<>();
    private final StringBuilder current = new StringBuilder();
    
    private int lastErrorCode = -1;
    private String lastErrorMessage = null;
    
    // The current default attribute. Tracked so that COLOR statements
    // in a headless program don't fail, even though we don't render them.
    private int fg = 15;
    private int bg = 0;

    public List<String> getOutput() { return lines; }
    public boolean hadError() { return lastErrorCode >= 0; }
    public int getLastErrorCode() { return lastErrorCode; }
    public String getLastErrorMessage() { return lastErrorMessage; }

    @Override
    public void print(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') { lines.add(current.toString()); current.setLength(0); }
            else if (c != '\r') current.append(c);
        }
    }

    @Override public void printNewline() { print("\n"); }
    @Override public void cls()          { lines.clear(); current.setLength(0); }

    @Override public void setScreenMode(int mode)          { /* no-op */ }
    @Override public void setPixel(int x, int y, int c)    { /* no-op */ }
    @Override public void pset(int x, int y, int c)        { /* no-op */ }
    @Override public void drawLine(int x1, int y1, int x2, int y2, int c, int s) { /* no-op */ }
    @Override public void circle(int cx, int cy, int r, int c, boolean filled)   { /* no-op */ }
    @Override public void locate(int row, int col)         { /* no-op */ }
    @Override public void color(int f, int b) {
        this.fg = f & 0xFF;
        this.bg = b & 0xFF;
    }
    @Override public int colorFg() { return fg; }
    @Override public void setViewport(int x1, int y1, int x2, int y2, int b) { /* no-op */ }
    @Override public void resetViewport()                                  { /* no-op */ }
    @Override public void beep()                           { /* no-op */ }
    @Override public void sleep(int ms)                    { /* no-op */ }

    @Override
    public void end() {
        if (current.length() > 0) {
            lines.add(current.toString());
            current.setLength(0);
        }
    }

    @Override
    public void runtimeError(int code, String message, int line) {
    	this.lastErrorCode = code;
        this.lastErrorMessage = (message == null || message.isEmpty())
                ? "Invalid syntax"
                : message;
    }
    
    /**
     * Always returns {@code false}. The immediate pane does not deliver
     * keystrokes to a running program, so {@code INKEY$} inside an
     * immediate-mode statement yields the empty string and the
     * interpreter's step limit protects against infinite loops.
     *
     * @return {@code false}
     */
    @Override
    public boolean hasKey() { return false; }

    /**
     * Always returns {@code ""}. See {@link #hasKey()} for rationale.
     *
     * @return the empty string
     */
    @Override
    public String pollKey() { return ""; }
}
