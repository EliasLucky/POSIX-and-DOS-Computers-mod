package com.eliaslucky.mc_dos.client.tui;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A single line of static text. Never consumes input.
 *
 * <p>Useful for titles, help lines, and captions inside a
 * {@link TuiBox}.
 *
 * @since 1.5
 */
public class TuiLabel implements TuiWidget {
	private final int row;
	private final int col;
	private String text;
	private int color = -1;

	/**
	 * @param row	row in character cells
	 * @param col	column in character cells
	 * @param text	initial text
	 */
	public TuiLabel(int row, int col, String text) {
		this.row = row;
		this.col = col;
		this.text = text == null ? "" : text;
	}

	/**
	 * @param row	row in character cells
	 * @param col	column in character cells
	 * @param text	initial text
	 * @param color foreground colour, from {@link TuiPalette}
	 */
	public TuiLabel(int row, int col, String text, int color) {
		this(row,col,text);
		this.color = color;
	}

	/**
	 * Change the displayed text.
	 *
	 * @param text the new text; {@code null} is treated as {@code ""}
	 */
	public void setText(String text) {
		this.text = text == null ? "" : text;
	}

	/**
	 * Change the text colour.
	 *
	 * @param color a colour from {@link TuiPalette}
	 */
	public void setColor(int color) { this.color = color; }

	@Override public int row()	  { return row; }
	@Override public int col()	  { return col; }
	@Override public int width()  { return text.length(); }
	@Override public int height() { return 1; }

	@Override
	public void render(GuiGraphics g, TerminalApplication app) {
		int fg = (color >= 0) ? color : app.theme().screenFg();
		app.drawDos(g, text, col * TerminalApplication.CELL_W,
				row * TerminalApplication.CELL_H, color);
	}
}
