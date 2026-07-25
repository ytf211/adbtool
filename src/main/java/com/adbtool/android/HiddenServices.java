package com.adbtool.android;

import android.os.IBinder;

final class HiddenServices {
    static Object interfaceFor(String interfaceName, String serviceName) throws Exception {
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        IBinder binder = (IBinder) serviceManager.getMethod("getService", String.class)
            .invoke(null, serviceName);
        if (binder == null) {
            throw new IllegalStateException("Android service is unavailable: " + serviceName);
        }

        Class<?> stub = Class.forName(interfaceName + "$Stub");
        Object service = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
        if (service == null) {
            throw new IllegalStateException("Cannot obtain Android interface: " + interfaceName);
        }
        return service;
    }

    static java.lang.reflect.Method method(String interfaceName, String methodName) throws Exception {
        return ReflectionCompat.widestMethod(Class.forName(interfaceName), methodName);
    }

    private HiddenServices() {}
}
