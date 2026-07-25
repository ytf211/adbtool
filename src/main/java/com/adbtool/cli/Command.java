package com.adbtool.cli;

/** Parsed adbtool command. */
public final class Command {
    public enum Kind {
        SET_CLIP,
        GET_CLIP,
        TOAST,
        NOTIFY,
        HELP
    }

    private final Kind kind;
    private final String primaryText;
    private final String secondaryText;
    private final boolean longDuration;

    private Command(Kind kind, String primaryText, String secondaryText, boolean longDuration) {
        this.kind = kind;
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.longDuration = longDuration;
    }

    public static Command setClip(String text) {
        return new Command(Kind.SET_CLIP, text, null, false);
    }

    public static Command getClip() {
        return new Command(Kind.GET_CLIP, null, null, false);
    }

    public static Command toast(String text, boolean longDuration) {
        return new Command(Kind.TOAST, text, null, longDuration);
    }

    public static Command notify(String title, String body) {
        return new Command(Kind.NOTIFY, title, body, false);
    }

    public static Command help() {
        return new Command(Kind.HELP, null, null, false);
    }

    public Kind kind() {
        return kind;
    }

    public String primaryText() {
        return primaryText;
    }

    public String secondaryText() {
        return secondaryText;
    }

    public boolean longDuration() {
        return longDuration;
    }
}
