package com.eliaslucky.mc_dos.client.apps.editor;

import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DialogState {

    public record Item(String label, String action) {}

    private final List<String> staticLines = new ArrayList<>();
    private final List<Item>   items       = new ArrayList<>();
    private int  selected = 0;
    private Runnable onClosed;

    public DialogState addLine(String line)   { staticLines.add(line); return this; }
    public DialogState addItem(String label, String action) {
        items.add(new Item(label, action));
        return this;
    }
    public DialogState onClosed(Runnable r)   { this.onClosed = r; return this; }

    // ── The welcome dialog, matching QBASIC exactly ──────────────────────
    public static DialogState welcome() {
        return new DialogState()
            .addLine("")
            .addLine("Welcome to MS-DOS QBasic")
            .addLine("")
            .addLine("Copyright (C) Microsoft Corporation, 1987-1992.")
            .addLine("All rights reserved.")
            .addLine("")
            .addItem("Press Enter to see the Survival Guide", "help.survival")
            .addItem("Press ESC to clear this dialog box",    "close");
    }

    // ── Key handling ─────────────────────────────────────────────────────
    public boolean keyPressed(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_UP:    selected = Math.max(0, selected - 1); return true;
            case GLFW.GLFW_KEY_DOWN:  selected = Math.min(items.size() - 1, selected + 1); return true;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                onAction(items.isEmpty() ? "close" : items.get(selected).action());
                return true;
            case GLFW.GLFW_KEY_ESCAPE:
                onAction("close");
                return true;
        }
        return false;
    }

    public boolean charTyped(char cp) { return true; } // swallow

    private void onAction(String action) {
        // Subclass decides what to do; default behavior just closes.
        if (onClosed != null) onClosed.run();
    }

    // ── Render ───────────────────────────────────────────────────────────
    public void render(GuiGraphics g, int cols, int rows,
                       AbstractEditorApplication owner) {
        int innerW = 0;
        for (String s : staticLines) innerW = Math.max(innerW, s.length());
        for (Item it : items)         innerW = Math.max(innerW, it.label.length() + 4);

        int w = innerW + 4;
        int h = staticLines.size() + items.size() + 2;
        int x = Math.max(0, (cols - w) / 2);
        int y = Math.max(0, (rows - h) / 2);

        // Drop shadow
        g.fill((x + 1) * 8, (y + 1) * 16,
               (x + w + 1) * 8, (y + h + 1) * 16, DosPalette.BLACK);

        // Body
        g.fill(x * 8, y * 16, (x + w) * 8, (y + h) * 16, DosPalette.LIGHT_GRAY);

        // Border (single-line box drawing)
        StringBuilder top = new StringBuilder("\u250C");
        StringBuilder bot = new StringBuilder("\u2514");
        for (int i = 0; i < w - 2; i++) { top.append('\u2500'); bot.append('\u2500'); }
        top.append('\u2510'); bot.append('\u2518');

        owner.drawDos(g, top.toString(), x * 8, y * 16, DosPalette.BLACK);
        owner.drawDos(g, bot.toString(), x * 8, (y + h - 1) * 16, DosPalette.BLACK);
        for (int i = 1; i < h - 1; i++) {
            owner.drawDos(g, "\u2502", x * 8, (y + i) * 16, DosPalette.BLACK);
            owner.drawDos(g, "\u2502", (x + w - 1) * 8, (y + i) * 16, DosPalette.BLACK);
        }

        // Content
        int cy = y + 1;
        int centerX = x + w / 2;
        for (String s : staticLines) {
            int px = centerX * 8 - (s.length() * 8) / 2;
            owner.drawDos(g, s, px, cy * 16, DosPalette.BLACK);
            cy++;
        }

        for (int i = 0; i < items.size(); i++) {
            String label = items.get(i).label;
            String text = (i == selected)
                    ? "< " + label + " >"
                    : "  " + label + "  ";
            int px = centerX * 8 - (text.length() * 8) / 2;
            owner.drawDos(g, text, px, cy * 16, DosPalette.BLACK);
            cy++;
        }
    }
}
