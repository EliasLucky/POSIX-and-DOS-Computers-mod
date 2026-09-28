package com.eliaslucky.mc_dos.client.tui;

/**
 * Color constants for TUI widgets.
 * @since 1.5
 */
public final class TuiPalette {
	private TuiPalette() {}

	// Base 16 (EGA/VGA text mode)
	public static final int BLACK		  = 0xFF000000;
	public static final int BLUE		  = 0xFF0000AA;
	public static final int GREEN		  = 0xFF00AA00;
	public static final int CYAN		  = 0xFF00AAAA;
	public static final int RED			  = 0xFFAA0000;
	public static final int MAGENTA		  = 0xFFAA00AA;
	public static final int BROWN		  = 0xFFAA5500;
	public static final int LIGHT_GRAY	  = 0xFFAAAAAA;

	public static final int DARK_GRAY	  = 0xFF555555;
	public static final int LIGHT_BLUE	  = 0xFF5555FF;
	public static final int LIGHT_GREEN   = 0xFF55FF55;
	public static final int LIGHT_CYAN	  = 0xFF55FFFF;
	public static final int LIGHT_RED	  = 0xFFFF5555;
	public static final int LIGHT_MAGENTA = 0xFFFF55FF;
	public static final int YELLOW		  = 0xFFFFFF55;
	public static final int WHITE		  = 0xFFFFFFFF;
	
	// Screen
	public static final int SCREEN_BG		= BLUE;
	public static final int SCREEN_FG		= LIGHT_GRAY;

	// Frames
	public static final int BORDER			= BLACK;
	public static final int FRAME_BG		= LIGHT_GRAY;
	public static final int TITLE_BG		= LIGHT_GRAY;
	public static final int TITLE_FG		= BLACK;

	// Menu bar
	public static final int MENU_BG			= CYAN;
	public static final int MENU_FG			= BLACK;
	public static final int MENU_HOT_BG		= LIGHT_CYAN;
	public static final int MENU_HOT_FG		= BLACK;

	// Selection
	public static final int HIGHLIGHT_BG	= CYAN;
	public static final int HIGHLIGHT_FG	= BLACK;
	public static final int HIGHLIGHT_MNEM	= RED;

	// Status and function key bars
	public static final int STATUS_BG		= CYAN;
	public static final int STATUS_FG		= BLACK;
	public static final int STATUS_KEY_BG	= LIGHT_CYAN;
	public static final int STATUS_KEY_FG	= BLACK;

	// Text roles
	public static final int NORMAL			= LIGHT_GRAY;
	public static final int DISABLED		= DARK_GRAY;
	public static final int VALUE			= WHITE;
	public static final int EDITABLE		= YELLOW;
	public static final int WARNING			= YELLOW;
	public static final int ERROR			= LIGHT_RED;
	public static final int SUCCESS			= LIGHT_GREEN;

	// Dialog
	public static final int DIALOG_BG		= LIGHT_GRAY;
	public static final int DIALOG_FG		= BLACK;
	public static final int DIALOG_SHADOW	= BLACK;

	// Progress
	public static final int PROGRESS_FILL	= WHITE;
	public static final int PROGRESS_EMPTY	= DARK_GRAY;

	// Scrollbar
	public static final int SCROLL_TRACK	= LIGHT_GRAY;
	public static final int SCROLL_THUMB	= DARK_GRAY;
	public static final int SCROLL_ARROW	= BLACK;
}
