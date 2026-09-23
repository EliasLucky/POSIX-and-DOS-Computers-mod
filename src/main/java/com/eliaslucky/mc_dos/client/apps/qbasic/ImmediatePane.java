package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class ImmediatePane {
    private final List<String> history = new ArrayList<>();
    private final StringBuilder input = new StringBuilder();

    public void type(char c) { input.append(c); }
    public void backspace() { if (input.length() > 0) input.deleteCharAt(input.length() - 1); }
    public String consume() {
        String s = input.toString();
        history.add(s);
        input.setLength(0);
        return s;
    }
    public void appendOutput(List<String> out) { history.addAll(out); }
    public void clear()               { history.clear(); input.setLength(0); }

    public void render(GuiGraphics g, QBasicApplication owner,
                       int xCell, int yCell, int widthCells, boolean focused) {
        /*int shown = Math.min(history.size(), 1); // just the last line, like DOS
        int start = history.size() - shown;
        for (int i = 0; i < shown; i++) {
            owner.drawDos(g, history.get(start + i),xCell * 8, (yCell + i) * 16, DosPalette.LIGHT_GRAY);
        }
        // Draw the input line
        owner.drawDos(g, input.toString(),xCell * 8, (yCell + shown) * 16, DosPalette.YELLOW);*/
    	// Show the last history line, then the input line, side by side at yCell.
        String lastLine = history.isEmpty() ? "" : history.get(history.size() - 1);
        // Left half: last output
        if (!lastLine.isEmpty()) {
            String s = lastLine.length() > widthCells / 2 ? lastLine.substring(0, widthCells / 2) : lastLine;
            owner.drawDos(g, s, xCell * 8, yCell * 16, DosPalette.LIGHT_GRAY);
        }
        // Right half: current input, yellow
        int inputX = xCell * 8 + (widthCells / 2) * 8;
        String shown = input.toString();
        if (shown.length() > widthCells / 2) shown = shown.substring(shown.length() - widthCells / 2);
        owner.drawDos(g, shown, inputX, yCell * 16, DosPalette.YELLOW);
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = inputX + shown.length() * 8;
            g.fill(cursorX, yCell * 16 + 14, cursorX + 8, yCell * 16 + 16, DosPalette.YELLOW);
        }
    }
    
    public void insertTab() {
        int stop = 4;
        int pad  = stop - (input.length() % stop);
        for (int i = 0; i < pad; i++) input.append(' ');
    }
}
