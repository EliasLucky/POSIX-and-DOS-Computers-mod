package com.eliaslucky.mc_dos.blocks.computer.basic;

/** The interpreter's only view of the outside world. */
public interface Host {
    void print(String s);              // text without newline; may contain '\n'
    void printNewline();
    void cls();
    void setScreenMode(int mode);
    void setPixel(int x, int y, int color);
    void pset(int x, int y, int color);
    void drawLine(int x1, int y1, int x2, int y2, int color, int style);
    void circle(int cx, int cy, int r, int color, boolean filled);
    int colorFg();
    void locate(int row, int col);
    void color(int fg, int bg);
    void beep();
    void sleep(int millis);
    void end();
    void runtimeError(int code, String message, int sourceLine);
    /** @return true if at least one key is queued. */
    boolean hasKey();
    /** @return the next key, or "" if the queue is empty. */
    String pollKey();
    /** Set a clipping rectangle. Subsequent draws are confined to it. */
    void setViewport(int x1, int y1, int x2, int y2, int borderColor);
    /** Remove the current viewport — draws go full-screen again. */
    void resetViewport();
}
