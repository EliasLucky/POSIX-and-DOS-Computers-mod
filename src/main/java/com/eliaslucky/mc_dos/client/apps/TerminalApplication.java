package com.eliaslucky.mc_dos.client.apps;

import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.display.DisplayMode;
import com.eliaslucky.mc_dos.client.apps.display.Screen0Text;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A client-side TUI program. Subclass this to create a full-screen
 * application that runs inside a computer terminal.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>The server returns {@code "APP_LAUNCH:NAME:..."} from an
 *       executable runner.</li>
 *   <li>The client looks up {@code NAME} in
 *       {@link TerminalApplicationRegistry}.</li>
 *   <li>{@link TerminalApplicationRegistry.AppFactory#create}
 *       constructs an instance.</li>
 *   <li>{@link ComputerTerminalScreen#launchApp} hands over the screen.</li>
 *   <li>Render and input events are routed to the app until it closes.</li>
 * </ol>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class CalcApplication extends TerminalApplication {
 *     public CalcApplication(ComputerTerminalScreen screen,
 *                            String[] args, String content) {
 *         super(screen);
 *     }
 *     @Override public void render(GuiGraphics g, int mx, int my, float p) {
 *         // draw calculator
 *     }
 *     @Override public boolean keyPressed(int k, int s, int m) {
 *         // handle digit keys
 *         return true;
 *     }
 *     @Override public String getTitle() { return "Calculator"; }
 * }
 * }</pre>
 */
public abstract class TerminalApplication {
	/** Width of one character cell in pixels. */
    public static final int CELL_W = 8;
    /** Height of one character cell in pixels. */
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
