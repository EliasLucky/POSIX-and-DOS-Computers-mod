package com.eliaslucky.mc_dos.client.tui;

/**
 * A mapping from semantic UI roles to color indices.
 *
 * <p>Where {@link TuiPalette} lists what colors exist, a theme
 * decides which color each role uses. Two apps that share the same
 * {@code TuiPalette} can look completely different by installing
 * different {@code TuiTheme}s.
 *
 * <p>Instances are immutable. Use the {@code withX} helpers to derive
 * variants, or construct a new one from scratch. Addons typically
 * declare a {@code public static final TuiTheme MY_THEME} and pass it
 * to their widgets.
 *
 * @param screenBg      background behind everything
 * @param screenFg      default text colour
 * @param titleBg       menu-bar and title-bar background
 * @param titleFg       menu-bar and title-bar text
 * @param highlightBg   selected-row background
 * @param highlightFg   selected-row text
 * @param highlightMn   selected-row mnemonic character
 * @param frameBg       dialog and frame interior
 * @param border        box and border colour
 * @param statusBg      status bar background
 * @param statusFg      status bar text
 * @param value         editable or emphasized text
 * @param disabled      greyed-out text
 * @param warning       warning text
 * @param error         error text
 * @param success       success text
 *
 * @since 1.5
 */
public record TuiTheme(
        int screenBg,
        int screenFg,
        int titleBg,
        int titleFg,
        int highlightBg,
        int highlightFg,
        int highlightMn,
        int frameBg,
        int border,
        int statusBg,
        int statusFg,
        int value,
        int disabled,
        int warning,
        int error,
        int success
) {
    /**
     * A bare theme that only fills the most common roles; all others
     * default to {@link TuiPalette#LIGHT_GRAY} on {@link TuiPalette#BLACK}.
     * Useful for quick prototypes and tests.
     */
    public static TuiTheme minimal() {
        return new TuiTheme(
                TuiPalette.BLACK,   TuiPalette.LIGHT_GRAY,
                TuiPalette.LIGHT_GRAY, TuiPalette.BLACK,
                TuiPalette.BLUE,    TuiPalette.WHITE,  TuiPalette.YELLOW,
                TuiPalette.LIGHT_GRAY, TuiPalette.BLACK,
                TuiPalette.LIGHT_GRAY, TuiPalette.BLACK,
                TuiPalette.WHITE,
                TuiPalette.DARK_GRAY,
                TuiPalette.YELLOW, TuiPalette.LIGHT_RED, TuiPalette.LIGHT_GREEN);
    }

    /** @return a copy with a different screen background. */
    public TuiTheme withScreenBg(int c) {
        return new TuiTheme(c, screenFg, titleBg, titleFg, highlightBg, highlightFg,
                highlightMn, frameBg, border, statusBg, statusFg, value, disabled,
                warning, error, success);
    }
}
