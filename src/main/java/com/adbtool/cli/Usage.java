package com.adbtool.cli;

public final class Usage {
    public static final String TEXT =
        "adbtool - Android clipboard, Toast, and notification CLI\n" +
        "\n" +
        "Usage: adbtool <command> [arguments]\n" +
        "\n" +
        "Commands:\n" +
        "  set-clip <text>            Set clipboard text\n" +
        "  get-clip                   Print clipboard text\n" +
        "  toast [-l|--long] <text>   Show a Toast\n" +
        "  notify <title> [body]      Post or update a notification\n" +
        "  help                       Show this help\n";

    private Usage() {}
}
