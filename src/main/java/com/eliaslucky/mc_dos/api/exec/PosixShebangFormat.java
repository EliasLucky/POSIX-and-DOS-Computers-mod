/** POSIX shebang — file starts with "#!"; interpreter follows. */
public final class PosixShebangFormat implements ExecutableFormat {
    public static final PosixShebangFormat INSTANCE = new PosixShebangFormat();
    private PosixShebangFormat() {}
    @Override public String name() { return "POSIX shebang"; }
    @Override
    public boolean matches(String fileName, VirtualFileSystem.Node node) {
        String c = node.content;
        return c != null && c.startsWith("#!");
    }
}
