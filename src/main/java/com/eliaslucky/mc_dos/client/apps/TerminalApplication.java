package com.eliaslucky.mc_dos.client.apps;

import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.display.DisplayMode;
import com.eliaslucky.mc_dos.client.apps.display.Screen0Text;

import net.minecraft.client.gui.GuiGraphics;

public abstract class TerminalApplication {
    public static final int CELL_W = 8;
    public static final int CELL_H = 16;

    protected final ComputerTerminalScreen screen;
    protected int appWidth;
    protected int appHeight;

/** Every app has a current display surface. Default is 80×25 text. */
    protected DisplayMode displayMode = new Screen0Text();

    protected TerminalApplication(ComputerTerminalScreen screen) {
        this.screen = screen;
    }

    /** Called whenever Minecraft window size changes (and once at launch). */
    public final void setSize(int w, int h) {
        this.appWidth = w;
        this.appHeight = h;
        onResize();
    }

    public void setDisplayMode(DisplayMode mode) { this.displayMode = mode; }
    public DisplayMode getDisplayMode()          { return displayMode; }

    protected void onResize() {}
    public abstract void render(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);
    public boolean charTyped(char cp, int mods)                 { return false; }
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) { return false; }
    public boolean mouseClicked(double x, double y, int btn)    { return false; }
    public boolean mouseScrolled(double x, double y, double d)  { return false; }

    public void onClose() {}
    public abstract String getTitle();

    protected int cols() { return Math.max(1, appWidth  / CELL_W); }
    protected int rows() { return Math.max(1, appHeight / CELL_H); }
}
