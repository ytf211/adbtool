package com.adbtool.android;

import android.os.Binder;
import android.os.IBinder;

import java.lang.reflect.Method;

final class ToastClient {
    private static final String SHELL_PACKAGE = "com.android.shell";

    void show(String text, boolean longDuration) throws Exception {
        Object service = HiddenServices.interfaceFor("android.app.INotificationManager", "notification");
        Method method = HiddenServices.method("android.app.INotificationManager", "enqueueTextToast");
        int duration = longDuration ? 1 : 0;
        Object result = method.invoke(service, arguments(method.getParameterTypes(), text, new Binder(), duration));
        if (Boolean.FALSE.equals(result)) {
            throw new IllegalStateException("Android rejected the Toast request");
        }
        Thread.sleep(longDuration ? 3800L : 2300L);
    }

    static Object[] arguments(Class<?>[] types, String text, IBinder token, int duration) {
        Object[] arguments = new Object[types.length];
        boolean durationAssigned = false;
        for (int index = 0; index < types.length; index++) {
            Class<?> type = types[index];
            if (type == String.class) {
                arguments[index] = SHELL_PACKAGE;
            } else if (type == IBinder.class) {
                arguments[index] = token;
            } else if (type == CharSequence.class) {
                arguments[index] = text;
            } else if (ReflectionCompat.isInteger(type)) {
                arguments[index] = durationAssigned ? Integer.valueOf(0) : Integer.valueOf(duration);
                durationAssigned = true;
            } else if (ReflectionCompat.isBoolean(type)) {
                arguments[index] = Boolean.FALSE;
            } else if (type.getName().endsWith("ITransientNotificationCallback")) {
                arguments[index] = null;
            } else {
                throw new IllegalArgumentException("Unsupported Toast parameter: " + type.getName());
            }
        }
        return arguments;
    }
}
