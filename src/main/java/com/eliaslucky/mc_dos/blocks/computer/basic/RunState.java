package com.eliaslucky.mc_dos.blocks.computer.basic;

public enum RunState {
    RUNNING,         // executing, more statements to process
    WAITING_INPUT,   // blocked on INPUT, needs a line
    WAITING_SLEEP,   // blocked on SLEEP, needs time to pass
    FINISHED         // program ended or aborted
}
