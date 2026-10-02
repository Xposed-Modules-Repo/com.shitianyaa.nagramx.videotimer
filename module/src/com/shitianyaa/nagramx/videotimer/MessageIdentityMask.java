package com.shitianyaa.nagramx.videotimer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 作用域限定的 MessageObject 身份欺骗。
 */
final class MessageIdentityMask {

    enum Kind { MUSIC, VIDEO }

    public interface Action<T> {
        T run() throws Throwable;
    }

    public interface VoidAction {
        void run() throws Throwable;
    }

    static final class Spec {
        private final Object[] targets;
        private final Boolean isMusic;
        private final Boolean isVideo;

        Spec(Object[] targets, Boolean isMusic, Boolean isVideo) {
            this.targets = targets != null ? targets : new Object[0];
            this.isMusic = isMusic;
            this.isVideo = isVideo;
        }

        boolean matches(Object target) {
            for (Object candidate : targets) {
                if (candidate == target) return true;
            }
            return false;
        }

        Boolean valueOf(Kind kind) {
            if (kind == Kind.MUSIC) return isMusic;
            if (kind == Kind.VIDEO) return isVideo;
            return null;
        }

        static Spec asMusic(Object... targets) {
            Object[] present = filterNotNull(targets);
            if (present.length == 0) return null;
            return new Spec(present, Boolean.TRUE, null);
        }

        static Spec asMusicHidingVideo(Object... targets) {
            Object[] present = filterNotNull(targets);
            if (present.length == 0) return null;
            return new Spec(present, Boolean.TRUE, Boolean.FALSE);
        }

        static Spec hidingVideo(Object... targets) {
            Object[] present = filterNotNull(targets);
            if (present.length == 0) return null;
            return new Spec(present, null, Boolean.FALSE);
        }

        private static Object[] filterNotNull(Object[] array) {
            if (array == null || array.length == 0) return new Object[0];
            List<Object> list = new ArrayList<>(array.length);
            for (Object o : array) {
                if (o != null) list.add(o);
            }
            return list.toArray();
        }
    }

    private static final AtomicInteger depth = new AtomicInteger();
    private static final ThreadLocal<List<Spec>> stack = new ThreadLocal<List<Spec>>() {
        @Override
        protected List<Spec> initialValue() {
            return new ArrayList<>(4);
        }
    };

    private MessageIdentityMask() {}

    static <T> T around(Spec spec, Action<T> action) {
        if (spec == null) {
            try {
                return action.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        List<Spec> frames = stack.get();
        if (frames == null) {
            try {
                return action.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
        frames.add(spec);
        depth.incrementAndGet();
        try {
            return action.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            depth.decrementAndGet();
            frames.remove(frames.size() - 1);
        }
    }

    static void around(Spec spec, VoidAction action) {
        around(spec, () -> {
            action.run();
            return null;
        });
    }

    static Boolean resolve(Object target, Kind kind) {
        if (target == null || depth.get() == 0) return null;
        List<Spec> frames = stack.get();
        if (frames == null) return null;
        for (int i = frames.size() - 1; i >= 0; i--) {
            Spec spec = frames.get(i);
            if (!spec.matches(target)) continue;
            Boolean val = spec.valueOf(kind);
            if (val != null) return val;
        }
        return null;
    }
}
