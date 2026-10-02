package com.shitianyaa.nagramx.videotimer;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * 定时关闭的状态机。
 */
final class VideoBackgroundSession {

    public static final int MODE_OFF = 0;
    public static final int MODE_DURATION = 1;
    public static final int MODE_AFTER_CURRENT = 2;

    public static final int DEFAULT_MINUTES = 30;
    public static final int MAX_MINUTES = 23 * 60 + 59; // 1439
    private static final long TICK_MS = 1000L;

    public interface DelayedTaskScheduler {
        void postDelayed(Runnable task, long delayMillis);
        void removeCallbacks(Runnable task);
    }

    public static class MainThreadDelayedTaskScheduler implements DelayedTaskScheduler {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void postDelayed(Runnable task, long delayMillis) {
            handler.postDelayed(task, delayMillis);
        }

        @Override
        public void removeCallbacks(Runnable task) {
            handler.removeCallbacks(task);
        }
    }

    public interface TimeProvider {
        long elapsedRealtime();
    }

    public interface ProbeCallback {
        ProbeResult probe();
    }

    public interface ExpireCallback {
        void onExpire();
    }

    public interface TickCallback {
        void onTick();
    }

    public static final class ProbeResult {
        public final boolean sessionAlive;
        public final int messageId;
        public final boolean reachedEnd;
        public final boolean isPlaying;

        public ProbeResult(boolean sessionAlive, int messageId, boolean reachedEnd, boolean isPlaying) {
            this.sessionAlive = sessionAlive;
            this.messageId = messageId;
            this.reachedEnd = reachedEnd;
            this.isPlaying = isPlaying;
        }

        public ProbeResult(boolean sessionAlive, int messageId, boolean reachedEnd) {
            this(sessionAlive, messageId, reachedEnd, true);
        }

        public boolean endedCurrentItem(int anchorId) {
            if (anchorId == 0) return reachedEnd;
            if (messageId != 0 && messageId != anchorId) return true;
            return reachedEnd;
        }

        public static final ProbeResult UNKNOWN = new ProbeResult(true, 0, false, true);
    }

    private final HostBridge.Logger logger;
    private final TimeProvider timeProvider;
    private final DelayedTaskScheduler scheduler;

    private int mode = MODE_OFF;
    private long remainingMs = 0L;
    private long lastTickRealtime = 0L;
    private boolean hasLastTickRealtime = false;
    private int anchorMessageId = 0;
    private int lastPickedMinutes = DEFAULT_MINUTES;
    private boolean backgroundActive = false;
    private boolean active = false;

