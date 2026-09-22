package com.eliaslucky.mc_dos.client.apps.display;

import net.minecraft.client.gui.GuiGraphics;

public abstract class DisplayMode {
    public final int id;              // 0, 1, 2, 7, 9, 12, 13
    public final int widthPx;         // framebuffer width (0 for text modes)
    public final int heightPx;
    public final int colors;          // 2, 4, 16, 256
    public final boolean textMode;    // SCREEN 0

    public abstract void setPixel(int x, int y, int color);
    public abstract int  getPixel(int x, int y);
    public abstract void clear(int color);
    public abstract void renderTo(GuiGraphics g, int screenX, int screenY, int maxW, int maxH);

    // Text modes only:
    public void putChar(int row, int col, char c, int fg, int bg) {}
    public void setCursor(int row, int col) {}
}

public final class Screen0Text  extends DisplayMode { /* 80x25 text */ }
public final class Screen13VGA  extends DisplayMode { /* 320x200x256 */ }
public final class Screen12VGA  extends DisplayMode { /* 640x480x16 */ }
// ... etc
