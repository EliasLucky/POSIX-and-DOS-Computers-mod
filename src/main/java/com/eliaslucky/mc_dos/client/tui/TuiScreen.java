package com.eliaslucky.mc_dos.client.tui;

import com.eliaslucky.mc_dos.client.apps.TerminalApplication;

import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * A container that hosts TUI widgets and dispatches input to them.
 *
 * <p>Rendering is top-down: each widget draws in the order it was
 * added. Input routing sends keys to the focused widget first; if it
 * doesn't consume the event, they go to each other widget in turn.
 *
 * <p>Widgets can be added and removed at runtime. Focus is updated
 * automatically when the focused widget is removed.
 *
 * @since 1.5
 */
public class TuiScreen {
    private final List<TuiWidget> widgets = new ArrayList<>();
    private TuiWidget focused;

    /**
     * Add a widget to the screen. The widget draws after any
     * previously added widget.
     *
     * @param w the widget; never {@code null}
     */
    public void add(TuiWidget w) {
        widgets.add(w);
    }

    /**
     * Remove a widget. If it held focus, focus is cleared.
     *
     * @param w the widget to remove
     */
    public void remove(TuiWidget w) {
        widgets.remove(w);
        if (focused == w) focused = null;
    }

    /** Remove all widgets. */
    public void clear() {
        widgets.clear();
        focused = null;
    }

    /**
     * Give keyboard focus to a widget. Pass {@code null} to clear.
     *
     * @param w the widget to focus
     */
    public void setFocus(TuiWidget w) {
        if (focused != null) focused.setFocused(false);
        focused = w;
        if (focused != null) focused.setFocused(true);
    }

    /** @return the currently focused widget, or {@code null}. */
    public TuiWidget getFocus() { return focused; }

    /** @return an unmodifiable view of the widget list. */
    public List<TuiWidget> widgets() { return List.copyOf(widgets); }

    /**
     * Render all widgets in order.
     *
     * @param g   the graphics context
     * @param app the hosting application
     */
    public void render(GuiGraphics g, TerminalApplication app) {
        for (TuiWidget w : widgets) w.render(g, app);
    }

    /**
     * Dispatch a key event. The focused widget gets first refusal,
     * then each other widget in insertion order.
     *
     * @param key  GLFW key code
     * @param scan platform scan code
     * @param mods modifier bitfield
     * @return {@code true} if any widget consumed the event
     */
    public boolean keyPressed(int key, int scan, int mods) {
        if (focused != null && focused.keyPressed(key, scan, mods)) return true;
        for (TuiWidget w : widgets) {
            if (w == focused) continue;
            if (w.keyPressed(key, scan, mods)) return true;
        }
        return false;
    }

    /**
     * Dispatch a character event. Same order as {@link #keyPressed}.
     *
     * @param cp   the character
     * @param mods modifier bitfield
     * @return {@code true} if any widget consumed the event
     */
    public boolean charTyped(char cp, int mods) {
        if (focused != null && focused.charTyped(cp, mods)) return true;
        for (TuiWidget w : widgets) {
            if (w == focused) continue;
            if (w.charTyped(cp, mods)) return true;
        }
        return false;
    }

    /**
     * Dispatch a mouse click. Widgets are offered the event in reverse
     * insertion order, so widgets drawn on top receive the click first.
     *
     * @param mx     mouse X in pixels
     * @param my     mouse Y in pixels
     * @param button mouse button index
     * @return {@code true} if any widget consumed the event
     */
    public boolean mouseClicked(double mx, double my, int button) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).mouseClicked(mx, my, button)) return true;
        }
        return false;
    }
}
