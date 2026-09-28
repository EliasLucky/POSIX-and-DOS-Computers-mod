package com.eliaslucky.mc_dos.client.tui;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import net.minecraft.client.gui.GuiGraphics;

/**
 * A horizontal progress bar. Renders as a filled bar of block-drawing
 * characters, capped at the ends.
 *
 * <p>Progress is a fraction between 0.0 and 1.0. The bar fills from
 * left to right; the un-filled portion is drawn in the empty colour.
 *
 * @since 1.
 */
public class TuiProgressBar implements TuiWidget {
	private final int row, col, width;
	private double progress;

	/**
	 * @param row	row in character cells
	 * @param col	left column in character cells
	 * @param width widget width in character cells
	 */
	public TuiProgressBar(int row, int col, int width) {
		this.row = row;
		this.col = col;
		this.width = width;
	}

	/**
	 * Set the current progress.
	 *
	 * @param p a fraction between 0.0 and 1.0; values outside the range
	 *			are clamped
	 */
	public void setProgress(double p) {
		this.progress = Math.max(0.0, Math.min(1.0, p));
	}

	/** @return the current progress as a fraction between 0.0 and 1.0. */
	public double progress() { return progress; }

	@Override public int row()	  { return row; }
	@Override public int col()	  { return col; }
	@Override public int width()  { return width; }
	@Override public int height() { return 1; }

	@Override
	public void render(GuiGraphics g, TerminalApplication app) {
		TuiTheme t = app.theme();
		
		int filled = (int) Math.round(progress * width);
		int px = col * TerminalApplication.CELL_W;
		int y = row * TerminalApplication.CELL_H;

		for (int i = 0; i < width; i++) {
			boolean on = i < filled;
			app.drawDos(g, "\u2588",
					px + i * TerminalApplication.CELL_W, y,
					on ? t.progressFill() : t.progressEmpty());
		}
	}
}
