package com.adbtool;

import com.adbtool.android.AndroidCommandExecutor;
import com.adbtool.cli.CliException;
import com.adbtool.cli.CliParser;
import com.adbtool.cli.Command;
import com.adbtool.cli.CommandExecutor;
import com.adbtool.cli.Usage;

import java.io.PrintStream;

/** Device-side entry point for adbtool. */
public final class AdbTool {
    private final CliParser parser;
    private final CommandExecutor executor;

    public AdbTool(CliParser parser, CommandExecutor executor) {
        this.parser = parser;
        this.executor = executor;
    }

    public static void main(String[] args) {
        AdbTool application = new AdbTool(new CliParser(), AndroidCommandExecutor.createDefault());
        int exitCode = application.run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Command command = parser.parse(args);
            if (command.kind() == Command.Kind.HELP) {
                out.print(Usage.TEXT);
                return 0;
            }
            String result = executor.execute(command);
            if (result != null) {
                out.println(result);
            }
            return 0;
        } catch (CliException e) {
            err.println(e.getMessage());
            err.print(Usage.TEXT);
            return 2;
        } catch (Throwable t) {
            err.println("adbtool error: " + safeMessage(t));
            if ("1".equals(System.getenv("ADBTOOL_DEBUG"))) {
                t.printStackTrace(err);
            }
            return 1;
        }
    }

    private static String safeMessage(Throwable t) {
        String message = t.getMessage();
        return message == null || message.isEmpty() ? t.getClass().getSimpleName() : message;
    }
}