    private ExpireCallback onExpire;
    private ProbeCallback probe;
    private TickCallback onTick;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!isArmed()) return;
            ProbeResult result;
            try {
                result = probe != null ? probe.probe() : ProbeResult.UNKNOWN;
            } catch (Throwable t) {
                logger.log("定时器探测播放状态失败", t);
                result = ProbeResult.UNKNOWN;
            }

            long now = timeProvider.elapsedRealtime();
            long elapsed = hasLastTickRealtime ? (now - lastTickRealtime) : 0L;
            lastTickRealtime = now;
            hasLastTickRealtime = true;

            // 只有当实际在播放时，才扣除倒计时时间；暂停时倒计时冻结
            if (mode == MODE_DURATION && elapsed > 0L && result.sessionAlive && result.isPlaying) {
                remainingMs -= elapsed;
                if (remainingMs < 0L) remainingMs = 0L;
            }

            if (!result.sessionAlive) {
                // Player ownership can be absent while entering/leaving fullscreen.
                // Keep the user's timer, freezing it until a video is available again.
                notifyTick();
                scheduler.postDelayed(this, TICK_MS);
                return;
            }

            boolean expired = false;
            if (mode == MODE_DURATION) {
                expired = remainingMs <= 0L;
            } else if (mode == MODE_AFTER_CURRENT) {
                expired = result.endedCurrentItem(anchorMessageId);
            }

            if (expired) {
                disarm();
                if (onExpire != null) {
                    try {
                        onExpire.onExpire();
                    } catch (Throwable t) {
                        logger.log("定时到期处理失败", t);
                    }
                }
                return;
            }

            notifyTick();
            scheduler.postDelayed(this, TICK_MS);
        }
    };

    VideoBackgroundSession(HostBridge.Logger logger) {
        this(logger, SystemClock::elapsedRealtime, new MainThreadDelayedTaskScheduler());
    }

    VideoBackgroundSession(HostBridge.Logger logger, TimeProvider timeProvider, DelayedTaskScheduler scheduler) {
        this.logger = logger != null ? logger : (msg, t) -> {};
        this.timeProvider = timeProvider != null ? timeProvider : SystemClock::elapsedRealtime;
        this.scheduler = scheduler != null ? scheduler : new MainThreadDelayedTaskScheduler();
    }

    int getMode() {
        return mode;
    }

    int getLastPickedMinutes() {
        return lastPickedMinutes;
    }

    boolean isBackgroundActive() {
        return backgroundActive;
    }

    boolean isActive() {
        return active;
    }

    void end() {
        logger.log("用户播放会话结束：timer=" + isArmed(), null);
        active = false;
        backgroundActive = false;
        disarm();
    }

    boolean isArmed() {
        return mode != MODE_OFF;
    }

    int getRemainingMinutes() {
        if (mode != MODE_DURATION) return 0;
        if (remainingMs <= 0L) return 0;
        return (int) ((remainingMs + 59999L) / 60000L);
    }

    int getRemainingSeconds() {
        if (mode != MODE_DURATION) return 0;
        if (remainingMs <= 0L) return 0;
        return (int) ((remainingMs + 999L) / 1000L);
    }

    String getFormattedRemainingTime() {
        if (mode != MODE_DURATION) return "";
        long totalSeconds = getRemainingSeconds();
        if (totalSeconds <= 0L) return "00:00";
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    TimerState snapshot() {
        return new TimerState(
                isArmed(),
                mode,
                mode == MODE_DURATION ? getRemainingMinutes() : 0,
                lastPickedMinutes
        );
    }

    void bind(ProbeCallback probe, ExpireCallback onExpire, TickCallback onTick) {
        this.probe = probe;
        this.onExpire = onExpire;
        this.onTick = onTick;
    }

    void markBackgroundActive(boolean active) {
        this.backgroundActive = active;
        if (active) this.active = true;
        notifyTick();
    }

    void arm(int mode, int minutes, int currentMessageId) {
        if (mode == MODE_DURATION) {
            int bounded = Math.max(1, Math.min(minutes, MAX_MINUTES));
            this.mode = MODE_DURATION;
            this.remainingMs = bounded * 60000L;
            this.lastTickRealtime = timeProvider.elapsedRealtime();
            this.hasLastTickRealtime = true;
            this.anchorMessageId = 0;
            this.lastPickedMinutes = bounded;
        } else if (mode == MODE_AFTER_CURRENT) {
            this.mode = MODE_AFTER_CURRENT;
            this.remainingMs = 0L;
            this.lastTickRealtime = 0L;
            this.hasLastTickRealtime = false;
            this.anchorMessageId = currentMessageId;
        } else {
            disarm();
            return;
        }
        active = true;
        logger.log("设置定时：mode=" + mode + ", minutes=" + minutes + ", message=" + currentMessageId, null);
        restartTicking();
        notifyTick();
    }

    void disarm() {
        this.mode = MODE_OFF;
        this.remainingMs = 0L;
        this.lastTickRealtime = 0L;
        this.hasLastTickRealtime = false;
        this.anchorMessageId = 0;
        scheduler.removeCallbacks(tick);
        notifyTick();
    }

    private void notifyTick() {
        if (onTick != null) {
            try {
                onTick.onTick();
            } catch (Throwable t) {
                logger.log("定时器状态变化回调失败", t);
            }
        }
    }

    private void restartTicking() {
        scheduler.removeCallbacks(tick);
        scheduler.postDelayed(tick, TICK_MS);
    }
}
