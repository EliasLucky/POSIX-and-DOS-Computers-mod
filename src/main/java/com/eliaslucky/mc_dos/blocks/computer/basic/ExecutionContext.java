package com.eliaslucky.mc_dos.blocks.computer.basic;

import java.util.*;

public final class ExecutionContext {
    public record ForFrame(String var, double end, double step, int bodyStartPc) {}

    private final List<Statement> program;
    private final Map<String, Integer> labels = new HashMap<>();
    private final Map<String, Value>   vars   = new HashMap<>();
    private final Deque<ForFrame>      forStack    = new ArrayDeque<>();
    private final Deque<Integer>       returnStack = new ArrayDeque<>();

    private int pc = 0;
    private int printColumn = 0;
    private boolean stopped = false;

    public ExecutionContext(List<Statement> program) {
        this.program = program;
        for (int i = 0; i < program.size(); i++) {
            labels.put(String.valueOf(program.get(i).line()), i);
        }
    }

    // Program control
    public int getPc() { return pc; }
    public void setPc(int v) { pc = v; }
    public void advance() { pc++; }
    public boolean hasNext() { return !stopped && pc >= 0 && pc < program.size(); }
    public Statement next() { return program.get(pc); }
    public void jumpTo(int index) { this.pc = index; }
    public Integer labelToPc(String label) { return labels.get(label); }
    public void stop() { stopped = true; }

    // Variables
    public Value getVar(String name) {
        return vars.getOrDefault(normalize(name), Value.of(0));
    }
    public void setVar(String name, Value v) { vars.put(normalize(name), v); }

    // FOR stack
    public void pushFor(ForFrame f) { forStack.push(f); }
    public ForFrame peekFor()       { return forStack.peek(); }
    public void popFor()            { forStack.pop(); }

    // GOSUB return stack
    public void pushReturn(int pc) { returnStack.push(pc); }
    public Integer popReturn()     { return returnStack.poll(); }

    // PRINT column
    public int  getPrintColumn()      { return printColumn; }
    public void setPrintColumn(int c) { printColumn = c; }
    
    // INPUT
    private boolean inputRequested = false;
    private String  inputValue     = null;

    public boolean hasInputRequest() { return inputRequested; }
    public void    requestInput()    { inputRequested = true; }
    public boolean hasInputValue()   { return inputValue != null; }
    public String  consumeInput()    { String s = inputValue; inputValue = null; return s; }
    public void    provideInput(String s) {
        this.inputValue = s;
        this.inputRequested = false;
    }

    // SLEEP
    private long sleepUntilMillis = 0;

    public boolean hasPendingSleep() { return sleepUntilMillis > 0; }
    public long    getSleepUntil()   { return sleepUntilMillis; }
    public void    requestSleep(long until) { sleepUntilMillis = until; }
    public void    clearSleep()      { sleepUntilMillis = 0; }

    private static String normalize(String name) {
        String n = name.trim().toUpperCase(Locale.ROOT);
        while (!n.isEmpty()) {
            char c = n.charAt(n.length() - 1);
            if (c == '$' || c == '%' || c == '!' || c == '#' || c == '&') {
                n = n.substring(0, n.length() - 1);
            } else break;
        }
        return n;
    }
}
