package com.eliaslucky.mc_dos.client.tui;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import net.minecraft.client.gui.GuiGraphics;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A menu bar with dropdown menus, styled after QBasic's top bar.
 *
 * <p>The menu bar is one row tall and spans a horizontal range of
 * columns. Each top-level menu has a mnemonic - a single character
 * that opens it via Alt+letter. When open, the dropdown lists the
 * menu's items; arrow keys navigate, Enter activates, Escape closes.
 *
 * {@code TuiMenu} the menus and their items are passed
 * in as a list of records, so any TUI application can supply its own.
 *
 * @since 1.5
 */
public class TuiMenu implements TuiWidget {
	/**
	 * One entry in a dropdown.
	 *
	 * @param label		 display text
	 * @param mnemonic	 activation character (unique within the menu)
	 * @param action	 identifier passed to the action handler
	 * @param footerHelp text shown in the footer while highlighted
	 */
	public record Item(String label, char mnemonic, String action, String footerHelp) {}

	/**
	 * A top-level menu.
	 *
	 * @param label    display text, e.g. {@code "File"}
	 * @param mnemonic Alt-key character that opens this menu
	 * @param items    dropdown items
	 */
	public record Menu(String label, char mnemonic, List<Item> items) {}

	private final int row;
	private final List<Menu> menus;
	private boolean active = false;
	private int selectedMenu = -1;
	private int selectedItem = -1;
	private Consumer<String> onAction;
	
	/**
	 * @param row	the row the menu bar occupies (usually 0)
	 * @param menus the menu tree
	 */
	public TuiMenu(int row, List<Menu> menus) {
		this.row = row;
		this.menus = new ArrayList<>(menus);
	}

	/**
	 * Register a callback fired when a menu item is activated.
	 *
	 * @param c the callback, receiving the item's action string
	 */
	public void onAction(Consumer<String> c) { this.onAction = c; }

	/** @return {@code true} if a dropdown is currently open. */
	public boolean isOpen() { return active; }

	/** Open the first menu. */
	public void open() { active = true; selectedMenu = 0; selectedItem = -1; }

	/**
	 * Open a specific menu by its mnemonic.
	 *
	 * @param m the mnemonic character
	 * @return {@code true} if a menu was opened
	 */
	public boolean openByMnemonic(char m) {
		for (int i = 0; i < menus.size(); i++) {
			if (Character.toUpperCase(menus.get(i).mnemonic())
					== Character.toUpperCase(m)) {
				active = true;
				selectedMenu = i;
				selectedItem = -1;
				return true;
			}
		}
		return false;
	}

	/** Close the dropdown, if any. */
	public void close() { active = false; selectedMenu = -1; selectedItem = -1; }

	/**
	 * @return help text for the footer, based on the highlighted item
	 */
	public String footerHelp() {
		if (!active || selectedMenu < 0) return "";
		if (selectedItem < 0) return menus.get(selectedMenu).label() + ": use UP/DOWN then ENTER";
		return menus.get(selectedMenu).items().get(selectedItem).footerHelp();
	}

	@Override public int row()	  { return row; }
	@Override public int col()	  { return 0; }
	@Override public int width()  { return 80; }
	@Override public int height() { return active ? 12 : 1; }

