package com.eliaslucky.mc_dos.client.apps.msd;

import com.eliaslucky.mc_dos.client.ComputerTerminalScreen;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.tui.TuiBox;
import com.eliaslucky.mc_dos.client.tui.TuiDialog;
import com.eliaslucky.mc_dos.client.tui.TuiMenu;
import com.eliaslucky.mc_dos.client.tui.TuiScreen;
import com.eliaslucky.mc_dos.client.tui.TuiTheme;
import com.eliaslucky.mc_dos.client.tui.TuiPalette;
import com.eliaslucky.mc_dos.client.tui.TuiThemes;

import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Diagnostics 2.00, running as a client-side TUI application.
 *
 * <p>The application is pure presentation. It receives a sectioned
 * report from the server (via the {@code APP_LAUNCH:MSD::} payload)
 * and renders it as a grid of category buttons. Selecting a category
 * opens a detail window with that section's lines.
 *
 * @since 1.5
 */
public class MsdApplication extends TerminalApplication {
	// Layout constants

	private static final int BUTTON_WIDTH  = 14;
	private static final int BUTTON_HEIGHT = 3;
	private static final int BUTTON_PAD_X  = 2;
	private static final int BUTTON_PAD_Y  = 1;
	private static final int GRID_COLS	   = 4;
	private static final int GRID_TOP_ROW  = 3;

	// State

	/** Sections in the order the server sent them. */
	private final List<String> sectionOrder = new ArrayList<>();

	/** Lines of each section, keyed by section name. */
	private final Map<String, List<String>> sections = new LinkedHashMap<>();

	private int selectedIndex = 0;
	private String openSection = null;
	private boolean altHeld = false;

	private TuiMenu menuBar;
	private final TuiScreen overlay = new TuiScreen();
	// Construction

	/**
	 * @param screen  the hosting terminal screen
	 * @param content the sectioned report from the server
	 */
	public MsdApplication(ComputerTerminalScreen screen, String content) {
		super(screen);
		setTheme(TuiThemes.MSD);
		parseReport(content);

		this.menuBar = new TuiMenu(0, List.of(
				new TuiMenu.Menu("File", 'F', List.of(
						new TuiMenu.Item("Print Report...", 'P', "file.print",
								"Print the diagnostic report"),
						new TuiMenu.Item("Exit", 'X', "file.exit",
								"Exit Diagnostics")
				)),
				new TuiMenu.Menu("Utilities", 'U', List.of(
						new TuiMenu.Item("Memory Block Display", 'M', "util.memory",
								"Display memory blocks"),
						new TuiMenu.Item("Memory Browser", 'B', "util.browser",
								"Browse memory contents")
				)),
				new TuiMenu.Menu("Help", 'H', List.of(
						new TuiMenu.Item("Index", 'I', "help.index",
								"Display the Help index"),
						new TuiMenu.Item("About", 'A', "help.about",
								"About Diagnostics")
				))
		));
		this.menuBar.onAction(this::handleMenuAction);
	}

	/**
	 * Split the server's sectioned report into ordered names and
	 * per-section line lists.
	 *
	 * @param raw the report; may be {@code null}
	 */
	private void parseReport(String raw) {
		if (raw == null) return;

		String current = null;
		for (String line : raw.split("\n", -1)) {
			if (line.startsWith("=== ") && line.endsWith(" ===")) {
				current = line.substring(4, line.length() - 4);
				sectionOrder.add(current);
				sections.put(current, new ArrayList<>());
			} else if (current != null) {
				sections.get(current).add(line);
			}
		}
	}

	// Render

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		TuiTheme t = theme();
		g.fill(0, 0, appWidth, appHeight, t.screenBg());

		menuBar.render(g, this);

		// Title bar (row 1).
		String title = "Diagnostics  Version 2.00";
		g.fill(0, CELL_H, appWidth, CELL_H * 2, t.titleBg());
		drawDos(g, center(title, cols()), 0, CELL_H, t.titleFg());

		// Category buttons.
		renderButtonGrid(g);

		// Detail window on top of everything.
		if (openSection != null) {
			renderDetailWindow(g, openSection);
		}

