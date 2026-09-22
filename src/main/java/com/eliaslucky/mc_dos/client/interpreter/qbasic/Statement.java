public interface Statement {
    int line();
    void execute(ExecutionContext ctx, Host host);
}
