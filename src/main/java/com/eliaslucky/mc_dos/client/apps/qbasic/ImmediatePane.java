package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ImmediatePane {
    private static final int VISIBLE_LINES = 2;

    private final List<String> transcript = new ArrayList<>();  // output of executed lines
    private final List<StringBuilder> buffer = new ArrayList<>();// user's typed lines

    private int cursorRow = 0;
    private int cursorCol = 0;

    public ImmediatePane() { buffer.add(new StringBuilder()); }

    public boolean isBlank() {
        return buffer.size() == 1 && buffer.get(0).length() == 0;
    }

    // Editing
    public void insert(char c) {
        buffer.get(cursorRow).insert(cursorCol, c);
        cursorCol++;
    }

    public void insertTab() {
        int pad = 4 - (cursorCol % 4);
        for (int i = 0; i < pad; i++) buffer.get(cursorRow).insert(cursorCol++, ' ');
    }

    public void backspace() {
        StringBuilder line = buffer.get(cursorRow);
        if (cursorCol > 0) { line.deleteCharAt(--cursorCol); }
        else if (cursorRow > 0) {
            StringBuilder prev = buffer.get(--cursorRow);
            cursorCol = prev.length();
            prev.append(line);
            buffer.remove(cursorRow + 1);
        }
    }

    public void moveLeft()  { if (cursorCol > 0) cursorCol--; }
    public void moveRight() { if (cursorCol < buffer.get(cursorRow).length()) cursorCol++; }
    public void moveUp()    { if (cursorRow > 0) { cursorRow--; cursorCol = Math.min(cursorCol, buffer.get(cursorRow).length()); } }
    public void moveDown()  { if (cursorRow < buffer.size() - 1) { cursorRow++; cursorCol = Math.min(cursorCol, buffer.get(cursorRow).length()); } }

    /** Enter: gather the buffer, join with newlines, clear, and return the source. */
    public String consume() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(buffer.get(i));
        }
        String source = sb.toString();
        buffer.clear();
        buffer.add(new StringBuilder());
        cursorRow = 0;
        cursorCol = 0;
        return source;
    }

    public void appendOutput(List<String> out) {
        transcript.addAll(out);
    }

    public void clear() {
        transcript.clear();
        buffer.clear();
        buffer.add(new StringBuilder());
        cursorRow = 0; cursorCol = 0;
    }

    // Rendering
    public void render(GuiGraphics g, QBasicApplication owner,
                       int xCell, int yCell, int widthCells, boolean focused) {

        int totalRows = transcript.size() + buffer.size();
        int startRow  = Math.max(0, totalRows - VISIBLE_LINES);

        for (int i = 0; i < VISIBLE_LINES; i++) {
            int abs = startRow + i;
            if (abs >= totalRows) break;

            String line;
            int color;
            if (abs < transcript.size()) {
                line = transcript.get(abs);
                color = DosPalette.LIGHT_GRAY;
            } else {
                line = buffer.get(abs - transcript.size()).toString();
                color = DosPalette.YELLOW;
            }

            if (line.length() > widthCells) line = line.substring(0, widthCells);
            owner.drawDos(g, line, xCell * 8, (yCell + i) * 16, color);
        }

        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int bufRowAbs = transcript.size() + cursorRow;
            if (bufRowAbs >= startRow && bufRowAbs < startRow + VISIBLE_LINES) {
                int screenRow = yCell + (bufRowAbs - startRow);
                int cx = xCell * 8 + cursorCol * 8;
                g.fill(cx, screenRow * 16 + 14, cx + 8, screenRow * 16 + 16, DosPalette.YELLOW);
            }
        }
    }
}