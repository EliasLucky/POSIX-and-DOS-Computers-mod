public abstract class TextDisplayMode extends DisplayMode {
    public abstract void writeChar(int row, int col, char c, int fg, int bg);
    public abstract void setAttribute(int fg, int bg);
    public abstract void scrollUp(int lines);
}
