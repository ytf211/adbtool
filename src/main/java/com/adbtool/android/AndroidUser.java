package com.adbtool.android;

final class AndroidUser {
    static int currentId() {
        try {
            Class<?> activityManager = Class.forName("android.app.ActivityManager");
            Object result = activityManager.getMethod("getCurrentUser").invoke(null);
            return ((Integer) result).intValue();
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private AndroidUser() {}
}
