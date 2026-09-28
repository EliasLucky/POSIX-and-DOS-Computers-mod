package com.eliaslucky.mc_dos.client.tui;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import net.minecraft.client.gui.GuiGraphics;

import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A two-column table of key/value rows, matching the classic BIOS
 * SETUP layout. Rows are drawn as {@code [N] Key  Value} with the
 * numeric index in brackets, exactly like the IBM AT SETUP screen.
 *
 * <p>Keyboard navigation uses arrow keys. Enter fires the
 * {@linkplain Row#action() row's action} string through the
 * {@linkplain #onAction(java.util.function.Consumer) action handler},
 * letting the host open a sub-editor for the selected value. Rows
 * marked {@linkplain Row#editable() non-editable} skip the action
 * callback but remain selectable for reference.
 *
 * @since 1.5
 */
public class TuiKeyValueTable implements TuiWidget {
	/**
	 * One row of the table.
	 *
	 * @param key	   left column text, e.g. {@code "Floppy Disk A:"}
	 * @param value    right column text, e.g. {@code "1.44M 3.5\""}
	 * @param editable whether the row can be edited by pressing Enter
	 * @param action   action identifier passed to the handler on Enter;
	 *				   {@code null} means no action
	 */
	public record Row(String key, String value, boolean editable, String action) {}

	private final int row, col, width, height;
	private final List<Row> rows = new ArrayList<>();
	private int selected = 0;
	private Consumer<String> onAction;

	/**
	 * @param row	 top-left row in cells
	 * @param col	 top-left column in cells
	 * @param width  widget width in cells
	 * @param height widget height in cells
	 */
	public TuiKeyValueTable(int row, int col, int width, int height) {
		this.row = row;
		this.col = col;
		this.width = width;
		this.height = height;
	}

	/**
	 * Replace the table contents.
	 *
	 * @param newRows the new rows
	 */
	public void setRows(List<Row> newRows) {
		rows.clear();
		rows.addAll(newRows);
		if (selected >= rows.size()) selected = Math.max(0, rows.size() - 1);
	}

	/**
	 * Replace the value of a row in place.
	 *
	 * @param index the row index
	 * @param value the new value text
	 */
	public void setValue(int index, String value) {
		if (index < 0 || index >= rows.size()) return;
		Row r = rows.get(index);
		rows.set(index, new Row(r.key(), value, r.editable(), r.action()));
	}

	/** @return the index of the currently highlighted row. */
	public int selectedIndex() { return selected; }

	/**
	 * Set the highlighted row.
	 *
	 * @param i the row index; clamps to the table bounds
	 */
	public void setSelected(int i) {
		selected = Math.max(0, Math.min(rows.size() - 1, i));
	}

	/**
	 * Register a callback fired when Enter is pressed on an editable row.
	 *
	 * @param c the callback, receiving the row's action string
	 */
	public void onAction(Consumer<String> c) { this.onAction = c; }

	@Override public int row()	  { return row; }
	@Override public int col()	  { return col; }
	@Override public int width()  { return width; }
	@Override public int height() { return height; }

	@Override
	public void render(GuiGraphics g, TerminalApplication app) {
		TuiTheme t = app.theme();

		for (int i = 0; i < rows.size() && i < height; i++) {
			Row r = rows.get(i);
			int y = (row + i) * TerminalApplication.CELL_H;
			int px = col * TerminalApplication.CELL_W;
			int pw = width * TerminalApplication.CELL_W;

			boolean hot = (i == selected);
			if (hot) g.fill(px, y, px + pw, y + TerminalApplication.CELL_H,
					t.highlightBg());

			int fg = hot ? t.highlightFg() : t.screenFg();
			int vfg = hot ? t.highlightFg()
						  : (r.editable() ? t.value() : t.screenFg());

			String left = "[" + i + "] " + r.key();
			app.drawDos(g, left, px, y, fg);

			String value = r.value();
			int valueCol = col + width - value.length() - 1;
			if (valueCol < col + 4) valueCol = col + 4;
			app.drawDos(g, value, valueCol * TerminalApplication.CELL_W, y, vfg);
		}
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (rows.isEmpty()) return false;
		switch (key) {
			case GLFW.GLFW_KEY_UP:
				selected = Math.max(0, selected - 1);
				return true;
			case GLFW.GLFW_KEY_DOWN:
				selected = Math.min(rows.size() - 1, selected + 1);
				return true;
			case GLFW.GLFW_KEY_HOME:
				selected = 0;
				return true;
			case GLFW.GLFW_KEY_END:
				selected = rows.size() - 1;
				return true;
			case GLFW.GLFW_KEY_ENTER:
			case GLFW.GLFW_KEY_KP_ENTER:
				Row r = rows.get(selected);
				if (r.editable() && r.action() != null && onAction != null) {
					onAction.accept(r.action());
				}
				return true;
		}
		return false;
	}
}
