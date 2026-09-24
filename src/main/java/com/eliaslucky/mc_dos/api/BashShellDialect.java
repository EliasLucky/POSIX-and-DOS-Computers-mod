public class BashDialect extends ShellParser implements ShellDialect {
    @Override public boolean supportsLogicalOps() { return true; }
    @Override public boolean supportsModernSubstitution() { return true; }
    @Override public boolean supportsFdRedirection() { return true; }
}
