package com.eliaslucky.mc_dos.client.apps.display;

/**
 * Base class for anything that's a character-cell grid rather than a
 * pixel framebuffer. SCREEN 0 in DOS, VT100/ANSI terminals in Linux,
 * the BIOS text console — all of these share the same operations.
 *
 * Coordinates are (row, col), zero-based, top-left origin.
 * Colors are palette indices, not RGB — the palette is per-mode.
 */
public abstract class TextDisplayMode extends DisplayMode {

    public final int cols;
    public final int rows;

    protected TextDisplayMode(int id, int cols, int rows, int colors) {
        super(id, cols * 8, rows * 16, colors, true);
        this.cols = cols;
        this.rows = rows;
    }

    /** Write a glyph at (row, col) with the given fg/bg palette indices. */
    public abstract void writeChar(int row, int col, char c, int fg, int bg);
    /** Read the glyph at (row, col); returns ' ' if out of bounds. */
    public abstract char readChar(int row, int col);
    /** Read the packed attribute (fg<<4|bg) at (row, col). */
    public abstract int readAttr(int row, int col);

    // Cursor
    public abstract void setCursor(int row, int col);
    public abstract int  getCursorRow();
    public abstract int  getCursorCol();

    // Scrolling
    /** Scroll the whole screen up by `lines` rows; blank lines fill the bottom. */
    public abstract void scrollUp(int lines);

    /** Convenience: scroll up by one row. */
    public final void scrollUp() { scrollUp(1); }

    /** Set the default fg/bg applied to subsequent writeChar calls that don't specify. */
    public abstract void setAttribute(int fg, int bg);
    public abstract int  getForeground();
    public abstract int  getBackground();

    @Override public final void setPixel(int x, int y, int color) {}
    @Override public final int  getPixel(int x, int y) { return 0; }
}