		// Footer.
		renderFooter(g);
		overlay.render(g, this);
		menuBar.renderOverlay(g,this);
	}

	/**
	 * Draw the grid of category buttons. The number of buttons comes
	 * from how many sections the server sent.
	 *
	 * @param g the graphics context
	 */
	private void renderButtonGrid(GuiGraphics g) {
		for (int i = 0; i < sectionOrder.size(); i++) {
			int row = GRID_TOP_ROW + (i / GRID_COLS) * (BUTTON_HEIGHT + BUTTON_PAD_Y);
			int col = 2 + (i % GRID_COLS) * (BUTTON_WIDTH + BUTTON_PAD_X);
			String label = sectionOrder.get(i);

			boolean hot = (i == selectedIndex);
			boolean open = label.equals(openSection);

			renderButton(g, label, row, col, hot, open);
		}
	}

	/**
	 * Draw one category button.
	 *
	 * @param g		   the graphics context
	 * @param label    the section name to display
	 * @param row	   top row in cells
	 * @param col	   left column in cells
	 * @param selected whether the button is currently highlighted
	 * @param open	   whether the button's detail window is open
	 */
	private void renderButton(GuiGraphics g, String label, int row, int col, boolean selected, boolean open) {
		TuiTheme t = theme();
		int px = col * CELL_W;
		int py = row * CELL_H;
		int pw = BUTTON_WIDTH * CELL_W;
		int ph = BUTTON_HEIGHT * CELL_H;

		int bg,fg;
		if (selected) {
			bg = t.highlightBg();
			fg = t.highlightFg();
		}
		else if (open) {
			bg = t.frameBg();
			fg = t.titleFg();
		}
		else {
			bg = t.frameBg();
			fg = t.titleFg();
		}

		g.fill(px, py, px + pw, py + ph, bg);

		// Single-line box border.
		StringBuilder horiz = new StringBuilder();
		for (int i = 0; i < BUTTON_WIDTH - 2; i++) horiz.append('\u2500');
		String top    = "\u250C" + horiz + "\u2510";
		String bottom = "\u2514" + horiz + "\u2518";

		drawDos(g, top, px, py, fg);
		drawDos(g, bottom, px, py + ph - CELL_H, fg);
		for (int r = 1; r < BUTTON_HEIGHT - 1; r++) {
			drawDos(g, "\u2502", px, py + r * CELL_H, fg);
			drawDos(g, "\u2502", px + pw - CELL_W, py + r * CELL_H, fg);
		}

		// Center the label vertically and horizontally.
		String shown = label.length() > BUTTON_WIDTH - 2
				? label.substring(0, BUTTON_WIDTH - 2)
				: label;
		int labelRow = row + 1;
		int labelCol = col + (BUTTON_WIDTH - shown.length()) / 2;
		drawDos(g, shown, labelCol * CELL_W, labelRow * CELL_H, fg);
	}

	/**
	 * Draw the detail window for an opened section.
	 *
	 * @param g the graphics context
	 * @param section the section name to display
	 */
	private void renderDetailWindow(GuiGraphics g, String section) {
		TuiTheme t = theme();
		List<String> lines = sections.getOrDefault(section, List.of());

		int w = 60;
		int h = Math.min(lines.size() + 4, 20);
		int x = (cols() - w) / 2;
		int y = (rows() - h) / 2;

		// Shadow.
		g.fill((x + 1) * CELL_W, (y + 1) * CELL_H,
			   (x + w + 1) * CELL_W, (y + h + 1) * CELL_H,
			   TuiPalette.BLACK);

		// Double-line border.
		TuiBox box = new TuiBox(y, x, w, h, TuiBox.Style.DOUBLE).themed(t).titled(section);
		box.render(g, this);

		// Content lines, clipped to the window.
		int cy = y + 2;
		int maxLines = h - 4;	// top border + blank + content + OK button
		for (int i = 0; i < lines.size() && i < maxLines; i++, cy++) {
			String line = lines.get(i);
			if (line.length() > w - 4) line = line.substring(0, w - 4);
			drawDos(g, line, (x + 2) * CELL_W, cy * CELL_H, t.titleFg());
		}

		// OK button.
		int okRow = y + h - 2;
		int okCol = x + (w - 8) / 2;
		g.fill(okCol * CELL_W, okRow * CELL_H,
			   (okCol + 8) * CELL_W, (okRow + 1) * CELL_H,
			   t.highlightBg());
		drawDos(g, "  OK  ", (okCol + 1) * CELL_W, okRow * CELL_H, t.highlightFg());
	}

	/**
	 * Draw the footer hint line.
	 *
	 * @param g the graphics context
	 */
	private void renderFooter(GuiGraphics g) {
		TuiTheme t = theme();
		int y = (rows() - 1) * CELL_H;
		g.fill(0, y, appWidth, y + CELL_H, t.statusBg());

		String hints = openSection != null
				? " Enter=Close  Esc=Cancel "
				: " F1=Help  Alt=Menu  \u2191\u2193\u2190\u2192=Select  Enter=View  Esc=Exit ";
		drawDos(g, hints, 0, y, t.statusFg());
	}

	// Input

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (overlay.keyPressed(key, scan, mods)) return true;
		// Menu takes precedence when open.
		boolean isAlt = (key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT);
		if (isAlt) {
			if (!altHeld) {
				altHeld = true;
				if (menuBar.isOpen()) menuBar.close();
				else menuBar.open();
			}
			return true;
		}
		if (menuBar.isOpen()) {
			menuBar.keyPressed(key, scan, mods);
			return true;
		}

		// Detail window: Enter / Esc close it.
		if (openSection != null) {
			if (key == GLFW.GLFW_KEY_ESCAPE
				|| key == GLFW.GLFW_KEY_ENTER
				|| key == GLFW.GLFW_KEY_KP_ENTER) {
				openSection = null;
			}
			return true;
		}

		// Function keys.
		if (key == GLFW.GLFW_KEY_F1) {
			showAboutDialog();
			return true;
		}

		// Arrow keys move the selection across the grid.
		int row = selectedIndex / GRID_COLS;
		int col = selectedIndex % GRID_COLS;
		int rows = (sectionOrder.size() + GRID_COLS - 1) / GRID_COLS;

		switch (key) {
			case GLFW.GLFW_KEY_UP:	  row = Math.max(0, row - 1); break;
			case GLFW.GLFW_KEY_DOWN:  row = Math.min(rows - 1, row + 1); break;
			case GLFW.GLFW_KEY_LEFT:  col = Math.max(0, col - 1); break;
			case GLFW.GLFW_KEY_RIGHT: col = Math.min(GRID_COLS - 1, col + 1); break;
			case GLFW.GLFW_KEY_ENTER:
			case GLFW.GLFW_KEY_KP_ENTER:
				if (selectedIndex < sectionOrder.size()) {
					openSection = sectionOrder.get(selectedIndex);
				}
				return true;
			case GLFW.GLFW_KEY_ESCAPE:
				screen.returnToShell();
				return true;
			default:
				return false;
		}

		int newIndex = row * GRID_COLS + col;
		if (newIndex < sectionOrder.size()) {
			selectedIndex = newIndex;
		}
		return true;
	}

	@Override
	public boolean keyReleased(int key, int scan, int mods) {
		if (key == GLFW.GLFW_KEY_LEFT_ALT || key == GLFW.GLFW_KEY_RIGHT_ALT) {
			altHeld = false;
		}
		return super.keyReleased(key,scan,mods);
	}

	@Override
	public boolean charTyped(char cp, int mods) {
		// MSD takes no text input.
		return true;
	}

	// Menu actions

	/**
	 * Handle a menu action identifier from {@link TuiMenu}.
	 *
	 * @param action the action string, e.g. {@code "file.exit"}
	 */
	private void handleMenuAction(String action) {
		switch (action) {
			case "file.exit" -> screen.returnToShell();
			case "file.print" -> showDialog("Print Report",
					"Printer not ready.",
					"Check that a printer is connected.");
			case "util.memory" -> showDialog("Memory Block Display",
					"Memory map not available.",
					"Requires a memory driver.");
			case "util.browser" -> showDialog("Memory Browser",
					"Memory browser requires",
					"a protected-mode driver.");
			case "help.index" -> showDialog("MSD Help Index",
					"Categories:",
					"Computer, Memory, Video, Network,",
					"OS Version, Mouse, Other Adapters,",
					"Disk Drives, LPT Ports, COM Ports,",
					"IRQ Status, TSR Programs, Device Drivers.");
			case "help.about" -> showAboutDialog();
		}
	}

	/**
	 * Show the About dialog.
	 */
	private void showAboutDialog() {
		showDialog("About",
				"Diagnostics",
				"Version 2.00",
				"",
				"Copyright (C) 2026 Elias Lucky",
				"GNU General Public License version 3",
				"",
				"Simulated diagnostic tool.");
	}

	private void showDialog(String title, String... lines) {
		// MSD's real dialogs were simple message boxes; we use
		// TuiDialog but the caller manages stacking.
		TuiDialog dlg = new TuiDialog();
		dlg.addLine("");
		dlg.addLine(title);
		dlg.addLine("");
		for (String line : lines) dlg.addLine(line);
		dlg.addLine("");
		dlg.addItem("OK", "close");
 
		dlg.onAction(a -> { overlay.remove(dlg); overlay.setFocus(null); });
		dlg.onCancel(() -> { overlay.remove(dlg); overlay.setFocus(null); });

		overlay.add(dlg);
		overlay.setFocus(dlg);
	}

	// Helpers

	/**
	 * Center a string within a given width.
	 *
	 * @param text	the string
	 * @param width the target width in cells
	 * @return the padded string
	 */
	private static String center(String text, int width) {
		int pad = Math.max(0, (width - text.length()) / 2);
		return " ".repeat(pad) + text;
	}

	@Override
	public String getTitle() {
		return "MSD - Diagnostics";
	}
}
