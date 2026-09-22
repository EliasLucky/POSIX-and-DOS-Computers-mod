package com.eliaslucky.mc_dos.client.apps.display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * SCREEN 0 — 80×25 text mode with 16 fg/bg attributes per cell.
 * The DOS prompt, QBASIC's editor chrome, and EDIT.COM all run here.
 */
public final class Screen0Text extends TextDisplayMode {

    private final char[] glyphs;
    private final int[]  attrs;

    private int cursorRow = 0;
    private int cursorCol = 0;

    private int currentFg = 7;   // light gray
    private int currentBg = 1;   // blue

    public Screen0Text() {
        super(0, 80, 25, 16);
        this.glyphs = new char[cols * rows];
        this.attrs  = new int[cols * rows];
        clear(1);
    }

    @Override
    public void clear(int bgIndex) {
        int bg = bgIndex & 0xF;
        int attr = (7 << 4) | bg;
        for (int i = 0; i < glyphs.length; i++) {
            glyphs[i] = ' ';
            attrs[i]  = attr;
        }
        cursorRow = 0;
        cursorCol = 0;
    }

    @Override
    public void writeChar(int row, int col, char c, int fg, int bg) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return;
        int i = row * cols + col;
        glyphs[i] = c;
        attrs[i]  = ((fg & 0xF) << 4) | (bg & 0xF);
    }

    /** write at the cursor and advance it, wrapping. */
    public void writeAtCursor(char c) {
        if (c == '\n') { cursorCol = 0; cursorRow++; }
        else if (c == '\r') { cursorCol = 0; }
        else {
            writeChar(cursorRow, cursorCol, c, currentFg, currentBg);
            cursorCol++;
            if (cursorCol >= cols) { cursorCol = 0; cursorRow++; }
        }
        if (cursorRow >= rows) { scrollUp(1); cursorRow = rows - 1; }
    }

    @Override public char readChar(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return ' ';
        return glyphs[row * cols + col];
    }

    @Override public int readAttr(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return (7 << 4) | 1;
        return attrs[row * cols + col];
    }

    @Override public void setCursor(int row, int col) { cursorRow = row; cursorCol = col; }
    @Override public int  getCursorRow() { return cursorRow; }
    @Override public int  getCursorCol() { return cursorCol; }

    @Override
    public void scrollUp(int lines) {
        if (lines <= 0) return;
        if (lines >= rows) { clear(currentBg); return; }

        int keep  = cols * (rows - lines);
        System.arraycopy(glyphs, cols * lines, glyphs, 0, keep);
        System.arraycopy(attrs,  cols * lines, attrs,  0, keep);

        int blank = (7 << 4) | currentBg;
        for (int i = keep; i < glyphs.length; i++) {
            glyphs[i] = ' ';
            attrs[i]  = blank;
        }
    }

    @Override public void setAttribute(int fg, int bg) {
        currentFg = fg & 0xF;
        currentBg = bg & 0xF;
    }
    @Override public int getForeground() { return currentFg; }
    @Override public int getBackground() { return currentBg; }

    @Override
    public void render(GuiGraphics g, int screenX, int screenY, int maxW, int maxH) {
        Minecraft mc = Minecraft.getInstance();
        Style dosStyle = Style.EMPTY.withFont(ResourceLocation.fromNamespaceAndPath(
                com.eliaslucky.mc_dos.Computers.MODID, "ibm_vga_8x16"));

        for (int r = 0; r < rows; r++) {
            int c = 0;
            while (c < cols) {
                int a = attrs[r * cols + c];
                int end = c;
                while (end < cols && attrs[r * cols + end] == a) end++;

                StringBuilder sb = new StringBuilder(end - c);
                for (int i = c; i < end; i++) sb.append(glyphs[r * cols + i]);

                int fgIdx = (a >> 4) & 0xF;
                int bgIdx = a & 0xF;

                int px = screenX + c * 8;
                int py = screenY + r * 16;

                g.fill(px, py, px + (end - c) * 8, py + 16, DosPalette.EGA[bgIdx]);
                g.drawString(mc.font,
                        Component.literal(sb.toString()).withStyle(dosStyle),
                        px, py, DosPalette.EGA[fgIdx], false);

                c = end;
            }
        }
    }
}
