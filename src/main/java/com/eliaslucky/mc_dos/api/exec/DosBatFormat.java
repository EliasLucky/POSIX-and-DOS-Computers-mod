/** MS-DOS .BAT — text file of shell commands. */
public final class DosBatFormat implements ExecutableFormat {
    public static final DosBatFormat INSTANCE = new DosBatFormat();
    private DosBatFormat() {}
    @Override public String name() { return "MS-DOS batch"; }
    @Override
    public boolean matches(String fileName, VirtualFileSystem.Node node) {
        return fileName.toUpperCase().endsWith(".BAT");
    }
}
