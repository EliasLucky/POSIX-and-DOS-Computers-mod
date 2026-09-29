package com.eliaslucky.mc_dos.client.tui;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import net.minecraft.client.gui.GuiGraphics;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A modal dialog box, styled after the QBASIC error dialogs and the
 * IBM AT BIOS warning popups.
 *
 * <p>A dialog has:
 * <ul>
 *	 <li>optional static lines of text (title, body, blank spacing)</li>
 *	 <li>one or more selectable items rendered as
 *		 {@code < Label >} with the selection highlighted</li>
 *	 <li>Tab to cycle items, Enter to activate, Escape to dismiss
 *		 with a cancel action</li>
 * </ul>
 *
 * <p>Two layouts are supported:
 * <ul>
 *	 <li>{@link Layout#VERTICAL} — items stacked in a single column,
 *		 navigated with UP/DOWN. This is the classic QBasic dialog
 *		 shape, used by the welcome screen and the error prompts.</li>
 *	 <li>{@link Layout#HORIZONTAL} — items arranged in one row,
 *		 navigated with LEFT/RIGHT. Used by dialogs whose buttons sit
 *		 side by side, like QBasic's "Save it now?" prompt.</li>
 * </ul>
 * 
 * <p>Dialogs are typically used by pushing them to a
 * {@link TuiScreen} at the top of the widget list so they draw on
 * top of everything else and receive input first.
 *
 * @since 1.5
 */
public class TuiDialog implements TuiWidget {
	/**
	 * A selectable row inside the dialog.
	 *
	 * @param label  display text
	 * @param action action identifier fired when activated
	 */
	public record Item(String label, String action) {}

	public enum Layout { VERTICAL, HORIZONTAL }

	private final List<String> lines = new ArrayList<>();
	private final List<Item> items = new ArrayList<>();
	private int selected = 0;
	private Layout layout = Layout.VERTICAL;
	private Consumer<String> onAction;
	private Runnable onCancel;

	private boolean hasFrameBg = false;
	private boolean hasBorder = false;
	private boolean hasText = false;
	private boolean hasHotBg = false;
	private boolean hasHotFg = false;
	private int overrideFrameBg,overrideBorder,overrideText,overrideHotBg,overrideHotFg;

	/** Add a static line of text. */
	public TuiDialog addLine(String line) { lines.add(line == null ? "" : line); return this; }

	/** Add a selectable item. */
	public TuiDialog addItem(String label, String action) {
		items.add(new Item(label, action));
		return this;
	}

	/** Arrange items in a single row. */
	public TuiDialog horizontal() {
		this.layout = Layout.HORIZONTAL;
		return this;
	}

	/** Arrange items in a column (the default). */
	public TuiDialog vertical() {
		this.layout = Layout.VERTICAL;
		return this;
	}

	/** Register the item activation handler. */
	public TuiDialog onAction(Consumer<String> c) { this.onAction = c; return this; }

	/** Register the Escape / cancel handler. */
	public TuiDialog onCancel(Runnable r) { this.onCancel = r; return this; }

	// Sizing
	// Dialogs draw centered; the caller provides the screen dimensions
	// through the render call so the dialog can size itself.
	private int computedRow, computedCol, computedWidth, computedHeight;

	public TuiDialog fill(int c)   { this.overrideFrameBg = c; hasFrameBg = true; return this; }
	public TuiDialog border(int c) { this.overrideBorder  = c; hasBorder = true; return this; }
	public TuiDialog text(int c)   { this.overrideText = c; hasText = true; return this; }
	public TuiDialog highlight(int bg, int fg) {
		this.overrideHotBg = bg;
		this.overrideHotFg = fg;
		hasHotBg = true;
		hasHotFg = true;
		return this;
	}

	@Override public int row()	  { return computedRow; }
	@Override public int col()	  { return computedCol; }
	@Override public int width()  { return computedWidth; }
	@Override public int height() { return computedHeight; }

	@Override
	public void render(GuiGraphics g, TerminalApplication app) {
		// Compute size and position based on text lengths and screen.
		TuiTheme t = app.theme();

		int frameBg = hasFrameBg ? overrideFrameBg : t.frameBg();
		int border  = hasBorder ? overrideBorder  : t.border();
		int textFg  = hasText ? overrideText    : t.titleFg();
		int hotBg   = hasHotBg ? overrideHotBg   : t.highlightBg();
		int hotFg   = hasHotFg ? overrideHotFg   : t.highlightFg();

		int innerW = 0;
		for (String s : lines) innerW = Math.max(innerW, s.length());
		
		if (layout == Layout.HORIZONTAL) {
			int rowW = 0;
			for (int i = 0; i < items.size(); i++) {
				if (i > 0) rowW += 2;
				rowW += items.get(i).label().length() + 4;
			}
			innerW = Math.max(innerW,rowW);
		}
		else {
			for (Item it : items) {
				innerW = Math.max(innerW,it.label().length()+4);
			}
		}

		int w = innerW + 4;
		int h = 2 + lines.size() + (layout == Layout.HORIZONTAL ? 1 : items.size());

		// Position
		int cols = app.cols();
		int rows = app.rows();
		int x = Math.max(0, (cols - w) / 2);
		int y = Math.max(0, (rows - h) / 2);

		this.computedRow = y;
		this.computedCol = x;
		this.computedWidth = w;
		this.computedHeight = h;

		int px = x * TerminalApplication.CELL_W;
		int py = y * TerminalApplication.CELL_H;
		int pw = w * TerminalApplication.CELL_W;
		int ph = h * TerminalApplication.CELL_H;

		// Shadow
		g.fill(px + TerminalApplication.CELL_W,
				py + TerminalApplication.CELL_H,
				px + pw + TerminalApplication.CELL_W,
				py + ph + TerminalApplication.CELL_H,
				TuiPalette.BLACK);

		// Body fill
		//g.fill(px,py,px+pw,py+ph,frameBg);

		// Body use a TuiBox for the border.
		TuiBox box = new TuiBox(y, x, w, h, TuiBox.Style.SINGLE)
			.border(border)
			.fill(frameBg);
		box.render(g,app);

		int cy = y + 1;
		int centerX = x + w / 2;
		for (String s : lines) {
			int col = centerX - s.length() / 2;
			app.drawDos(g, s, col * TerminalApplication.CELL_W, cy * TerminalApplication.CELL_H, textFg);
			cy++;
		}

		if (layout == Layout.HORIZONTAL) {
			int rowW = 0;
			for (int i = 0; i < items.size(); i++) {
				if (i > 0) rowW += 2;
				rowW += items.get(i).label().length()+4;
			}
			int cursorCol = centerX - rowW / 2;

			for (int i = 0; i < items.size(); i++) {
				if (i > 0) cursorCol += 2;
				String label = items.get(i).label();
				int cellW = label.length()+4;
				boolean hot = (i == selected);

				int cellPx = cursorCol * TerminalApplication.CELL_W;
				int cellPy = cy * TerminalApplication.CELL_H;
				int cellPw = cellW * TerminalApplication.CELL_W;

				if (hot) {
					g.fill(cellPx,cellPy,cellPx+cellPw,cellPy + TerminalApplication.CELL_H, hotBg);
				}
				String display = hot ? "< " + label + " >" : " " + label + " ";
				app.drawDos(g,display,cellPx,cellPy,hot ? hotFg : textFg);
				cursorCol += cellW;
			}
		}
		else {
			for (int i = 0; i < items.size(); i++) {
				String label = items.get(i).label();
				int cellW = label.length()+4;
				int cellCol = centerX - cellW / 2;
				boolean hot = (i == selected);

				int cellPx = cellCol * TerminalApplication.CELL_W;
				int cellPy = cy * TerminalApplication.CELL_H;
				int cellPw = cellW * TerminalApplication.CELL_W;

				if (hot) {
					g.fill(cellPx,cellPy,cellPx+cellPw,cellPy + TerminalApplication.CELL_H,hotBg);
				}
				String display = hot ? "< " + label + " >" : " " + label + " ";
				app.drawDos(g,display,cellPx,cellPy, hot ? hotFg : textFg);
				cy++;
			}
		}
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (items.isEmpty()) {
			if (key == GLFW.GLFW_KEY_ESCAPE && onCancel != null) onCancel.run();
			return true;
		}
		switch (key) {
			case GLFW.GLFW_KEY_UP:
				if (layout == Layout.VERTICAL) selected = Math.max(0, selected - 1);
				return true;
			case GLFW.GLFW_KEY_DOWN:
				if (layout == Layout.VERTICAL) selected = Math.min(items.size() - 1, selected + 1);
				return true;
			case GLFW.GLFW_KEY_LEFT:
				if (layout == Layout.HORIZONTAL) selected = (selected - 1 + items.size()) % items.size();
				return true;
			case GLFW.GLFW_KEY_RIGHT:
				if (layout == Layout.HORIZONTAL) selected = (selected + 1) % items.size();
				return true;
			case GLFW.GLFW_KEY_TAB:
				selected = (selected + 1) % items.size();
				return true;
			case GLFW.GLFW_KEY_ENTER:
			case GLFW.GLFW_KEY_KP_ENTER:
				if (onAction != null) {
					onAction.accept(items.get(selected).action());
				}
				return true;
			case GLFW.GLFW_KEY_ESCAPE:
				if (onCancel != null) onCancel.run();
				return true;
		}
		return false;
	}

	@Override
	public boolean charTyped(char cp, int mods) {
		return true;
	}
}
