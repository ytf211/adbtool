package com.adbtool.cli;

import com.adbtool.TestSupport;

public final class CliParserTest {
    public static void run() throws Exception {
        CliParser parser = new CliParser();

        Command setClip = parser.parse(new String[] {"set-clip", "hello", "世界\n"});
        TestSupport.equal(Command.Kind.SET_CLIP, setClip.kind());
        TestSupport.equal("hello 世界\n", setClip.primaryText());

        Command longToast = parser.parse(new String[] {"toast", "--long", "hello"});
        TestSupport.equal(Command.Kind.TOAST, longToast.kind());
        TestSupport.isTrue(longToast.longDuration(), "Expected a long Toast");

        Command shortToast = parser.parse(new String[] {"toast", "hello"});
        TestSupport.isTrue(!shortToast.longDuration(), "Expected a short Toast");

        Command notify = parser.parse(new String[] {"notify", "title"});
        TestSupport.equal("title", notify.primaryText());
        TestSupport.equal("", notify.secondaryText());

        assertError(parser, new String[] {"get-clip", "extra"}, "Usage: adbtool get-clip");
        assertError(parser, new String[] {"unknown"}, "Unknown command: unknown");
    }

    private static void assertError(CliParser parser, String[] args, String expected) throws Exception {
        try {
            parser.parse(args);
            throw new AssertionError("Expected CliException");
        } catch (CliException e) {
            TestSupport.equal(expected, e.getMessage());
        }
    }

    private CliParserTest() {}
}
