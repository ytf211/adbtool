package com.adbtool.cli;

public interface CommandExecutor {
    /** Returns a line to print, or null when the command has no output. */
    String execute(Command command) throws Exception;
}
