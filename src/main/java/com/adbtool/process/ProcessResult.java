package com.adbtool.process;

public final class ProcessResult {
    private final int exitCode;
    private final String output;

    public ProcessResult(int exitCode, String output) {
        this.exitCode = exitCode;
        this.output = output;
    }

    public int exitCode() {
        return exitCode;
    }

    public String output() {
        return output;
    }
}
