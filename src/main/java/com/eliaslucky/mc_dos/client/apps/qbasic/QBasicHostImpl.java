package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.blocks.computer.basic.Host;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.apps.display.*;

public class QBasicHostImpl implements Host {

    private final TerminalApplication app;

    public QBasicHostImpl(TerminalApplication app) { this.app = app; }

    @Override
    public void print(String s) {
        if (!(app.getDisplayMode() instanceof TextDisplayMode t)) return;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r') continue;
            if (c == '\n') {
                int row = t.getCursorRow() + 1, col = 0;
                if (row >= t.rows) { t.scrollUp(1); row = t.rows - 1; }
                t.setCursor(row, col);
            } else {
                int row = t.getCursorRow(), col = t.getCursorCol();
                t.writeChar(row, col, c, t.getForeground(), t.getBackground());
                col++;
                if (col >= t.cols) {
                    col = 0; row++;
                    if (row >= t.rows) { t.scrollUp(1); row = t.rows - 1; }
                }
                t.setCursor(row, col);
            }
        }
    }

    @Override public void printNewline() { print("\n"); }
    @Override public void cls()          { app.getDisplayMode().clear(1); }

    @Override
    public void setScreenMode(int mode) {
        switch (mode) {
            case 0  -> app.setDisplayMode(new Screen0Text());
            case 13 -> app.setDisplayMode(new Screen13VGA());
            default -> { /* unsupported: keep current */ }
        }
    }

    @Override public void setPixel(int x, int y, int c) { app.getDisplayMode().setPixel(x, y, c); }
    @Override public void pset(int x, int y, int c)     { setPixel(x, y, c); }

    @Override public void drawLine(int x1, int y1, int x2, int y2, int c, int style) {
        // TODO: Bresenham
    }
    @Override public void circle(int cx, int cy, int r, int c, boolean filled) {
        // TODO: midpoint circle
    }
    @Override public void locate(int row, int col) {
        if (app.getDisplayMode() instanceof TextDisplayMode t) t.setCursor(row, col);
    }
    @Override public void color(int fg, int bg) {
        if (app.getDisplayMode() instanceof TextDisplayMode t) t.setAttribute(fg, bg);
    }
    @Override public void beep()        { /* no-op */ }
    @Override public void sleep(int ms) { /* no-op for now — blocking freezes the tick */ }
    @Override public void end()         { /* optional: clear a "running" flag */ }

    @Override
    public void runtimeError(int code, String msg, int line) {
        printNewline();
        print("Runtime error " + code + " at line " + line + ": " + msg);
    }
}