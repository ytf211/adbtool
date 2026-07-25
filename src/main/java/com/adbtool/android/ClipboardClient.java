package com.adbtool.android;

import android.content.ClipData;

import java.lang.reflect.Method;

final class ClipboardClient {
    private static final String SHELL_PACKAGE = "com.android.shell";

    void setText(String text) throws Exception {
        Object service = HiddenServices.interfaceFor("android.content.IClipboard", "clipboard");
        Method method = HiddenServices.method("android.content.IClipboard", "setPrimaryClip");
        method.invoke(service, arguments(method.getParameterTypes(), ClipData.newPlainText("adbtool", text), AndroidUser.currentId()));
    }

    String getText() throws Exception {
        Object service = HiddenServices.interfaceFor("android.content.IClipboard", "clipboard");
        Method method = HiddenServices.method("android.content.IClipboard", "getPrimaryClip");
        Object result = method.invoke(service, arguments(method.getParameterTypes(), null, AndroidUser.currentId()));
        if (result == null) return "";

        ClipData clip = (ClipData) result;
        if (clip.getItemCount() == 0) return "";
        CharSequence text = clip.getItemAt(0).getText();
        return text == null ? "" : text.toString();
    }

    static Object[] arguments(Class<?>[] types, ClipData clip, int userId) {
        Object[] arguments = new Object[types.length];
        boolean packageAssigned = false;
        boolean userAssigned = false;
        for (int index = 0; index < types.length; index++) {
            Class<?> type = types[index];
            if (type == ClipData.class) {
                arguments[index] = clip;
            } else if (type == String.class) {
                arguments[index] = packageAssigned ? null : SHELL_PACKAGE;
                packageAssigned = true;
            } else if (ReflectionCompat.isInteger(type)) {
                arguments[index] = userAssigned ? Integer.valueOf(0) : Integer.valueOf(userId);
                userAssigned = true;
            } else {
                throw new IllegalArgumentException("Unsupported clipboard parameter: " + type.getName());
            }
        }
        return arguments;
    }
}
