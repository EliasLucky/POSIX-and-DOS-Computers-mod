package com.eliaslucky.mc_dos.client.tui;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A framed rectangle with an optional title. Draws using the IBM VGA
 * box-drawing characters, so it looks like a DOS dialog from a 1980s
 * application.
 *
 * <p>Two border styles are supported:
 * <ul>
 *   <li><b>Single line</b> — {@code ┌─┐ │ └─┘}, the classic QBasic dialog</li>
 *   <li><b>Double line</b> — {@code ╔═╗ ║ ╚═╝}, the BIOS SETUP frame</li>
 * </ul>
 *
 * <p>A {@code TuiBox} is purely decorative; it does not clip child
 * widgets or intercept input. Widgets placed inside it draw on top
 * via the normal {@link TuiScreen} ordering.
 *
 * @since 1.5
 */
public class TuiBox implements TuiWidget {
    /** Box border style. */
    public enum Style { SINGLE, DOUBLE }

    private final int row, col, width, height;
    private final Style style;
    private String title;
    private int borderColor;
    private int bgColor;

    /**
     * @param row    top-left row in cells
     * @param col    top-left column in cells
     * @param width  width in cells, including borders
     * @param height height in cells, including borders
     * @param style  border style
     */
    public TuiBox(int row, int col, int width, int height, Style style) {
        this.row = row;
        this.col = col;
        this.width = width;
        this.height = height;
        this.style = style;
        this.title = null;
        this.borderColor = TuiPalette.BORDER;
        this.bgColor = TuiPalette.FRAME_BG;
    }

    /**
     * Set a title shown in the top border.
     *
     * @param title the title text; {@code null} for no title
     * @return this box, for chaining
     */
    public TuiBox titled(String title) { this.title = title; return this; }

    /**
     * Set the border colour.
     *
     * @param c a colour from {@link TuiPalette}
     * @return this box, for chaining
     */
    public TuiBox border(int c) { this.borderColor = c; return this; }

    /**
     * Set the interior fill colour. Pass {@code -1} to leave the
     * interior untouched (letting whatever was drawn below show
     * through).
     *
     * @param c a colour, or {@code -1} for no fill
     * @return this box, for chaining
     */
    public TuiBox fill(int c) { this.bgColor = c; return this; }

    @Override public int row()    { return row; }
    @Override public int col()    { return col; }
    @Override public int width()  { return width; }
    @Override public int height() { return height; }

    @Override
    public void render(GuiGraphics g, TerminalApplication app) {
        int px = col * TerminalApplication.CELL_W;
        int py = row * TerminalApplication.CELL_H;
        int pw = width * TerminalApplication.CELL_W;
        int ph = height * TerminalApplication.CELL_H;

        // Interior fill (below the border glyphs).
        if (bgColor >= 0) {
            g.fill(px + TerminalApplication.CELL_W,
                    py + TerminalApplication.CELL_H,
                    px + pw - TerminalApplication.CELL_W,
                    py + ph - TerminalApplication.CELL_H,
                    bgColor);
        }

        // Pick the box-drawing glyph set.
        char tl, tr, bl, br, hz, vt;
        if (style == Style.DOUBLE) {
            tl = '\u2554'; tr = '\u2557'; bl = '\u255A'; br = '\u255D';
            hz = '\u2550'; vt = '\u2551';
        } else {
            tl = '\u250C'; tr = '\u2510'; bl = '\u2514'; br = '\u2518';
            hz = '\u2500'; vt = '\u2502';
        }

        // Top and bottom borders.
        StringBuilder horiz = new StringBuilder();
        for (int i = 0; i < width - 2; i++) horiz.append(hz);
        String hLine = horiz.toString();
        String topLine = tl + hLine + tr;
        String bottomLine = bl + hLine + br;

        app.drawDos(g, topLine, px, py, borderColor);
        app.drawDos(g, bottomLine, px, py + ph - TerminalApplication.CELL_H, borderColor);

        // Vertical sides.
        for (int r = 1; r < height - 1; r++) {
            int y = py + r * TerminalApplication.CELL_H;
            app.drawDos(g, String.valueOf(vt), px, y, borderColor);
            app.drawDos(g, String.valueOf(vt),
                    px + pw - TerminalApplication.CELL_W, y, borderColor);
        }

        // Title, replacing the top border in place.
        if (title != null && !title.isEmpty()) {
            int titleLen = title.length() + 2; // " title "
            if (titleLen > width - 2) titleLen = width - 2;
            String display = " " + title.substring(0, titleLen - 2) + " ";
            int startCol = col + (width - titleLen) / 2;
            app.drawDos(g, display,
                    startCol * TerminalApplication.CELL_W,
                    py, borderColor);
        }
    }
}
