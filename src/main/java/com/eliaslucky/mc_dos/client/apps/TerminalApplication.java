package com.eliaslucky.mc_dos.client.apps;

import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.display.DisplayMode;
import com.eliaslucky.mc_dos.client.apps.display.Screen0Text;
import com.eliaslucky.mc_dos.client.tui.TuiTheme;
import com.eliaslucky.mc_dos.client.tui.TuiThemes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * A client-side TUI program. Subclass this to create a full-screen
 * application that runs inside a computer terminal.
 *
 * <p>Lifecycle:
 * <ol>
 *	 <li>The server returns {@code "APP_LAUNCH:NAME:..."} from an
 *		 executable runner.</li>
 *	 <li>The client looks up {@code NAME} in
 *		 {@link TerminalApplicationRegistry}.</li>
 *	 <li>{@link TerminalApplicationRegistry.AppFactory#create}
 *		 constructs an instance.</li>
 *	 <li>{@link ComputerTerminalScreen#launchApp} hands over the screen.</li>
 *	 <li>Render and input events are routed to the app until it closes.</li>
 * </ol>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * public class CalcApplication extends TerminalApplication {
 *	   public CalcApplication(ComputerTerminalScreen screen, String[] args, String content) {
 *		   super(screen);
 *	   }
 *	   @Override public void render(GuiGraphics g, int mx, int my, float p) {
 *		   // draw calculator
 *	   }
 *	   @Override public boolean keyPressed(int k, int s, int m) {
 *		   // handle digit keys
 *		   return true;
 *	   }
 *	   @Override public String getTitle() { return "Calculator"; }
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
	/** The active theme. Widgets read their defaults from here. */
	protected TuiTheme theme = TuiThemes.QBASIC;

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
	public DisplayMode getDisplayMode()			 { return displayMode; }
	/**
	 * Draw a string on the character grid.
	 *
	 * <p>Each glyph is drawn at its own 8-pixel offset rather than
	 * letting the vanilla font advance by its own metric. This matters
	 * because every TUI widget — boxes, tables, menus, dialogs — places
	 * things at integer cell positions. If a single string is drawn in
	 * one call, spaces and punctuation shift subsequent glyphs by a
	 * fraction of a pixel each and by the right edge of a 60-column
	 * widget the misalignment is a full cell.
	 *
	 * <p>Spaces are skipped: they cost nothing to draw and their advance
	 * is implicit in the {@code x + i * CELL_W} math below.
	 *
	 * <p>Coordinates are in pixels. Widgets translate from cell
	 * coordinates using {@link #CELL_W} and {@link #CELL_H} before
	 * calling.
	 *
	 * @param g		the graphics context
	 * @param text	the string to draw; {@code null} or empty is a no-op
	 * @param x		left edge in pixels
	 * @param y		top edge in pixels
	 * @param color the ARGB color
	 */
	public void drawDos(GuiGraphics g, String text, int x, int y, int color) {
		if (text == null || text.isEmpty()) return;
		var mcFont = Minecraft.getInstance().font;
		var style  = screen.getDosStyle();

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == ' ') continue;
			g.drawString(mcFont,
				Component.literal(String.valueOf(c)).withStyle(style),
				x + i * CELL_W, y,
				color, false);
		}
	}

	protected void onResize() {}
	public abstract void render(GuiGraphics g, int mouseX, int mouseY, float partialTick);

	public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);
	public boolean charTyped(char cp, int mods)					{ return false; }
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) { return false; }
	public boolean mouseClicked(double x, double y, int btn)	{ return false; }
	public boolean mouseScrolled(double x, double y, double d)	{ return false; }
	public boolean mouseReleased(double x, double y, int btn) { return false; }

	public void onClose() {}
	public abstract String getTitle();

	public int cols() { return Math.max(1, appWidth  / CELL_W); }
	public int rows() { return Math.max(1, appHeight / CELL_H); }
	public TuiTheme theme() { return theme; }
	public void setTheme(TuiTheme t) { this.theme = t; }
}
