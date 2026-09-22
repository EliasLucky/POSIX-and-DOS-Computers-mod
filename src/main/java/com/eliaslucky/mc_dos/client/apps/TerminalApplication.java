package com.eliaslucky.furniture.blocks.computer.apps;

import com.eliaslucky.furniture.client.ComputerTerminalScreen;
import net.minecraft.client.gui.GuiGraphics;

public abstract class TerminalApplication {
    protected final ComputerTerminalScreen screen;
    protected int appWidth;
    protected int appHeight;

    protected TerminalApplication(ComputerTerminalScreen screen) {
        this.screen = screen;
    }

    /** Called whenever Minecraft window size changes (and once at launch). */
    public final void setSize(int w, int h) {
        this.appWidth = w;
        this.appHeight = h;
        onResize();
    }

    protected void onResize() {}
    public abstract void render(GuiGraphics g, int mouseX, int mouseY, float partialTick);

    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);
    public boolean charTyped(char cp, int mods)                 { return false; }
    public boolean mouseClicked(double x, double y, int btn)    { return false; }
    public boolean mouseScrolled(double x, double y, double d)  { return false; }

    public void onClose() {}
    public abstract String getTitle();
}
