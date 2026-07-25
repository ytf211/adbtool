package com.adbtool.android;

import com.adbtool.TestSupport;
import com.adbtool.process.ProcessResult;
import com.adbtool.process.ProcessRunner;

import java.util.List;

public final class NotificationClientTest {
    public static void run() throws Exception {
        RecordingRunner runner = new RecordingRunner(new ProcessResult(0, ""));
        new NotificationClient(runner).post("title; echo bad", "$(bad)\nbody");
        TestSupport.equal("/system/bin/cmd", runner.command.get(0));
        TestSupport.equal("title; echo bad", runner.command.get(6));
        TestSupport.equal("$(bad)\nbody", runner.command.get(10));

        NotificationClient client = new NotificationClient(
            new RecordingRunner(new ProcessResult(7, "permission denied\n")));
        try {
            client.post("title", "body");
            throw new AssertionError("Expected command failure");
        } catch (IllegalStateException e) {
            TestSupport.isTrue(e.getMessage().contains("exit code 7"), "Expected exit code");
            TestSupport.isTrue(e.getMessage().contains("permission denied"), "Expected process output");
        }
    }

    private static final class RecordingRunner implements ProcessRunner {
        private final ProcessResult result;
        private List<String> command;

        private RecordingRunner(ProcessResult result) {
            this.result = result;
        }

        @Override
        public ProcessResult run(List<String> command) {
            this.command = command;
            return result;
        }
    }

    private NotificationClientTest() {}
}
