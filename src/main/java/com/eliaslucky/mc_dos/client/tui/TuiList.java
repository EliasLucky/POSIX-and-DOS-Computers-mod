package com.eliaslucky.mc_dos.client.tui;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import net.minecraft.client.gui.GuiGraphics;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A vertical list of strings with keyboard selection.
 *
 * <p>Arrow keys move the highlight. Enter fires the
 * {@linkplain #onActivate(java.util.function.Consumer) activation
 * callback} with the selected index. Home and End jump to the
 * extremes. The list scrolls when the number of items exceeds the
 * widget's height.
 *
 * <p>Selection is by index and persists across item list changes;
 * call {@link #setItems(java.util.List)} carefully if you plan to
 * replace the list while a selection is active.
 *
 * @since 1.5
 */
public class TuiList implements TuiWidget {
	private final int row, col, width, height;
	private final List<String> items = new ArrayList<>();
	private int selected = 0;
	private int scrollTop = 0;
	private Consumer<Integer> onActivate;

	/**
	 * @param row	 top-left row in cells
	 * @param col	 top-left column in cells
	 * @param width  widget width in cells
	 * @param height widget height in cells
	 */
	public TuiList(int row, int col, int width, int height) {
		this.row = row;
		this.col = col;
		this.width = width;
		this.height = height;
	}

	/**
	 * Replace the item list.
	 *
	 * @param newItems the new items
	 */
	public void setItems(List<String> newItems) {
		items.clear();
		items.addAll(newItems);
		if (selected >= items.size()) selected = Math.max(0, items.size() - 1);
		clampScroll();
	}

	/** @return the index of the currently highlighted item, or {@code -1} if empty. */
	public int selectedIndex() { return items.isEmpty() ? -1 : selected; }

	/** @return the currently highlighted item, or {@code null} if empty. */
	public String selectedItem() { return items.isEmpty() ? null : items.get(selected); }

	/**
	 * Set the index of the highlighted item.
	 *
	 * @param i the index; clamps to the list bounds
	 */
	public void setSelected(int i) {
		selected = Math.max(0, Math.min(items.size() - 1, i));
		clampScroll();
	}

	/**
	 * Register a callback fired when the user presses Enter on a row.
	 *
	 * @param c the callback, receiving the selected index
	 */
	public void onActivate(Consumer<Integer> c) { this.onActivate = c; }

	@Override public int row()	  { return row; }
	@Override public int col()	  { return col; }
	@Override public int width()  { return width; }
	@Override public int height() { return height; }

	@Override
	public void render(GuiGraphics g, TerminalApplication app) {
		TuiTheme t = app.theme();

		for (int i = 0; i < height; i++) {
			int itemIndex = scrollTop + i;
			if (itemIndex >= items.size()) break;

			String text = items.get(itemIndex);
			if (text.length() > width) text = text.substring(0, width);

			int y = (row + i) * TerminalApplication.CELL_H;
			boolean hot = (itemIndex == selected);

			if (hot) {
				int px = col * TerminalApplication.CELL_W;
				int pw = width * TerminalApplication.CELL_W;
				g.fill(px, y, px + pw, y + TerminalApplication.CELL_H,
						t.highlightBg());
			}
			app.drawDos(g, text, col * TerminalApplication.CELL_W, y,
					hot ? t.highlightFg() : t.screenFg());
		}
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (items.isEmpty()) return false;
		switch (key) {
			case GLFW.GLFW_KEY_UP:
				selected = Math.max(0, selected - 1);
				clampScroll();
				return true;
			case GLFW.GLFW_KEY_DOWN:
				selected = Math.min(items.size() - 1, selected + 1);
				clampScroll();
				return true;
			case GLFW.GLFW_KEY_HOME:
				selected = 0;
				clampScroll();
				return true;
			case GLFW.GLFW_KEY_END:
				selected = items.size() - 1;
				clampScroll();
				return true;
			case GLFW.GLFW_KEY_ENTER:
			case GLFW.GLFW_KEY_KP_ENTER:
				if (onActivate != null) onActivate.accept(selected);
				return true;
		}
		return false;
	}

	private void clampScroll() {
		if (selected < scrollTop) scrollTop = selected;
		if (selected >= scrollTop + height) scrollTop = selected - height + 1;
		scrollTop = Math.max(0, Math.min(scrollTop, Math.max(0, items.size() - height)));
	}
}
