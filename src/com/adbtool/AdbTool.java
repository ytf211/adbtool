package com.adbtool;

import android.content.ClipData;
import android.os.Binder;
import android.os.IBinder;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * AdbTool —— 通过 app_process 在设备上运行,借助反射调用 Android 隐藏系统服务,
 * 实现四项能力:设置剪贴板 / 读取剪贴板 / 弹出 Toast / 发送通知。
 *
 * 运行方式(设备端):
 *   CLASSPATH=/data/local/tmp/adbtool.jar app_process /system/bin com.adbtool.AdbTool <subcommand> [args]
 *
 * 之所以走 app_process:剪贴板(IClipboard)与 Toast(INotificationManager)
 * 没有现成 shell 命令,必须以进程身份直接调用系统服务。
 */
public final class AdbTool {

    /** 呈现给系统服务的调用包名。必须属于 shell UID(2000),否则 verifyPackage 失败。 */
    private static final String PKG = "com.android.shell";

    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                usage();
                System.exit(2);
                return;
            }
            String cmd = args[0];
            switch (cmd) {
                case "set-clip": {
                    if (args.length < 2) fail("用法: set-clip <文本>");
                    setClip(joinFrom(args, 1));
                    break;
                }
                case "get-clip": {
                    // 直接把剪贴板文本原样输出到 stdout(带换行,便于 shell 取值)
                    System.out.println(getClip());
                    break;
                }
                case "toast": {
                    if (args.length < 2) fail("用法: toast [-l|--long] <文本>");
                    boolean lng = false;
                    int start = 1;
                    if ("-l".equals(args[1]) || "--long".equals(args[1])) {
                        lng = true;
                        start = 2;
                    }
                    if (args.length <= start) fail("用法: toast [-l|--long] <文本>");
                    toast(joinFrom(args, start), lng);
                    break;
                }
                case "notify": {
                    if (args.length < 2) fail("用法: notify <标题> [正文]");
                    String title = args[1];
                    String body = args.length > 2 ? joinFrom(args, 2) : "";
                    notify(title, body);
                    break;
                }
                case "help":
                case "-h":
                case "--help":
                    usage();
                    break;
                default:
                    System.err.println("未知子命令: " + cmd);
                    usage();
                    System.exit(2);
            }
        } catch (Throwable t) {
            System.err.println("adbtool 错误: " + t.getMessage());
            t.printStackTrace();
            System.exit(1);
        }
    }

    // ---------------------------------------------------------------------
    // 剪贴板
    // ---------------------------------------------------------------------

    private static void setClip(String text) throws Exception {
        Object clipboard = clipboardService();
        ClipData clip = ClipData.newPlainText("adbtool", text);
        Method m = pickMethod(clipboard.getClass(), "setPrimaryClip");
        Object[] a = clipArgs(m.getParameterTypes(), clip);
        m.invoke(clipboard, a);
    }

    private static String getClip() throws Exception {
        Object clipboard = clipboardService();
        Method m = pickMethod(clipboard.getClass(), "getPrimaryClip");
        Object[] a = clipArgs(m.getParameterTypes(), null);
        Object res = m.invoke(clipboard, a);
        if (res == null) return "";
        ClipData clip = (ClipData) res;
        if (clip.getItemCount() == 0) return "";
        CharSequence cs = clip.getItemAt(0).getText();
        return cs == null ? "" : cs.toString();
    }

    /**
     * 为 IClipboard.{get,set}PrimaryClip 按参数类型装配实参,兼容多版本签名。
     * API 35 形态:setPrimaryClip(ClipData, String pkg, String attributionTag, int userId, int deviceId)
     *             getPrimaryClip(String pkg, String attributionTag, int userId, int deviceId)
     */
    private static Object[] clipArgs(Class<?>[] types, ClipData clip) {
        Object[] a = new Object[types.length];
        boolean pkgAssigned = false;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == ClipData.class) {
                a[i] = clip;
            } else if (t == String.class) {
                a[i] = pkgAssigned ? null : PKG;   // 第一个 String 是调用包名,其后(attributionTag)置 null
                pkgAssigned = true;
            } else if (t == int.class || t == Integer.class) {
                a[i] = 0;                            // userId / deviceId 均用默认 0
            } else {
                a[i] = null;                         // 其他未知类型给 null,尽力而为
            }
        }
        return a;
    }

    private static Object clipboardService() throws Exception {
        return asInterface("android.content.IClipboard", getService("clipboard"));
    }

    // ---------------------------------------------------------------------
    // Toast
    // ---------------------------------------------------------------------

    private static void toast(String text, boolean lng) throws Exception {
        Object nm = notificationService();
        Method m = pickMethod(nm.getClass(), "enqueueTextToast");
        IBinder token = new Binder();
        int duration = lng ? 1 : 0;                 // Toast.LENGTH_LONG=1, LENGTH_SHORT=0
        Object[] a = toastArgs(m.getParameterTypes(), text, token, duration);
        m.invoke(nm, a);
        // Toast 由 SystemUI 渲染;若本进程立即退出,token 的 binder 死亡可能导致 toast 被取消,
        // 故保持存活到 toast 大致展示完毕。
        Thread.sleep(lng ? 3800L : 2300L);
    }

    /**
     * 装配 INotificationManager.enqueueTextToast 实参,兼容多版本签名。
     * API 35 形态:enqueueTextToast(String pkg, IBinder token, CharSequence text,
     *                              int duration, boolean isUiContext, int displayId,
     *                              ITransientNotificationCallback callback)
     */
    private static Object[] toastArgs(Class<?>[] types, String text, IBinder token, int duration) {
        Object[] a = new Object[types.length];
        boolean firstIntAssigned = false;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == String.class) {
                a[i] = PKG;
            } else if (t == IBinder.class) {
                a[i] = token;
            } else if (t == CharSequence.class) {
                a[i] = text;
            } else if (t == int.class || t == Integer.class) {
                a[i] = firstIntAssigned ? 0 : duration;   // 第一个 int 是 duration,其后(displayId)为默认屏 0
                firstIntAssigned = true;
            } else if (t == boolean.class || t == Boolean.class) {
                a[i] = Boolean.FALSE;                      // isUiContext=false → 使用 displayId 指定的默认屏
            } else {
                a[i] = null;                               // ITransientNotificationCallback 等 → null
            }
        }
        return a;
    }

    private static Object notificationService() throws Exception {
        return asInterface("android.app.INotificationManager", getService("notification"));
    }

    // ---------------------------------------------------------------------
    // 通知(复用系统内建的 `cmd notification post`,其内部已处理通道创建等细节)
    // ---------------------------------------------------------------------

    private static void notify(String title, String body) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add("/system/bin/cmd");
        cmd.add("notification");
        cmd.add("post");
        cmd.add("-S");
        cmd.add("bigtext");
        cmd.add("-t");
        cmd.add(title);
        cmd.add("-i");
        cmd.add("@android:drawable/ic_dialog_info");
        cmd.add("adbtool");                 // tag:固定值,同一 tag 的通知会就地更新
        cmd.add(body.isEmpty() ? title : body);

        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        byte[] out = readAll(p.getInputStream());
        int code = p.waitFor();
        if (code != 0) {
            System.err.write(out);
            throw new RuntimeException("cmd notification post 退出码 " + code);
        }
    }

    // ---------------------------------------------------------------------
    // 反射辅助
    // ---------------------------------------------------------------------

    private static IBinder getService(String name) throws Exception {
        Class<?> sm = Class.forName("android.os.ServiceManager");
        IBinder b = (IBinder) sm.getMethod("getService", String.class).invoke(null, name);
        if (b == null) throw new IllegalStateException("系统服务不存在: " + name);
        return b;
    }

    private static Object asInterface(String iface, IBinder binder) throws Exception {
        Class<?> stub = Class.forName(iface + "$Stub");
        Object o = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
        if (o == null) throw new IllegalStateException("无法获取接口: " + iface);
        return o;
    }

    /** 选取同名重载中参数最多的一个 —— 通常即最新 API 的完整签名。 */
    private static Method pickMethod(Class<?> c, String name) throws NoSuchMethodException {
        Method best = null;
        for (Method m : c.getMethods()) {
            if (m.getName().equals(name)) {
                if (best == null || m.getParameterCount() > best.getParameterCount()) {
                    best = m;
                }
            }
        }
        if (best == null) {
            throw new NoSuchMethodException(c.getName() + "#" + name);
        }
        return best;
    }

    // ---------------------------------------------------------------------
    // 杂项
    // ---------------------------------------------------------------------

    private static String joinFrom(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) bos.write(buf, 0, n);
        return bos.toByteArray();
    }

    private static void fail(String msg) {
        System.err.println(msg);
        System.exit(2);
    }

    private static void usage() {
        System.out.println(
            "adbtool —— Android 剪贴板/Toast/通知 命令行工具\n" +
            "\n" +
            "用法: adbtool <子命令> [参数]\n" +
            "\n" +
            "子命令:\n" +
            "  set-clip <文本>            设置剪贴板内容\n" +
            "  get-clip                   读取并打印剪贴板文本\n" +
            "  toast [-l|--long] <文本>   弹出 Toast(-l 为长时)\n" +
            "  notify <标题> [正文]       发送通知\n" +
            "  help                       显示本帮助\n");
    }

    private AdbTool() {}
}
