package com.adbtool.cli;

public final class CliParser {
    public Command parse(String[] args) throws CliException {
        if (args == null || args.length == 0) {
            throw new CliException("Missing command");
        }

        String name = args[0];
        if ("help".equals(name) || "-h".equals(name) || "--help".equals(name)) {
            requireCount(args, 1, "Usage: adbtool help");
            return Command.help();
        }
        if ("set-clip".equals(name)) {
            requireMinimum(args, 2, "Usage: adbtool set-clip <text>");
            return Command.setClip(join(args, 1));
        }
        if ("get-clip".equals(name)) {
            requireCount(args, 1, "Usage: adbtool get-clip");
            return Command.getClip();
        }
        if ("toast".equals(name)) {
            return parseToast(args);
        }
        if ("notify".equals(name)) {
            requireMinimum(args, 2, "Usage: adbtool notify <title> [body]");
            return Command.notify(args[1], args.length > 2 ? join(args, 2) : "");
        }
        throw new CliException("Unknown command: " + name);
    }

    private static Command parseToast(String[] args) throws CliException {
        requireMinimum(args, 2, "Usage: adbtool toast [-l|--long] <text>");
        boolean longDuration = "-l".equals(args[1]) || "--long".equals(args[1]);
        int textIndex = longDuration ? 2 : 1;
        requireMinimum(args, textIndex + 1, "Usage: adbtool toast [-l|--long] <text>");
        return Command.toast(join(args, textIndex), longDuration);
    }

    private static void requireMinimum(String[] args, int count, String message) throws CliException {
        if (args.length < count) {
            throw new CliException(message);
        }
    }

    private static void requireCount(String[] args, int count, String message) throws CliException {
        if (args.length != count) {
            throw new CliException(message);
        }
    }

    private static String join(String[] args, int first) {
        StringBuilder result = new StringBuilder();
        for (int i = first; i < args.length; i++) {
            if (i > first) result.append(' ');
            result.append(args[i]);
        }
        return result.toString();
    }
}
