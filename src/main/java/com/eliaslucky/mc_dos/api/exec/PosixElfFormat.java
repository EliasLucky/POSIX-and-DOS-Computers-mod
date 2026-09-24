/** POSIX ELF binary — matched by magic, but never actually run by us. */
public final class PosixElfFormat implements ExecutableFormat {
    public static final PosixElfFormat INSTANCE = new PosixElfFormat();
    private PosixElfFormat() {}
    @Override public String name() { return "POSIX ELF"; }
    @Override
    public boolean matches(String fileName, VirtualFileSystem.Node node) {
        String c = node.content;
        return c != null && c.startsWith("\u007fELF");
    }
}
