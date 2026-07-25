package com.adbtool;

import com.adbtool.android.AndroidSignaturesTest;
import com.adbtool.android.NotificationClientTest;
import com.adbtool.cli.CliParserTest;

public final class TestSuite {
    public static void main(String[] args) throws Exception {
        CliParserTest.run();
        AdbToolTest.run();
        NotificationClientTest.run();
        AndroidSignaturesTest.run();
        System.out.println("All JVM tests passed");
    }

    private TestSuite() {}
}
