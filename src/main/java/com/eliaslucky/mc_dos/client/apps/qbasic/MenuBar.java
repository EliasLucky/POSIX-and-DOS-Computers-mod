public class MenuBar {
    private final List<Menu> menus;
    private boolean active;
    private int selectedMenu = -1;
    private int selectedItem = -1;

    public String currentFooterHelp() {
        if (!active) return defaultHelp();
        if (selectedItem < 0) return "Use arrow keys to select a menu";
        return menus.get(selectedMenu).items().get(selectedItem).footerHelp();
    }

    // Called by the app; returns an action string or null.
    public String handleKey(int key) { /* ... */ return null; }
    public String handleClick(double mx, double my) { /* ... */ return null; }
}
