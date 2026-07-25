package com.adbtool;

public final class TestSupport {
    public static void equal(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected <" + expected + "> but got <" + actual + ">");
        }
    }

    public static void isTrue(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void isNull(Object value) {
        if (value != null) throw new AssertionError("Expected null but got <" + value + ">");
    }

    private TestSupport() {}
}
