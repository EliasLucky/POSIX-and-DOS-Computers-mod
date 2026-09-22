package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

public class QBasicHostImpl implements Host {

    private final TerminalApplication app;
    private int row = 0, col = 0;

    public QBasicHostImpl(TerminalApplication app) { this.app = app; }

    @Override
    public void print(String s) {
        var d = app.getDisplayMode();
        if (!(d instanceof Screen0Text text)) {
            // Falls back to System.out-style capture when not in text mode
            return;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n') { newline(text); continue; }
            if (c == '\r') continue;
            if (col >= text.cols) { newline(text); }
            text.write(row, col, c, 15, 1);  // white on blue
            col++;
        }
        text.setCursor(row, col);
    }

    private void newline(Screen0Text t) {
        col = 0;
        row++;
        if (row >= t.rows) { t.scrollUp(); row = t.rows - 1; }
    }

    @Override
    public void printNewline() { print("\n"); }

    @Override
    public void cls() {
        if (app.getDisplayMode() instanceof Screen0Text t) { t.clear(1); row = 0; col = 0; }
        else app.getDisplayMode().clear(0);
    }

    @Override
    public void setScreenMode(int mode) {
        switch (mode) {
            case 0  -> app.setDisplayMode(new Screen0Text());
            case 13 -> app.setDisplayMode(new Screen13VGA());
            // Add 1, 2, 7, 9, 12 as you implement them.
            default -> { /* unsupported mode: ignore, keep current */ }
        }
    }

    @Override
    public void setPixel(int x, int y, int color) {
        app.getDisplayMode().setPixel(x, y, color);
    }
    @Override public void pset(int x, int y, int color) { setPixel(x, y, color); }
    // ... drawLine/circle implemented in terms of setPixel for now ...

    // The rest (locate, color, beep, sleep) fill in over time.
}
