package com.adbtool.android;

import android.os.IBinder;
import com.adbtool.TestSupport;

import java.lang.reflect.Method;

public final class AndroidSignaturesTest {
    public static void run() throws Exception {
        Method api30 = ReflectionCompat.widestMethod(Api30Clipboard.class, "getPrimaryClip");
        TestSupport.equal(2, api30.getParameterTypes().length);

        Method api35 = ReflectionCompat.widestMethod(Api35Clipboard.class, "getPrimaryClip");
        TestSupport.equal(4, api35.getParameterTypes().length);

        Object[] clipboard = ClipboardClient.arguments(
            new Class<?>[] {String.class, String.class, Integer.TYPE, Integer.TYPE}, null, 12);
        TestSupport.equal("com.android.shell", clipboard[0]);
        TestSupport.isNull(clipboard[1]);
        TestSupport.equal(12, clipboard[2]);
        TestSupport.equal(0, clipboard[3]);

        Object[] api30Toast = ToastClient.arguments(
            new Class<?>[] {
                String.class, IBinder.class, CharSequence.class, Integer.TYPE,
                Integer.TYPE, ITransientNotificationCallback.class
            },
            "hello", null, 1);
        TestSupport.equal("com.android.shell", api30Toast[0]);
        TestSupport.isNull(api30Toast[1]);
        TestSupport.equal("hello", api30Toast[2]);
        TestSupport.equal(1, api30Toast[3]);
        TestSupport.equal(0, api30Toast[4]);
        TestSupport.isNull(api30Toast[5]);

        Method proxyMethod = ReflectionCompat.widestMethod(PublicClipboard.class, "getPrimaryClip");
        TestSupport.isNull(proxyMethod.invoke(new PrivateClipboardProxy(), "com.android.shell", 0));

        Object[] api35Toast = ToastClient.arguments(
            new Class<?>[] {
                String.class, IBinder.class, CharSequence.class, Integer.TYPE,
                Boolean.TYPE, Integer.TYPE, ITransientNotificationCallback.class
            },
            "hello", null, 0);
        TestSupport.equal(Boolean.FALSE, api35Toast[4]);
        TestSupport.equal(0, api35Toast[5]);
    }

    public static class Api30Clipboard {
        public Object getPrimaryClip(String packageName, int userId) {
            return null;
        }
    }

    public static class Api35Clipboard extends Api30Clipboard {
        public Object getPrimaryClip(String packageName, String attributionTag, int userId, int deviceId) {
            return null;
        }
    }

    public interface PublicClipboard {
        Object getPrimaryClip(String packageName, int userId);
    }

    private static final class PrivateClipboardProxy implements PublicClipboard {
        @Override
        public Object getPrimaryClip(String packageName, int userId) {
            return null;
        }
    }

    public interface ITransientNotificationCallback {}

    private AndroidSignaturesTest() {}
}
