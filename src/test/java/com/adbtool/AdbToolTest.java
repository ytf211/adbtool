package com.adbtool;

import com.adbtool.cli.CliParser;
import com.adbtool.cli.Command;
import com.adbtool.cli.CommandExecutor;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class AdbToolTest {
    public static void run() throws Exception {
        RecordingExecutor executor = new RecordingExecutor();
        Result help = runApplication(executor, "help");
        TestSupport.equal(0, help.exitCode);
        TestSupport.isTrue(help.stdout.contains("Usage:"), "Expected help output");
        TestSupport.isNull(executor.command);

        Result parseError = runApplication(new RecordingExecutor(), "get-clip", "extra");
        TestSupport.equal(2, parseError.exitCode);
        TestSupport.isTrue(parseError.stderr.contains("Usage: adbtool get-clip"), "Expected parse error");

        CommandExecutor failing = new CommandExecutor() {
            @Override
            public String execute(Command command) {
                throw new IllegalStateException("failed safely");
            }
        };
        Result failure = runApplication(failing, "get-clip");
        TestSupport.equal(1, failure.exitCode);
        TestSupport.equal("adbtool error: failed safely\n", failure.stderr);
    }

    private static Result runApplication(CommandExecutor executor, String... args) throws Exception {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        AdbTool application = new AdbTool(new CliParser(), executor);
        int code = application.run(args, new PrintStream(stdout, true, "UTF-8"), new PrintStream(stderr, true, "UTF-8"));
        return new Result(
            code,
            new String(stdout.toByteArray(), StandardCharsets.UTF_8),
            new String(stderr.toByteArray(), StandardCharsets.UTF_8));
    }

    private static final class RecordingExecutor implements CommandExecutor {
        private Command command;

        @Override
        public String execute(Command command) {
            this.command = command;
            return null;
        }
    }

    private static final class Result {
        private final int exitCode;
        private final String stdout;
        private final String stderr;

        private Result(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }
    }

    private AdbToolTest() {}
}
