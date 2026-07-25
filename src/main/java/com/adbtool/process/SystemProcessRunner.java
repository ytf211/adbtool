package com.adbtool.process;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class SystemProcessRunner implements ProcessRunner {
    @Override
    public ProcessResult run(List<String> command) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        byte[] output;
        try (InputStream input = process.getInputStream()) {
            output = readAll(input);
        }
        int exitCode = process.waitFor();
        return new ProcessResult(exitCode, new String(output, StandardCharsets.UTF_8));
    }

    private static byte[] readAll(InputStream input) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
