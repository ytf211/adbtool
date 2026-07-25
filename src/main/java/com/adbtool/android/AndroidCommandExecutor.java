package com.adbtool.android;

import com.adbtool.cli.Command;
import com.adbtool.cli.CommandExecutor;
import com.adbtool.process.SystemProcessRunner;

public final class AndroidCommandExecutor implements CommandExecutor {
    private final ClipboardClient clipboard;
    private final ToastClient toast;
    private final NotificationClient notification;

    private AndroidCommandExecutor(
        ClipboardClient clipboard,
        ToastClient toast,
        NotificationClient notification
    ) {
        this.clipboard = clipboard;
        this.toast = toast;
        this.notification = notification;
    }

    public static AndroidCommandExecutor createDefault() {
        return new AndroidCommandExecutor(
            new ClipboardClient(),
            new ToastClient(),
            new NotificationClient(new SystemProcessRunner()));
    }

    @Override
    public String execute(Command command) throws Exception {
        switch (command.kind()) {
            case SET_CLIP:
                clipboard.setText(command.primaryText());
                return null;
            case GET_CLIP:
                return clipboard.getText();
            case TOAST:
                toast.show(command.primaryText(), command.longDuration());
                return null;
            case NOTIFY:
                notification.post(command.primaryText(), command.secondaryText());
                return null;
            case HELP:
                return null;
            default:
                throw new IllegalArgumentException("Unsupported command: " + command.kind());
        }
    }
}
