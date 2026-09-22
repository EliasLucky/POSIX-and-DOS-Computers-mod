public class DialogState {
    public record Item(String label, String action) {}
    private String title;
    private List<String> staticLines = new ArrayList<>();
    private List<Item> items = new ArrayList<>();
    private int selected = 0;
    private boolean shadow = true;

    public static DialogState welcome() {
        var d = new DialogState();
        d.staticLines = List.of(
            "",
            "Welcome to MS-DOS QBasic",
            "",
            "Copyright (C) Microsoft Corporation, 1987-1992.",
            "All rights reserved.",
            ""
        );
        d.items = List.of(
            new Item("Press Enter to see the Survival Guide", "help.survival"),
            new Item("Press ESC to clear this dialog box",    "dialog.close")
        );
        return d;
    }

    public void render(GuiGraphics g, int cols, int rows, Style font) {
        // Center it. Gray fill, black text, single-line box, drop shadow.
        int w = maxContentWidth() + 4;
        int h = staticLines.size() + items.size() + 2;
        int x = (cols - w) / 2;
        int y = (rows - h) / 2;

        // Shadow
        g.fill((x+1)*CHAR_W, (y+1)*CHAR_H, (x+w+1)*CHAR_W, (y+h+1)*CHAR_H, DosPalette.BLACK);
        // Body
        g.fill(x*CHAR_W, y*CHAR_H, (x+w)*CHAR_W, (y+h)*CHAR_H, DosPalette.LIGHT_GRAY);

        // Border and content
        drawBox(g, x, y, w, h, font, DosPalette.BLACK);
        // ... draw staticLines, then items with `< … >` around the selected one
    }
}
