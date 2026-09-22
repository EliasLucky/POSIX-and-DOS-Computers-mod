package com.eliaslucky.furniture.blocks.computer.apps;

import com.eliaslucky.furniture.client.ComputerTerminalScreen;
import com.eliaslucky.furniture.client.DosPalette;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class QBasicApplication extends TerminalApplication {
    // Character cell size — matches your ibm_vga_8x16 font.
    public static final int CHAR_W = 8;
    public static final int CHAR_H = 16;

    private final String filePath;
    private final List<StringBuilder> lines = new ArrayList<>();

    private int cursorRow, cursorCol;   // 0-based within the buffer
    private int scrollRow, scrollCol;   // top-left visible cell in the buffer
    private boolean modified;
    private String statusMessage = "";

    public QBasicApplication(ComputerTerminalScreen screen, String[] args, String initialContent) {
        super(screen);
        this.filePath = args.length > 0 && !args[0].isEmpty() ? args[0] : "UNTITLED.BAS";
        if (initialContent != null && !initialContent.isEmpty()) {
            // split(..., -1) keeps trailing blank lines
            for (String l : initialContent.split("\n", -1)) lines.add(new StringBuilder(l));
        }
        if (lines.isEmpty()) lines.add(new StringBuilder());
    }

    // ── Layout ────────────────────────────────────────────────────────────
    private int cols()      { return Math.max(1, appWidth  / CHAR_W); }
    private int rows()      { return Math.max(1, appHeight / CHAR_H); }
    private int textTop()   { return 1; }                 // row index
    private int textRows()  { return rows() - 3; }        // menu + text + footer + hscroll
    private int textCols()  { return cols() - 1; }        // 1 col for vscroll

    @Override protected void onResize() { clampScrollToCursor(); }

    // ── Render ────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Full-screen background
        g.fill(0, 0, appWidth, appHeight, DosPalette.BLUE);

        renderMenuBar(g);
        renderTextArea(g);
        renderVerticalScrollbar(g);
        renderHorizontalScrollbar(g);
        renderFooter(g);
    }

    private void renderMenuBar(GuiGraphics g) {
        int y = 0;
        g.fill(0, y, cols() * CHAR_W, y + CHAR_H, DosPalette.LIGHT_GRAY);
        // QBASIC's actual menu: File  Edit  View  Search  Run  Debug  Options  Help
        drawDos(g, " File   Edit   View   Search   Run   Debug   Options   Help ",
                0, y, DosPalette.BLACK);
    }

    private void renderTextArea(GuiGraphics g) {
        int rows = textRows();
        int cols = textCols();
        int baseY = textTop() * CHAR_H;

        for (int r = 0; r < rows; r++) {
            int lineIdx = scrollRow + r;
            if (lineIdx >= lines.size()) break;
            String line = lines.get(lineIdx).toString();

            int from = Math.min(scrollCol, line.length());
            int to   = Math.min(line.length(), scrollCol + cols);
            String visible = line.substring(from, to);

            drawDos(g, visible, 0, baseY + r * CHAR_H, DosPalette.WHITE);
        }

        // Cursor (blink)
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            int cr = cursorRow - scrollRow;
            int cc = cursorCol - scrollCol;
            if (cr >= 0 && cr < rows && cc >= 0 && cc < cols) {
                int cx = cc * CHAR_W;
                int cy = baseY + cr * CHAR_H;
                g.fill(cx, cy + CHAR_H - 2, cx + CHAR_W, cy + CHAR_H, DosPalette.WHITE);
            }
        }
    }

    private void renderVerticalScrollbar(GuiGraphics g) {
        int x  = (cols() - 1) * CHAR_W;
        int y  = textTop() * CHAR_H;
        int w  = CHAR_W;
        int h  = textRows() * CHAR_H;

        g.fill(x, y, x + w, y + h, DosPalette.LIGHT_GRAY);

        int total = lines.size();
        int view  = textRows();
        if (total <= view) return;

        int thumbH = Math.max(CHAR_H, (int) ((float) view / total * h));
        int thumbY = y + (int) ((float) scrollRow / total * h);
        g.fill(x, thumbY, x + w, thumbY + thumbH, DosPalette.DARK_GRAY);

        // Arrows (using DOS box drawing / arrows from your bitmap font)
        drawDos(g, "\u25B2", x, y - CHAR_H, DosPalette.BLACK);        // ▲
        drawDos(g, "\u25BC", x, y + h,     DosPalette.BLACK);         // ▼
    }

    private void renderHorizontalScrollbar(GuiGraphics g) {
        int x = 0;
        int y = (textTop() + textRows()) * CHAR_H;
        int w = textCols() * CHAR_W;
        int h = CHAR_H;

        g.fill(x, y, x + w, y + h, DosPalette.LIGHT_GRAY);

        int longest = maxLineLength();
        int view    = textCols();
        if (longest <= view) return;

        int thumbW = Math.max(CHAR_W, (int) ((float) view / longest * w));
        int thumbX = x + (int) ((float) scrollCol / longest * w);
        g.fill(thumbX, y, thumbX + thumbW, y + h, DosPalette.DARK_GRAY);
    }

    private void renderFooter(GuiGraphics g) {
        int y = (rows() - 1) * CHAR_H;
        g.fill(0, y, cols() * CHAR_W, y + CHAR_H, DosPalette.LIGHT_GRAY);

        // Left side: function key hints. Right side: dynamic message.
        String left  = " <F1=Help>  <F2=Save>  <F5=Run>  <F6=Window>  <F10=Menu> ";
        String right = statusMessage.isEmpty()
                ? (modified ? " *Modified" : "")
                : " " + statusMessage + " ";

        drawDos(g, left, 0, y, DosPalette.BLACK);

        int rightW = right.length() * CHAR_W;
        drawDos(g, right, cols() * CHAR_W - rightW, y, DosPalette.BLACK);
    }

    // ── Text helpers using the DOS font ───────────────────────────────────
    private void drawDos(GuiGraphics g, String text, int x, int y, int color) {
        g.drawString(Minecraft.getInstance().font,
                Component.literal(text).withStyle(screen.getDosStyle()),
                x, y, color, false);
    }

    // ── Input ─────────────────────────────────────────────────────────────
    @Override
    public boolean charTyped(char cp, int mods) {
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
        switch (key) {
            case GLFW.GLFW_KEY_UP:          moveCursor(-1,  0); return true;
            case GLFW.GLFW_KEY_DOWN:        moveCursor( 1,  0); return true;
            case GLFW.GLFW_KEY_LEFT:        moveCursor( 0, -1); return true;
            case GLFW.GLFW_KEY_RIGHT:       moveCursor( 0,  1); return true;
            case GLFW.GLFW_KEY_HOME:        cursorCol = 0; clampScrollToCursor(); return true;
            case GLFW.GLFW_KEY_END:         cursorCol = lines.get(cursorRow).length(); clampScrollToCursor(); return true;
            case GLFW.GLFW_KEY_PAGE_UP:     scrollRow -= textRows(); clampScrollToCursor(); return true;
            case GLFW.GLFW_KEY_PAGE_DOWN:   scrollRow += textRows(); clampScrollToCursor(); return true;
            case GLFW.GLFW_KEY_BACKSPACE:   backspace(); return true;
            case GLFW.GLFW_KEY_DELETE:      deleteForward(); return true;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:    splitLine(); return true;
            case GLFW.GLFW_KEY_F2:          saveFile(); return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        scrollRow -= (int) delta;
        clampScrollToCursor();
        return true;
    }

    // ── Editing primitives ────────────────────────────────────────────────
    private void moveCursor(int dRow, int dCol) {
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

    private void backspace() {
        StringBuilder cur = lines.get(cursorRow);
        if (cursorCol > 0) {
            cur.deleteCharAt(cursorCol - 1);
            cursorCol--;
        } else if (cursorRow > 0) {
            StringBuilder prev = lines.get(cursorRow - 1);
            int join = prev.length();
            prev.append(cur);
            lines.remove(cursorRow);
            cursorRow--;
            cursorCol = join;
        }
        modified = true;
        statusMessage = "";
        clampScrollToCursor();
    }

    private void deleteForward() {
        StringBuilder cur = lines.get(cursorRow);
        if (cursorCol < cur.length()) {
            cur.deleteCharAt(cursorCol);
        } else if (cursorRow < lines.size() - 1) {
            cur.append(lines.get(cursorRow + 1));
            lines.remove(cursorRow + 1);
        }
        modified = true;
        statusMessage = "";
        clampScrollToCursor();
    }

    private void splitLine() {
        StringBuilder cur = lines.get(cursorRow);
        String rest = cur.substring(cursorCol);
        cur.setLength(cursorCol);
        lines.add(cursorRow + 1, new StringBuilder(rest));
        cursorRow++;
        cursorCol = 0;
        modified = true;
        statusMessage = "";
        clampScrollToCursor();
    }

    private void saveFile() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines.get(i));
        }
        screen.saveFile(filePath, sb.toString());
        modified = false;
        statusMessage = "Written to " + filePath;
    }

    // ── Scroll math ──────────────────────────────────────────────────────
    private int maxLineLength() {
        int max = 0;
        for (StringBuilder sb : lines) max = Math.max(max, sb.length());
        return max;
    }

    private void clampScrollToCursor() {
        int rows = textRows(), cols = textCols();

        if (cursorRow < scrollRow)                 scrollRow = cursorRow;
        if (cursorRow >= scrollRow + rows)         scrollRow = cursorRow - rows + 1;

        if (cursorCol < scrollCol)                 scrollCol = cursorCol;
        if (cursorCol >= scrollCol + cols)         scrollCol = cursorCol - cols + 1;

        scrollRow = Math.max(0, Math.min(scrollRow, Math.max(0, lines.size() - rows)));
        scrollCol = Math.max(0, Math.min(scrollCol, Math.max(0, maxLineLength() - cols)));
    }

    @Override public String getTitle() { return "QBASIC - " + filePath; }
}
