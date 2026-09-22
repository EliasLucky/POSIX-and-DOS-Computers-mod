package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class RunOutputPane {
    private final List<String> lines = new ArrayList<>();
    private int scrollRow = 0;

    public void append(String line) {
        lines.add(line == null ? "" : line);
        int maxVisible = 23;
        if (lines.size() > maxVisible) scrollRow = lines.size() - maxVisible;
    }
    public void clear() { lines.clear(); scrollRow = 0; }

    public void render(GuiGraphics g, QBasicApplication owner) {
        int cols = owner.cols();
        int rows = owner.rows();
        int visible = rows - 1;
        int start = Math.max(0, lines.size() - visible);

        for (int i = 0; i < visible && start + i < lines.size(); i++) {
            String s = lines.get(start + i);
            if (s.length() > cols) s = s.substring(0, cols);
            owner.drawDos(g, s, 0, i * 16, DosPalette.WHITE);
        }
    }
}
