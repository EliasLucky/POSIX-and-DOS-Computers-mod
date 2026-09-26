package com.eliaslucky.mc_dos.client.tui;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base type for every TUI widget.
 *
 * <p>A widget occupies a rectangular region of the character grid
 * (row, col, width, height) and knows how to draw itself and handle
 * key and mouse events that fall within its bounds. Widgets are
 * composed by a {@link TuiScreen}, which routes events to the focused
 * widget first, then to the rest in order.
 *
 * <p>All positioning is in character cells, not pixels. Widgets
 * translate to pixels using {@link TerminalApplication#CELL_W} and
 * {@link TerminalApplication#CELL_H} when rendering.
 *
 * @since 1.5
 */
public interface TuiWidget {
    /** @return the top row of this widget in character cells. */
    int row();

    /** @return the left column of this widget in character cells. */
    int col();

    /** @return the width of this widget in character cells. */
    int width();

    /** @return the height of this widget in character cells. */
    int height();

    /**
     * Draw the widget. Called every frame.
     *
     * @param g   the graphics context
     * @param app the hosting application, for {@code drawDos} and palette access
     */
    void render(GuiGraphics g, TerminalApplication app);

    /**
     * Handle a key press.
     *
     * @param keyCode   GLFW key code
     * @param scanCode  platform scan code
     * @param modifiers GLFW modifier bitfield
     * @return {@code true} if the event was consumed
     */
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    /**
     * Handle a typed character.
     *
     * @param cp   the character
     * @param mods modifier bitfield
     * @return {@code true} if the event was consumed
     */
    default boolean charTyped(char cp, int mods) {
        return false;
    }

    /**
     * Handle a mouse click. Coordinates are in pixels; widgets
     * typically convert to cells using {@code TerminalApplication.CELL_W}
     * and {@code CELL_H}.
     *
     * @param mx     mouse X in pixels
     * @param my     mouse Y in pixels
     * @param button mouse button index
     * @return {@code true} if the event was consumed
     */
    default boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    /** @return {@code true} if this widget currently owns keyboard focus. */
    default boolean focused() { return false; }

    /**
     * Set the focus state of this widget. Called by {@link TuiScreen}
     * when focus moves between widgets.
     *
     * @param f the new focus state
     */
    default void setFocused(boolean f) {}
}
