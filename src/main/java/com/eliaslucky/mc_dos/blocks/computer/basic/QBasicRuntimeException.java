package com.eliaslucky.mc_dos.blocks.computer.basic;

public class QBasicRuntimeException extends RuntimeException {
    public final int code;
    public final int sourceLine;

    public QBasicRuntimeException(int code, int sourceLine, String message) {
        super(message);
        this.code = code;
        this.sourceLine = sourceLine;
    }
}