	@Override
	public void render(GuiGraphics g, TerminalApplication app) {
		TuiTheme t = app.theme();
		int y = row * TerminalApplication.CELL_H;

		// Menu bar strip
		g.fill(0, y, width() * TerminalApplication.CELL_W,
				y + TerminalApplication.CELL_H, t.titleBg());

		int col = 1;
		for (int i = 0; i < menus.size(); i++) {
			Menu m = menus.get(i);
			boolean hot = active && i == selectedMenu;
			int px = col * TerminalApplication.CELL_W;

			if (hot) {
				g.fill(px - TerminalApplication.CELL_W, y,
						px + (m.label().length() + 1) * TerminalApplication.CELL_W,
						y + TerminalApplication.CELL_H,
						t.highlightBg());
			}
			drawLabelWithMnemonic(g,app, m.label(),m.mnemonic(), px, y,
					hot ? t.highlightFg() : t.titleFg(),
					hot ? t.highlightMn() : t.titleFg());
			col += m.label().length() + 3;
		}
	}
	/**
	 * Draw the open dropdown, if any. Call <em>after</em> every other
	 * render method on the screen so the dropdown sits above the header,
	 * editor, and footer.
	 */
	public void renderOverlay(GuiGraphics g, TerminalApplication app) {
		if (!active || selectedMenu < 0) return;

		TuiTheme t = app.theme();
		Menu m = menus.get(selectedMenu);
		int maxLen = m.label().length();
		for (Item it : m.items()) maxLen = Math.max(maxLen, it.label().length());
		int w = maxLen + 4;
		int h = m.items().size() + 2;

		int dx = 1;
		for (int i = 0; i < selectedMenu; i++) {
			dx += menus.get(i).label().length() + 3;
		}
		int dy = row + 1;

		// Shadow
		g.fill((dx + 1) * TerminalApplication.CELL_W,
		       (dy + 1) * TerminalApplication.CELL_H,
		       (dx + w + 1) * TerminalApplication.CELL_W,
		       (dy + h + 1) * TerminalApplication.CELL_H,
		       TuiPalette.BLACK);

		// Body
		g.fill(dx * TerminalApplication.CELL_W,
		       dy * TerminalApplication.CELL_H,
		       (dx + w) * TerminalApplication.CELL_W,
		       (dy + h) * TerminalApplication.CELL_H,
		       t.frameBg());

		for (int i = 0; i < m.items().size(); i++) {
			Item it = m.items().get(i);
			int iy = (dy + 1 + i) * TerminalApplication.CELL_H;
			boolean hot = i == selectedItem;
			int fg = hot ? t.highlightFg() : t.titleFg();
			if (hot) {
				g.fill(dx * TerminalApplication.CELL_W, iy,
					   (dx + w) * TerminalApplication.CELL_W,
					   iy + TerminalApplication.CELL_H,
					   t.highlightBg());
			}
			drawLabelWithMnemonic(g,app, it.label(),it.mnemonic(),
					(dx + 1) * TerminalApplication.CELL_W, iy,
					fg, t.highlightMn());
		}
	}
	private void drawLabelWithMnemonic(GuiGraphics g, TerminalApplication app, String label, char mnemonic, int x, int y, int fg, int mnemColor) {
		app.drawDos(g,label,x,y,fg);
		int idx = -1;
		for (int i = 0; i < label.length(); i++) {
			if (Character.toUpperCase(label.charAt(i)) == Character.toUpperCase(mnemonic)) {
				idx = i;
				break;
			}
		}
		if (idx < 0) return;

		int cellX = x + idx * TerminalApplication.CELL_W;
		app.drawDos(g,String.valueOf(label.charAt(idx)),cellX,y,mnemColor);
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (!active) return false;
		switch (key) {
			case GLFW.GLFW_KEY_ESCAPE:
				close();
				return true;
			case GLFW.GLFW_KEY_LEFT:
				selectedMenu = (selectedMenu <= 0) ? menus.size() - 1 : selectedMenu - 1;
				selectedItem = -1;
				return true;
			case GLFW.GLFW_KEY_RIGHT:
				selectedMenu = (selectedMenu + 1) % menus.size();
				selectedItem = -1;
				return true;
			case GLFW.GLFW_KEY_DOWN:
				selectedItem = (selectedItem + 1) % menus.get(selectedMenu).items().size();
				return true;
			case GLFW.GLFW_KEY_UP: {
				int n = menus.get(selectedMenu).items().size();
				selectedItem = (selectedItem < 0) ? n - 1 : (selectedItem - 1 + n) % n;
				return true;
			}
			case GLFW.GLFW_KEY_ENTER:
			case GLFW.GLFW_KEY_KP_ENTER:
				if (selectedItem >= 0) {
					String action = menus.get(selectedMenu).items().get(selectedItem).action();
					close();
					if (onAction != null) onAction.accept(action);
				}
				return true;
		}
		// Letter pressed while open  try to match a top-level or item mnemonic.
		if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) {
			char c = (char) key;
			if (selectedMenu >= 0) {
				for (Item it : menus.get(selectedMenu).items()) {
					if (Character.toUpperCase(it.mnemonic()) == Character.toUpperCase(c)) {
						String action = it.action();
						close();
						if (onAction != null) onAction.accept(action);
						return true;
					}
				}
			}
			return openByMnemonic(c);
		}
		return false;
	}	
}
