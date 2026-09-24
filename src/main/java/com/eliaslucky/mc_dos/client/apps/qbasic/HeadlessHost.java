package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.blocks.computer.basic.Host;
import java.util.ArrayList;
import java.util.List;

/**
 * A Host that collects output into a list of strings instead of painting
 * to a display surface. Used by the Immediate pane, and useful for tests.
 */
public class HeadlessHost implements Host {
    private final List<String> lines = new ArrayList<>();
    private final StringBuilder current = new StringBuilder();
    
    private int lastErrorCode = -1;
    private String lastErrorMessage = null;
    
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
}
