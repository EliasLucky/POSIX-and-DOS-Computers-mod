/**
 * Bash. A superset of POSIX sh with GNU extensions:
 *   &&, ||, !        (logical operators)
 *   [[ ... ]]        (extended test)
 *   {a,b,c}          (brace expansion)
 *   $RANDOM, $SECONDS
 *   arrays, associative arrays
 *   <<< here-strings
 *   process substitution <(...), >(...)
 */
public final class BashDialect extends ShellDialect {
    public static final BashDialect INSTANCE = new BashDialect();
    private BashDialect() {}

    @Override public String name()              { return "bash"; }
    @Override public boolean supportsPipes()    { return true; }
    @Override public boolean supportsBackground() { return true; }
    @Override public boolean supportsAppend()     { return true; }
    @Override public boolean supportsPositional() { return true; }
    @Override public boolean supportsVariables()  { return true; }
    @Override public boolean supportsControlFlow() { return true; }
    @Override public boolean supportsFunctions()   { return true; }
    @Override public boolean supportsCommandSubst() { return true; }
    @Override public boolean supportsArithmetic()   { return true; }
    @Override public boolean supportsHereDoc()      { return true; }
    @Override public boolean supportsParamExpansion() { return true; }
    @Override public boolean supportsLogicalOps()   { return true; }
    @Override public boolean supportsBraceExpand()  { return true; }
    @Override public boolean supportsExtendedTest() { return true; }
}
