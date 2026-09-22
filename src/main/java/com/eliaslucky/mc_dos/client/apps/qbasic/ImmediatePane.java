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
                       int xCell, int yCell, int widthCells) {
        int shown = Math.min(history.size(), 1); // just the last line, like DOS
        int start = history.size() - shown;
        for (int i = 0; i < shown; i++) {
            owner.drawDos(g, history.get(start + i),xCell * 8, (yCell + i) * 16, DosPalette.LIGHT_GRAY);
        }
        // Draw the input line
        owner.drawDos(g, input.toString(),xCell * 8, (yCell + shown) * 16, DosPalette.YELLOW);
    }
}
