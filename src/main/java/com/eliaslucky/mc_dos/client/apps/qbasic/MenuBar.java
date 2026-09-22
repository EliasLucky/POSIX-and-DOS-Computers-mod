package com.eliaslucky.mc_dos.client.apps.qbasic;

import com.eliaslucky.mc_dos.client.apps.display.DosPalette;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class MenuBar {

    private final List<Menu> menus;
    private boolean active;
    private int selectedMenu = -1;   // -1 = none yet
    private int selectedItem = -1;   // -1 = menu open but no item highlighted

    public MenuBar(List<Menu> menus) { this.menus = menus; }

    public void open()        { active = true; selectedMenu = 0; selectedItem = -1; }
    public void openByMnemonic(char m) {
        int idx = QBasicMenus.indexOfMnemonic(m);
        if (idx < 0) return;
        active = true;
        selectedMenu = idx;
        selectedItem = -1;
    }
    public void close()       { active = false; selectedMenu = -1; selectedItem = -1; }
    public boolean isActive() { return active; }

    /** Text for the footer, driven by whichever menu item is currently highlighted. */
    public String currentFooterHelp() {
        if (!active || selectedMenu < 0) return " Use arrow keys to select a menu";
        if (selectedItem < 0)
            return " " + menus.get(selectedMenu).label() + ": use UP/DOWN then ENTER";
        return " " + menus.get(selectedMenu).items().get(selectedItem).footerHelp();
    }

    /** Returns an action string, "__close__", or null (still navigating). */
    public String handleKey(int key) {
        switch (key) {
            case GLFW.GLFW_KEY_ESCAPE:
                close();
                return "__close__";

            case GLFW.GLFW_KEY_LEFT:
                if (selectedMenu <= 0) selectedMenu = menus.size() - 1;
                else selectedMenu--;
                selectedItem = -1;
                return null;

            case GLFW.GLFW_KEY_RIGHT:
                selectedMenu = (selectedMenu + 1) % menus.size();
                selectedItem = -1;
                return null;

            case GLFW.GLFW_KEY_DOWN: {
                List<MenuItem> items = menus.get(selectedMenu).items();
                selectedItem = (selectedItem + 1) % items.size();
                return null;
            }

            case GLFW.GLFW_KEY_UP: {
                List<MenuItem> items = menus.get(selectedMenu).items();
                selectedItem = (selectedItem < 0)
                        ? items.size() - 1
                        : (selectedItem - 1 + items.size()) % items.size();
                return null;
            }

            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER: {
                if (selectedItem < 0) return null;
                String action = menus.get(selectedMenu).items().get(selectedItem).action();
                close();
                return action;
            }

            case GLFW.GLFW_KEY_LEFT_ALT:
            case GLFW.GLFW_KEY_RIGHT_ALT:
            case GLFW.GLFW_KEY_F10:
                close();
                return "__close__";
        }

        int letter = letterFromKey(key);
        if (letter >= 0) {
            char ch = (char) letter;

            if (selectedMenu >= 0) {
                List<MenuItem> items = menus.get(selectedMenu).items();
                for (int i = 0; i < items.size(); i++) {
                    if (Character.toUpperCase(items.get(i).mnemonic()) == Character.toUpperCase(ch)) {
                        String action = items.get(i).action();
                        close();
                        return action;
                    }
                }
            }

            int topIdx = QBasicMenus.indexOfMnemonic(ch);
            if (topIdx >= 0) {
                selectedMenu = topIdx;
                selectedItem = -1;
                return null;
            }
        }

        return null;
    }

    /** Mouse: returns an action string or null. */
    public String handleClick(double mx, double my) {
        // Menu bar strip is at y = 0..CELL_H
        int col = (int)(mx / 8);
        int row = (int)(my / 16);

        if (row == 0) {
            // Clicking the top strip selects a menu by column.
            int c = 1; // leading space
            for (int i = 0; i < menus.size(); i++) {
                int len = menus.get(i).label().length() + 3; // label + spaces
                if (col >= c && col < c + len) {
                    selectedMenu = i;
                    selectedItem = -1;
                    active = true;
                    return null;
                }
                c += len;
            }
        }
        // Clicking inside an open dropdown selects an item.
        if (active && selectedMenu >= 0 && row >= 1) {
            int idx = row - 2;
            if (idx >= 0 && idx < menus.get(selectedMenu).items().size()) {
                String action = menus.get(selectedMenu).items().get(idx).action();
                close();
                return action;
            }
        }
        return null;
    }

    public void render(GuiGraphics g, QBasicApplication owner, int screenW) {
        if (!active) return;

        // Highlight the currently selected menu label in the top strip.
        if (selectedMenu >= 0) {
            int startCol = 1; // matches the leading space in the rendered menu text
            for (int i = 0; i < selectedMenu; i++) {
                startCol += menus.get(i).label().length() + 3;
            }
            int labelLen = menus.get(selectedMenu).label().length();
            int x1 = startCol * 8;
            int x2 = (startCol + labelLen) * 8;
            g.fill(x1, 0, x2, 16, DosPalette.BLUE);
            //owner.drawDos(g, menus.get(selectedMenu).label(), x1, 0, DosPalette.WHITE);
	    drawLabelWithMnemonic(owner, menus.get(selectedMenu).label(),
                    menus.get(selectedMenu).mnemonic(), g,
                    x1, 0, DosPalette.WHITE, DosPalette.YELLOW);
        }

        // Dropdown
        if (selectedMenu >= 0) {
            List<MenuItem> items = menus.get(selectedMenu).items();
            int maxLen = menus.get(selectedMenu).label().length();
            for (MenuItem it : items) maxLen = Math.max(maxLen, it.label().length() + 4);
            int w = maxLen + 4;
            int h = items.size() + 2;
            int x = 1 + menuOffset(selectedMenu) ;
            int y = 1;

            g.fill((x+1)*8, (y+1)*16, (x+w+1)*8, (y+h+1)*16, DosPalette.BLACK); // shadow
            g.fill(x*8, y*16, (x+w)*8, (y+h)*16, DosPalette.LIGHT_GRAY);

            for (int i = 0; i < items.size(); i++) {
                int iy = (y + 1 + i) * 16;
                boolean hot = (i == selectedItem);
                if (hot) g.fill(x*8, iy, (x+w)*8, iy + 16, DosPalette.BLUE);
                int fg    = hot ? DosPalette.WHITE  : DosPalette.BLACK;
                int mnem  = hot ? DosPalette.YELLOW : DosPalette.BLUE; // contrast
                drawLabelWithMnemonic(owner, items.get(i).label(), items.get(i).mnemonic(), g,
                        (x + 1) * 8, iy, fg, mnem);
            }
        }
    }

    /**
     * Draws `label` in `fgColor`, and re-draws the mnemonic character in
     * `mnemonicColor` with an underline so it's visually distinct.
     * This is the DOS convention (highlighted letter, sometimes underlined).
     */
    private void drawLabelWithMnemonic(QBasicApplication owner, String label, char mnemonic, GuiGraphics g, int x, int y, int fgColor, int mnemColor) {
        owner.drawDos(g, label, x, y, fgColor);

        int idx = -1;
        for (int i = 0; i < label.length(); i++) {
            if (Character.toUpperCase(label.charAt(i)) == Character.toUpperCase(mnemonic)) {
                idx = i;
                break;
            }
        }
        if (idx < 0) return;

        int cellX = x + idx * 8;
        // Redraw the single character in the mnemonic color.
        owner.drawDos(g, String.valueOf(label.charAt(idx)), cellX, y, mnemColor);

        // Underline: 1 px line across the bottom of the 8×16 cell.
        g.fill(cellX, y + 16 - 2, cellX + 8, y + 16 - 1, mnemColor);
    }

    private int menuOffset(int index) {
        int off = 1;
        for (int i = 0; i < index; i++) off += menus.get(i).label().length() + 3;
        return off;
    }

    private static int letterFromKey(int key) {
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) return key;
        return -1;
    }
}
