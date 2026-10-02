package com.shitianyaa.nagramx.videotimer.core;

import android.util.Log;

import io.github.libxposed.api.XposedInterface.ExceptionMode;
import io.github.libxposed.api.XposedInterface.HookHandle;
import io.github.libxposed.api.XposedInterface.Hooker;
import io.github.libxposed.api.XposedModule;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * libxposed API 102 统一工具层：反射、Hook 包装、句柄登记与热重载。
 */
public final class Xp {

    public static final String TAG = "NagramXVideoTimer";

    private static volatile XposedModule MODULE;

    private static final List<HookHandle> HOOKS =
            Collections.synchronizedList(new ArrayList<HookHandle>());

    private Xp() {
    }

    public static void bind(XposedModule module) {
        MODULE = module;
    }

    public static XposedModule module() {
        XposedModule m = MODULE;
        if (m == null) {
            throw new IllegalStateException("Xp 未绑定模块实例（入口回调开头应先调用 Xp.bind）");
        }
        return m;
    }

    // ------------------------------------------------------------------ 日志

    public static void log(String msg) {
        log(msg, null);
    }

    public static void log(String msg, Throwable t) {
        if (t == null) {
            Log.i(TAG, msg);
        } else {
            Log.w(TAG, msg, t);
        }
        try {
            if (t == null) {
                module().log(Log.INFO, TAG, msg);
            } else {
                module().log(Log.WARN, TAG, msg, t);
            }
        } catch (Throwable ignored) {
            if (t == null) {
                System.err.println(TAG + " " + msg);
            } else {
                System.err.println(TAG + " " + msg + ": " + t);
            }
        }
    }

    // ------------------------------------------------------------ 类 / 方法查找

    /**
     * 加载类但不初始化（initialize=false），与 legacy 保持一致。
     */
    public static Class<?> findClass(String name, ClassLoader cl) throws ClassNotFoundException {
        if (name == null) {
            throw new ClassNotFoundException("class name is null");
        }
        return Class.forName(name, false, cl);
    }

    public static Class<?> findClassIfExists(String name, ClassLoader cl) {
        try {
            return findClass(name, cl);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        if (cls == null || name == null) {
            return null;
        }
        Class<?>[] wanted = params == null ? new Class<?>[0] : params;
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(name, wanted);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    public static Constructor<?> findConstructor(Class<?> cls, Class<?>... params) {
        if (cls == null) {
            return null;
        }
        Class<?>[] wanted = params == null ? new Class<?>[0] : params;
        try {
            Constructor<?> ctor = cls.getDeclaredConstructor(wanted);
            ctor.setAccessible(true);
            return ctor;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Field findField(Class<?> cls, String name) {
        if (cls == null || name == null) {
            return null;
        }
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                return null;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ Hook

    public static HookHandle hook(Method method, Hooker hooker) {
        return hook(method, ExceptionMode.PROTECTIVE, hooker);
    }

    public static HookHandle hook(Method method, ExceptionMode exceptionMode, Hooker hooker) {
        if (method == null || hooker == null) {
            return null;
        }
        try {
            HookHandle handle = module().hook(method)
                    .setExceptionMode(exceptionMode)
                    .intercept(hooker);
            HOOKS.add(handle);
            return handle;
        } catch (Throwable t) {
            log("hook FAILED: " + method.getDeclaringClass().getSimpleName() + "#" + method.getName(), t);
            return null;
        }
    }

    public static HookHandle hook(Constructor<?> ctor, Hooker hooker) {
        return hook(ctor, ExceptionMode.PROTECTIVE, hooker);
    }

    public static HookHandle hook(Constructor<?> ctor, ExceptionMode exceptionMode, Hooker hooker) {
        if (ctor == null || hooker == null) {
            return null;
        }
        try {
            HookHandle handle = module().hook(ctor)
                    .setExceptionMode(exceptionMode)
                    .intercept(hooker);
            HOOKS.add(handle);
            return handle;
        } catch (Throwable t) {
            log("hook ctor FAILED: " + ctor.getDeclaringClass().getSimpleName(), t);
            return null;
        }
    }

    public static int hookCount() {
        return HOOKS.size();
    }

    public static void unhookAll() {
        synchronized (HOOKS) {
            for (HookHandle h : HOOKS) {
                if (h != null) {
                    try {
                        h.unhook();
                    } catch (Throwable t) {
                        log("unhook 异常: " + t);
                    }
                }
            }
            HOOKS.clear();
        }
    }

    // ------------------------------------------------------------------ 反射

    public static Object invoke(Object target, Method method, Object... args) {
        if (method == null) return null;
        try {
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Throwable t) {
            Throwable cause = (t instanceof InvocationTargetException)
                    ? ((InvocationTargetException) t).getTargetException()
                    : t;
            log("调用 " + method.getName() + " 失败: " + cause);
            return null;
        }
    }

    public static Object invokeStatic(Class<?> cls, String methodName, Object... args) {
        if (cls == null || methodName == null) return null;
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().equals(methodName) && m.getParameterTypes().length == args.length) {
                return invoke(null, m, args);
            }
        }
        return null;
    }

    public static Object getField(Object target, Field field) {
        if (field == null) return null;
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object getField(Object target, String fieldName) {
        if (target == null || fieldName == null) return null;
        Field f = findField(target.getClass(), fieldName);
        return getField(target, f);
    }

    public static boolean setField(Object target, Field field, Object value) {
        if (field == null) return false;
        try {
            field.setAccessible(true);
            field.set(target, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean setField(Object target, String fieldName, Object value) {
        if (target == null || fieldName == null) return false;
        Field f = findField(target.getClass(), fieldName);
        return setField(target, f, value);
    }

    public static Object getStaticField(Class<?> cls, String fieldName) {
        Field f = findField(cls, fieldName);
        return getField(null, f);
    }

    public static boolean setStaticField(Class<?> cls, String fieldName, Object value) {
        Field f = findField(cls, fieldName);
        return setField(null, f, value);
    }
}
