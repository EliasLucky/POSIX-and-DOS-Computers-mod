package com.eliaslucky.mc_dos.client.apps.display;

import net.minecraft.client.gui.GuiGraphics;

public abstract class DisplayMode {
	public final int id; // 0, 1, 2, 7, 9, 12, 13
	public final int widthPx; // framebuffer width (0 for text modes)
	public final int heightPx;
	public final int colors; // 2, 4, 16, 256
	public final boolean textMode; // SCREEN 0

	protected DisplayMode(int id, int widthPx, int heightPx, int colors, boolean textMode) {
        this.id = id;
        this.widthPx = widthPx;
        this.heightPx = heightPx;
        this.colors = colors;
        this.textMode = textMode;
    }
	
	public abstract void setPixel(int x, int y, int color);
	public abstract int getPixel(int x, int y);
	public abstract void clear(int color);

	public abstract void render(GuiGraphics g, int screenX, int screenY, int maxW, int maxH);
}