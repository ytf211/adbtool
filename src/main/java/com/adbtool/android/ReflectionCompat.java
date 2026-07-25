package com.adbtool.android;

import java.lang.reflect.Method;

final class ReflectionCompat {
    static Method widestMethod(Class<?> type, String name) throws NoSuchMethodException {
        Method selected = null;
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name)
                && (selected == null || method.getParameterTypes().length > selected.getParameterTypes().length)) {
                selected = method;
            }
        }
        if (selected == null) {
            throw new NoSuchMethodException(type.getName() + "#" + name);
        }
        return selected;
    }

    static boolean isInteger(Class<?> type) {
        return type == Integer.TYPE || type == Integer.class;
    }

    static boolean isBoolean(Class<?> type) {
        return type == Boolean.TYPE || type == Boolean.class;
    }

    private ReflectionCompat() {}
}
