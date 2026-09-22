package com.eliaslucky.mc_dos.client.apps.editor;

import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEditorApplication extends TerminalApplication {

    // ── Text buffer ──────────────────────────────────────────────────────
    protected final List<StringBuilder> lines = new ArrayList<>();
    protected final String filePath;

    protected int cursorRow, cursorCol;
    protected int scrollRow, scrollCol;
    protected boolean modified;
    protected String statusMessage = "";

    // ── Popup dialog (null when none shown) ──────────────────────────────
    protected DialogState dialog;

    protected AbstractEditorApplication(ComputerTerminalScreen screen,
                                         String path, String initialContent) {
        super(screen);
        this.filePath = (path == null || path.isEmpty()) ? "UNTITLED" : path;
        if (initialContent != null && !initialContent.isEmpty()) {
            for (String l : initialContent.split("\n", -1)) lines.add(new StringBuilder(l));
        }
        if (lines.isEmpty()) lines.add(new StringBuilder());
    }

    // ── Chrome provided by subclasses ────────────────────────────────────
    protected abstract void renderMenuBar(GuiGraphics g);
    protected abstract String footerHints();

    // ── Layout ───────────────────────────────────────────────────────────
    protected int topRow()     { return 1; }                    // below menu bar
    protected int textRows()   { return rows() - 3; }           // minus menu + immediate + footer
    protected int textCols()   { return cols() - 1; }           // minus vscrollbar
    protected int immediateY() { return (rows() - 2) * CELL_H; } // row above footer

    @Override protected void onResize() { clampScrollToCursor(); }

    // ── Render entry point ───────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fill(0, 0, appWidth, appHeight, DosPalette.BLUE);

        renderMenuBar(g);
        renderEditorPane(g);
        renderSplitterAndImmediate(g);
        renderFooter(g);

        if (dialog != null) {
            dialog.render(g, cols(), rows(), this);
        }
    }

    protected void renderEditorPane(GuiGraphics g) {
        int baseY = topRow() * CELL_H;
        int rows  = textRows();
        int cols  = textCols();

        for (int r = 0; r < rows; r++) {
            int li = scrollRow + r;
            if (li >= lines.size()) break;
            String line = lines.get(li).toString();

            int from = Math.min(scrollCol, line.length());
            int to   = Math.min(line.length(), scrollCol + cols);
            String vis = line.substring(from, to);
            if (vis.isEmpty()) continue;
            drawDos(g, vis, 0, baseY + r * CELL_H, DosPalette.LIGHT_GRAY);
        }

        // Blinking cursor
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            int cr = cursorRow - scrollRow;
            int cc = cursorCol - scrollCol;
            if (cr >= 0 && cr < rows && cc >= 0 && cc < cols) {
                int cx = cc * CELL_W;
                int cy = baseY + cr * CELL_H;
                g.fill(cx, cy + CELL_H - 2, cx + CELL_W, cy + CELL_H, DosPalette.LIGHT_GRAY);
            }
        }
    }

    /** Draws the "─────── Immediate ───────" splitter above the footer. */
    protected void renderSplitterAndImmediate(GuiGraphics g) {
        int y = immediateY();
        int w = cols();
        // Fill the strip with blue
        g.fill(0, y, appWidth, y + CELL_H, DosPalette.BLUE);

        String label = " Immediate ";
        int labelStart = Math.max(0, (w - label.length()) / 2);
        // Draw box-drawing dashes to left and right of "Immediate"
        StringBuilder left  = new StringBuilder();
        StringBuilder right = new StringBuilder();
        for (int i = 0; i < labelStart; i++)              left.append('\u2500');
        for (int i = labelStart + label.length(); i < w; i++) right.append('\u2500');

        drawDos(g, left.toString(),  0, y, DosPalette.WHITE);
        drawDos(g, label,            labelStart * CELL_W, y, DosPalette.WHITE);
        drawDos(g, right.toString(), (labelStart + label.length()) * CELL_W, y, DosPalette.WHITE);
    }

    protected void renderFooter(GuiGraphics g) {
        int y = (rows() - 1) * CELL_H;
        g.fill(0, y, appWidth, y + CELL_H, DosPalette.LIGHT_GRAY);

        String left = footerHints();
        drawDos(g, left, 0, y, DosPalette.BLACK);

        String right = dialog != null ? "" :
                (statusMessage.isEmpty() ? (modified ? " *Modified" : "")
                                         : " " + statusMessage + " ");
        if (!right.isEmpty()) {
            int rightW = right.length() * CELL_W;
            drawDos(g, right, cols() * CELL_W - rightW, y, DosPalette.BLACK);
        }
    }

    // ── Input dispatch ───────────────────────────────────────────────────
    @Override
    public boolean charTyped(char cp, int mods) {
        if (dialog != null) return dialog.charTyped(cp);

        if (cp >= 32 && cp != 127) {
            lines.get(cursorRow).insert(cursorCol, cp);
            cursorCol++;
            modified = true;
            statusMessage = "";
            clampScrollToCursor();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (dialog != null) return dialog.keyPressed(key);
        if (handleFunctionKey(key)) return true;

        switch (key) {
            case GLFW.GLFW_KEY_UP:         moveCursor(-1,  0); return true;
            case GLFW.GLFW_KEY_DOWN:       moveCursor( 1,  0); return true;
            case GLFW.GLFW_KEY_LEFT:       moveCursor( 0, -1); return true;
            case GLFW.GLFW_KEY_RIGHT:      moveCursor( 0,  1); return true;
            case GLFW.GLFW_KEY_HOME:       cursorCol = 0; clampScrollToCursor(); return true;
            case GLFW.GLFW_KEY_END:        cursorCol = lines.get(cursorRow).length(); clampScrollToCursor(); return true;
            case GLFW.GLFW_KEY_PAGE_UP:    scrollRow = Math.max(0, scrollRow - textRows()); return true;
            case GLFW.GLFW_KEY_PAGE_DOWN:  scrollRow += textRows(); clampScrollToCursor(); return true;
            case GLFW.GLFW_KEY_BACKSPACE:  backspace(); return true;
            case GLFW.GLFW_KEY_DELETE:     deleteForward(); return true;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:   splitLine(); return true;
        }
        return false;
    }

    /** Subclass hook for F-keys etc. Return true if consumed. */
    protected boolean handleFunctionKey(int key) { return false; }

    // ── Editing primitives ───────────────────────────────────────────────
    protected void moveCursor(int dRow, int dCol) {
        if (dRow != 0) {
            cursorRow = Math.max(0, Math.min(lines.size() - 1, cursorRow + dRow));
            cursorCol = Math.min(cursorCol, lines.get(cursorRow).length());
        }
        if (dCol != 0) {
            int len = lines.get(cursorRow).length();
            cursorCol = Math.max(0, Math.min(len, cursorCol + dCol));
        }
        clampScrollToCursor();
    }

    protected void backspace() {
        StringBuilder cur = lines.get(cursorRow);
        if (cursorCol > 0) { cur.deleteCharAt(cursorCol - 1); cursorCol--; }
        else if (cursorRow > 0) {
            StringBuilder prev = lines.get(cursorRow - 1);
            int join = prev.length();
            prev.append(cur);
            lines.remove(cursorRow);
            cursorRow--;
            cursorCol = join;
        }
        modified = true; statusMessage = ""; clampScrollToCursor();
    }

    protected void deleteForward() {
        StringBuilder cur = lines.get(cursorRow);
        if (cursorCol < cur.length()) cur.deleteCharAt(cursorCol);
        else if (cursorRow < lines.size() - 1) {
            cur.append(lines.get(cursorRow + 1));
            lines.remove(cursorRow + 1);
        }
        modified = true; statusMessage = ""; clampScrollToCursor();
    }

    protected void splitLine() {
        StringBuilder cur = lines.get(cursorRow);
        String rest = cur.substring(cursorCol);
        cur.setLength(cursorCol);
        lines.add(cursorRow + 1, new StringBuilder(rest));
        cursorRow++; cursorCol = 0;
        modified = true; statusMessage = ""; clampScrollToCursor();
    }

    // ── File I/O ─────────────────────────────────────────────────────────
    protected void saveFile() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        screen.saveFile(filePath, sb.toString());
        modified = false;
        statusMessage = "Written to " + filePath;
    }

    protected String currentSource() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        return sb.toString();
    }

    // ── Scroll ───────────────────────────────────────────────────────────
    protected int maxLineLength() {
        int max = 0;
        for (StringBuilder sb : lines) max = Math.max(max, sb.length());
        return max;
    }

    protected void clampScrollToCursor() {
        int rows = textRows(), cols = textCols();

        if (cursorRow < scrollRow)              scrollRow = cursorRow;
        if (cursorRow >= scrollRow + rows)      scrollRow = cursorRow - rows + 1;
        if (cursorCol < scrollCol)              scrollCol = cursorCol;
        if (cursorCol >= scrollCol + cols)      scrollCol = cursorCol - cols + 1;

        scrollRow = Math.max(0, Math.min(scrollRow, Math.max(0, lines.size() - rows)));
        scrollCol = Math.max(0, Math.min(scrollCol, Math.max(0, maxLineLength() - cols)));
    }

    // ── Drawing helper — all subclasses should use this ──────────────────
    public void drawDos(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font,
                Component.literal(text).withStyle(screen.getDosStyle()),
                x, y, color, false);
    }

    public String getFilePath() { return filePath; }
    @Override public String getTitle() { return filePath; }
}
