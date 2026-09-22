package com.eliaslucky.mc_dos.client.interpreter.qbasic;

public interface Statement {
    int line();
    void execute(ExecutionContext ctx, Host host);
}
