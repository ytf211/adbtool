package com.adbtool.android;

import com.adbtool.process.ProcessResult;
import com.adbtool.process.ProcessRunner;

import java.util.ArrayList;
import java.util.List;

public final class NotificationClient {
    private final ProcessRunner processRunner;

    public NotificationClient(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    public void post(String title, String body) throws Exception {
        ProcessResult result = processRunner.run(command(title, body));
        if (result.exitCode() != 0) {
            String detail = result.output().trim();
            throw new IllegalStateException(
                "cmd notification post failed with exit code " + result.exitCode()
                    + (detail.isEmpty() ? "" : ": " + detail));
        }
    }

    static List<String> command(String title, String body) {
        List<String> command = new ArrayList<String>();
        command.add("/system/bin/cmd");
        command.add("notification");
        command.add("post");
        command.add("-S");
        command.add("bigtext");
        command.add("-t");
        command.add(title);
        command.add("-i");
        command.add("@android:drawable/ic_dialog_info");
        command.add("adbtool");
        command.add(body.isEmpty() ? title : body);
        return command;
    }
}
