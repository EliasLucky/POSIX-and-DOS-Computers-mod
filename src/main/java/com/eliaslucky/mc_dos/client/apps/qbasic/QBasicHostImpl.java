package com.eliaslucky.mc_dos.client.apps.qbasic;

import java.util.ArrayDeque;
import java.util.Deque;

import com.eliaslucky.mc_dos.blocks.computer.basic.Host;
import com.eliaslucky.mc_dos.client.apps.TerminalApplication;
import com.eliaslucky.mc_dos.client.apps.display.*;

public class QBasicHostImpl implements Host {
	private final TerminalApplication app;
	private int fg = 15;	// current foreground index (white)
	private int bg = 0;
	private int[] viewport = null;
	private int viewportOffsetX = 0;
	private int viewportOffsetY = 0;
	private final Deque<String> keyQueue = new ArrayDeque<>();
	private static final int MAX_KEY_QUEUE = 64;

	public QBasicHostImpl(TerminalApplication app) { this.app = app; }

	@Override
	public void print(String s) {
		if (!(app.getDisplayMode() instanceof TextDisplayMode t)) return;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\r') continue;
			if (c == '\n') {
				int row = t.getCursorRow() + 1, col = 0;
				if (row >= t.rows) { t.scrollUp(1); row = t.rows - 1; }
				t.setCursor(row, col);
			} else {
				int row = t.getCursorRow(), col = t.getCursorCol();
				t.writeChar(row, col, c, t.getForeground(), t.getBackground());
				col++;
				if (col >= t.cols) {
					col = 0; row++;
					if (row >= t.rows) { t.scrollUp(1); row = t.rows - 1; }
				}
				t.setCursor(row, col);
			}
		}
	}

	@Override public void printNewline() { print("\n"); }
	@Override public void cls()			 { app.getDisplayMode().clear(1); }

	@Override
	public void setScreenMode(int mode) {
		switch (mode) {
			case 0	-> app.setDisplayMode(new Screen0Text());
			case 7	-> app.setDisplayMode(new Screen7EGA());
			case 13 -> app.setDisplayMode(new Screen13VGA());
			default -> { /* unsupported: keep current */ }
		}
	}

	@Override public void setPixel(int x, int y, int c) {
		int ax = x + viewportOffsetX;
		int ay = y + viewportOffsetY;
		plot(ax, ay, c);
	}
	@Override public void pset(int x, int y, int c)		{ setPixel(x, y, c); }

	@Override public void drawLine(int x1, int y1, int x2, int y2, int c, int style) {
		// Bresenham
		int sx = x1 + viewportOffsetX, sy = y1 + viewportOffsetY;
		int ex = x2 + viewportOffsetX, ey = y2 + viewportOffsetY;

		int dx = Math.abs(ex - sx), dy = Math.abs(ey - sy);
		int stepX = sx < ex ? 1 : -1, stepY = sy < ey ? 1 : -1;
		int err = dx - dy;
		int cx = sx, cy = sy;

		while (true) {
			plot(cx, cy, c);
			if (cx == ex && cy == ey) break;
			int e2 = 2 * err;
			if (e2 > -dy) { err -= dy; cx += stepX; }
			if (e2 <  dx) { err += dx; cy += stepY; }
		}
	}
	@Override public void circle(int cx, int cy, int r, int c, boolean filled) {
		if (r < 0) return;
		int scx = cx + viewportOffsetX, scy = cy + viewportOffsetY;

		// Midpoint circle
		int x = r, y = 0, err = 1 - r;
		while (x >= y) {
			if (filled) {
				for (int xx = scx - x; xx <= scx + x; xx++) {
					plot(xx, scy + y, c);
					plot(xx, scy - y, c);
				}
				for (int xx = scx - y; xx <= scx + y; xx++) {
					plot(xx, scy + x, c);
					plot(xx, scy - x, c);
				}
			} else {
				plot8(scx, scy, x, y, c);
			}
			y++;
			if (err < 0) err += 2 * y + 1;
			else { x--; err += 2 * (y - x) + 1; }
		}
	}
	private void plot8(int cx, int cy, int x, int y, int color) {
		plot(cx + x, cy + y, color);
		plot(cx - x, cy + y, color);
		plot(cx + x, cy - y, color);
		plot(cx - x, cy - y, color);
		plot(cx + y, cy + x, color);
		plot(cx - y, cy + x, color);
		plot(cx + y, cy - x, color);
		plot(cx - y, cy - x, color);
	}
	private void plot(int sx,int sy, int color) {
		if (viewport != null) {
			if (sx < viewport[0] || sx > viewport[2] || sy < viewport[1] || sy > viewport[3]) return;
		}
		app.getDisplayMode().setPixel(sx,sy,color);
	}
	@Override public int colorFg() { return fg; }
	@Override public void locate(int row, int col) {
		if (app.getDisplayMode() instanceof TextDisplayMode t) t.setCursor(row, col);
	}
	@Override public void color(int fg, int bg) {
		this.fg = fg & 0xFF;
		this.bg = bg & 0xFF;
		if (app.getDisplayMode() instanceof TextDisplayMode t) t.setAttribute(fg, bg);
	}
	@Override public void beep()		{ /* no-op */ }
	@Override public void sleep(int ms) { /* no-op for now — blocking freezes the tick */ }
	@Override public void end()			{ /* optional: clear a "running" flag */ }

	@Override
	public void runtimeError(int code, String msg, int line) {
		printNewline();
		print("Runtime error " + code + " at line " + line + ": " + msg);
	}
	@Override
	public void setViewport(int x1, int y1, int x2, int y2, int borderColor, boolean screen) {
		// Normalize (QBasic allows either corner first).
		int vx1 = Math.min(x1, x2), vy1 = Math.min(y1, y2);
		int vx2 = Math.max(x1, x2), vy2 = Math.max(y1, y2);
		this.viewport = new int[]{ vx1, vy1, vx2, vy2 };

		// VIEW SCREEN coordinates are relative to the viewport's top-left
		// VIEW coordinates absolute
		this.viewportOffsetX = screen ? 0 : vx1;
		this.viewportOffsetY = screen ? 0 : vy1;

		if (borderColor >= 0) {
			// Draw the border rectangle in the border color.
			for (int x = vx1; x <= vx2; x++) {
				app.getDisplayMode().setPixel(x, vy1, borderColor);
				app.getDisplayMode().setPixel(x, vy2, borderColor);
			}
			for (int y = vy1; y <= vy2; y++) {
				app.getDisplayMode().setPixel(vx1, y, borderColor);
				app.getDisplayMode().setPixel(vx2, y, borderColor);
			}
		}
	}
	@Override public boolean hasKey() { return !keyQueue.isEmpty(); }

	@Override public String pollKey() {
		return keyQueue.isEmpty() ? "" : keyQueue.pollFirst();
	}

	/** Called by the client when a key is typed while a program is running. */
	public void enqueueKey(String key) { 
		if (keyQueue.size() >= MAX_KEY_QUEUE) keyQueue.pollFirst();
		keyQueue.addLast(key);
	}

	@Override
	public void resetViewport() {
		this.viewport = null;
		this.viewportOffsetX = 0;
		this.viewportOffsetY = 0;
	}
	
	public void backspaceChar() {
		if (!(app.getDisplayMode() instanceof TextDisplayMode t)) return;

		int col = t.getCursorCol() - 1;
		int row = t.getCursorRow();
		if (col < 0) {
			if (row > 0) { row--; col = t.cols - 1; }
			else return;
		}
		t.writeChar(row, col, ' ', t.getForeground(), t.getBackground());
		t.setCursor(row, col);
	}
}
