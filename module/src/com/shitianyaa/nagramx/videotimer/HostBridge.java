package com.shitianyaa.nagramx.videotimer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 宿主对象与类反射调用边界。
 */
final class HostBridge {

    public interface MethodPredicate {
        boolean test(Method method);
    }

    public interface Logger {
        void log(String message, Throwable throwable);
    }

    private final ClassLoader classLoader;
    private final Logger logger;

    HostBridge(ClassLoader classLoader, Logger logger) {
        this.classLoader = classLoader;
        this.logger = logger != null ? logger : (msg, t) -> {};
    }

    Class<?> loadClass(String name) {
        try {
            return Class.forName(name, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Throwable t) {
            logger.log("加载类 " + name + " 失败", t);
            return null;
        }
    }

    Method findMethod(Class<?> type, String name, int parameterCount, MethodPredicate predicate) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Method[] methods;
            try {
                methods = current.getDeclaredMethods();
            } catch (Throwable t) {
                logger.log("枚举 " + current.getName() + "." + name + " 方法失败", t);
                continue;
            }
            for (Method m : methods) {
                if (m.getName().equals(name) &&
                        (parameterCount < 0 || m.getParameterTypes().length == parameterCount) &&
                        (predicate == null || predicate.test(m))) {
                    makeAccessible(m);
                    return m;
                }
            }
        }
        return null;
    }

    Method findMethod(Class<?> type, String name, int parameterCount) {
        return findMethod(type, name, parameterCount, null);
    }

    Method findMethod(Class<?> type, String name) {
        return findMethod(type, name, 0, null);
    }

    Method findMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        int count = parameterTypes != null ? parameterTypes.length : 0;
        return findMethod(type, name, count, m -> {
            Class<?>[] types = m.getParameterTypes();
            if (types.length != count) return false;
            for (int i = 0; i < count; i++) {
                if (!types[i].equals(parameterTypes[i])) return false;
            }
            return true;
        });
    }

    Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field f = current.getDeclaredField(name);
                makeAccessible(f);
                return f;
            } catch (NoSuchFieldException ignored) {
            } catch (Throwable t) {
                logger.log("查找 " + current.getName() + "." + name + " 字段失败", t);
                return null;
            }
        }
        return null;
    }

    Object newInstance(Class<?> type, Object... args) {
        if (type == null) return null;
        Constructor<?> constructor = findConstructor(type, args);
        if (constructor == null) return null;
        try {
            makeAccessible(constructor);
            return constructor.newInstance(args);
        } catch (Throwable t) {
            Throwable cause = (t instanceof InvocationTargetException)
                    ? ((InvocationTargetException) t).getTargetException()
                    : t;
            logger.log("实例化 " + type.getName() + " 失败", cause);
            return null;
        }
    }

    Object newInstance(String className, Object... args) {
        return newInstance(loadClass(className), args);
    }

    boolean setStaticField(Class<?> type, String name, Object value) {
        Field field = findField(type, name);
        if (field == null) return false;
        try {
            field.set(null, value);
            return true;
        } catch (Throwable t) {
            logger.log("写入静态字段 " + field.getDeclaringClass().getName() + "." + field.getName() + " 失败", t);
            return false;
        }
    }

    Object getField(Object instance, String name) {
        if (instance == null) return null;
        Field field = findField(instance.getClass(), name);
        return getField(instance, field);
    }

    Object getField(Object instance, Field field) {
        if (instance == null || field == null) return null;
        try {
            return field.get(instance);
        } catch (Throwable t) {
            logger.log("读取字段 " + field.getDeclaringClass().getName() + "." + field.getName() + " 失败", t);
            return null;
        }
    }

    boolean setField(Object instance, String name, Object value) {
        if (instance == null) return false;
        Field field = findField(instance.getClass(), name);
        return setField(instance, field, value);
    }

    boolean setField(Object instance, Field field, Object value) {
        if (instance == null || field == null) return false;
        try {
            field.set(instance, value);
            return true;
        } catch (Throwable t) {
            logger.log("写入字段 " + field.getName() + " 失败", t);
            return false;
        }
    }

    Object getStaticField(Class<?> type, String name) {
        Field field = findField(type, name);
        return getStaticField(field);
    }

    Object getStaticField(Field field) {
        if (field == null) return null;
        try {
            return field.get(null);
        } catch (Throwable t) {
            logger.log("读取静态字段 " + field.getDeclaringClass().getName() + "." + field.getName() + " 失败", t);
            return null;
        }
    }

    Object invoke(Object instance, Method method, Object... args) {
        if (method == null) return null;
        try {
            makeAccessible(method);
            return method.invoke(instance, args);
        } catch (Throwable t) {
            Throwable cause = (t instanceof InvocationTargetException)
                    ? ((InvocationTargetException) t).getTargetException()
                    : t;
            logger.log("调用 " + method.getDeclaringClass().getName() + "." + method.getName() + " 失败", cause);
            return null;
        }
    }

    Object invokeNamed(Object instance, String name, Object... args) {
        if (instance == null) return null;
        Method method = findCompatibleMethod(instance.getClass(), name, args);
        if (method == null) return null;
        return invoke(instance, method, args);
    }

    Object invokeStatic(Class<?> type, String name, Object... args) {
        Method method = findCompatibleMethod(type, name, args);
        if (method == null) return null;
        return invoke(null, method, args);
    }

    boolean hasMethod(Class<?> type, String name, int parameterCount) {
        return findMethod(type, name, parameterCount, null) != null;
    }

    private Method findCompatibleMethod(Class<?> type, String name, Object[] args) {
        int length = args != null ? args.length : 0;
        return findMethod(type, name, length, method -> {
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < length; i++) {
                Object arg = args[i];
                Class<?> param = parameterTypes[i];
                if (arg == null) {
                    if (param.isPrimitive()) return false;
                } else {
                    if (!box(param).isAssignableFrom(arg.getClass())) return false;
                }
            }
            return true;
        });
    }

    private Constructor<?> findConstructor(Class<?> type, Object[] args) {
        int length = args != null ? args.length : 0;
        try {
            for (Constructor<?> ctor : type.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = ctor.getParameterTypes();
                if (parameterTypes.length != length) continue;
                boolean match = true;
                for (int i = 0; i < length; i++) {
                    Object arg = args[i];
                    Class<?> param = parameterTypes[i];
                    if (arg == null) {
                        if (param.isPrimitive()) {
                            match = false;
                            break;
                        }
                    } else {
                        if (!box(param).isAssignableFrom(arg.getClass())) {
                            match = false;
                            break;
                        }
                    }
                }
                if (match) return ctor;
            }
        } catch (Throwable t) {
            logger.log("枚举 " + type.getName() + " 构造方法失败", t);
        }
        return null;
    }

    private void makeAccessible(Object member) {
        try {
            if (member instanceof Method) {
                ((Method) member).setAccessible(true);
            } else if (member instanceof Field) {
                ((Field) member).setAccessible(true);
            } else if (member instanceof Constructor<?>) {
                ((Constructor<?>) member).setAccessible(true);
            }
        } catch (Throwable t) {
            logger.log("设置反射访问权限失败", t);
        }
    }

    private Class<?> box(Class<?> type) {
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        return type;
    }
}
