package com.eliaslucky.mc_dos.client.apps.qbasic;

import java.util.ArrayDeque;
import java.util.Deque;

import com.eliaslucky.mc_dos.blocks.computer.basic.Host;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.apps.display.*;

public class QBasicHostImpl implements Host {
    private final TerminalApplication app;
    private int fg = 15;    // current foreground index (white)
    private int bg = 0;
    private int[] viewport = null;
    private final Deque<String> keyQueue = new ArrayDeque<>();
    private static final int MAX_KEY_QUEUE = 64;

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
            case 7  -> app.setDisplayMode(new Screen7EGA());
            case 13 -> app.setDisplayMode(new Screen13VGA());
            default -> { /* unsupported: keep current */ }
        }
    }

    @Override public void setPixel(int x, int y, int c) {
        if (viewport != null) {
            if (x < viewport[0] || x > viewport[2] || y < viewport[1] || y > viewport[3]) return;
        }
        app.getDisplayMode().setPixel(x, y, c);
    }
    @Override public void pset(int x, int y, int c)     { setPixel(x, y, c); }

    @Override public void drawLine(int x1, int y1, int x2, int y2, int c, int style) {
    	// Bresenham
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        var d = app.getDisplayMode();

        while (true) {
            d.setPixel(x1, y1, c);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 <  dx) { err += dx; y1 += sy; }
        }
    }
    @Override public void circle(int cx, int cy, int r, int c, boolean filled) {
    	if (r < 0) return;
        var d = app.getDisplayMode();

        // Midpoint circle
        int x = r, y = 0, err = 1 - r;
        while (x >= y) {
            if (filled) {
                for (int xx = cx - x; xx <= cx + x; xx++) {
                    d.setPixel(xx, cy + y, c);
                    d.setPixel(xx, cy - y, c);
                }
                for (int xx = cx - y; xx <= cx + y; xx++) {
                    d.setPixel(xx, cy + x, c);
                    d.setPixel(xx, cy - x, c);
                }
            } else {
                plot8(cx, cy, x, y, c);
            }
            y++;
            if (err < 0) err += 2 * y + 1;
            else { x--; err += 2 * (y - x) + 1; }
        }
    }
    private void plot8(int cx, int cy, int x, int y, int color) {
        var d = app.getDisplayMode();
        d.setPixel(cx + x, cy + y, color);
        d.setPixel(cx - x, cy + y, color);
        d.setPixel(cx + x, cy - y, color);
        d.setPixel(cx - x, cy - y, color);
        d.setPixel(cx + y, cy + x, color);
        d.setPixel(cx - y, cy + x, color);
        d.setPixel(cx + y, cy - x, color);
        d.setPixel(cx - y, cy - x, color);
    }
    @Override public int colorFg() { return fg; }
    @Override public void locate(int row, int col) {
        if (app.getDisplayMode() instanceof TextDisplayMode t) t.setCursor(row, col);
    }
    @Override public void color(int fg, int bg) {
    	this.fg = fg & 0xFF;
        this.bg = bg & 0xFF;
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
    @Override
    public void setViewport(int x1, int y1, int x2, int y2, int borderColor) {
        // Normalize (QBasic allows either corner first).
        int vx1 = Math.min(x1, x2), vy1 = Math.min(y1, y2);
        int vx2 = Math.max(x1, x2), vy2 = Math.max(y1, y2);
        this.viewport = new int[]{ vx1, vy1, vx2, vy2 };

        if (borderColor >= 0) {
            // Draw the border rectangle in the border color.
            for (int x = vx1; x <= vx2; x++) {
                app.getDisplayMode().setPixel(x, vy1, borderColor);
                app.getDisplayMode().setPixel(x, vy2, borderColor);
            }
            for (int y = vy1; y <= vy2; y++) {
                app.getDisplayMode().setPixel(vx1, y, borderColor);
                app.getDisplayMode().setPixel(vx2, y, borderColor);
            }
        }
    }
    @Override public boolean hasKey() { return !keyQueue.isEmpty(); }

    @Override public String pollKey() {
        return keyQueue.isEmpty() ? "" : keyQueue.pollFirst();
    }

    /** Called by the client when a key is typed while a program is running. */
    public void enqueueKey(String key) { 
    	if (keyQueue.size() >= MAX_KEY_QUEUE) keyQueue.pollFirst();
        keyQueue.addLast(key);
    }

    @Override
    public void resetViewport() {
        this.viewport = null;
    }
    
    public void backspaceChar() {
        if (!(app.getDisplayMode() instanceof TextDisplayMode t)) return;

        int col = t.getCursorCol() - 1;
        int row = t.getCursorRow();
        if (col < 0) {
            if (row > 0) { row--; col = t.cols - 1; }
            else return;
        }
        t.writeChar(row, col, ' ', t.getForeground(), t.getBackground());
        t.setCursor(row, col);
    }
}