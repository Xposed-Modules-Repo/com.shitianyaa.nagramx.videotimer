package com.shitianyaa.nagramx.videotimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * PhotoViewer 侧：视频菜单注入与后台播放启动。
 */
final class PhotoViewerAgent {

    private static final int MENU_ID = 0x4E585654;
    private static final int MAX_INJECTION_ATTEMPTS = 5;
    private static final long INJECTION_RETRY_MS = 200L;
    private static final int MENU_TEXT_COLOR = 0xFFFAFAFA;
    private static final int MENU_SELECTOR_COLOR = 0x0FFFFFFF;
    private static final int ACCENT_COLOR = 0xFF73B4EC;
    private static final int ACCENT_SELECTOR_COLOR = 0x0F73B4EC;
    private static final int[] FALLBACK_MINUTES = new int[]{15, 30, 45, 60, 90};

    private static class MenuEntry {
        final View item;
        MenuEntry(View item) {
            this.item = item;
        }
    }

    private final HostProfile host;
    private final MediaControllerAgent media;
    private final VideoBackgroundSession session;
    private final HostTheme theme;
    private final HostStrings strings;
    private final VideoSleepTimerSheet sheet;
    private final PlayerUiAgent playerUi;
    private final HostBridge.Logger logger;
    private final HostBridge bridge;
    private final PhotoViewerMembers photo;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<Object, MenuEntry> injected = Collections.synchronizedMap(new WeakHashMap<>());
    private final Map<Object, Integer> attempts = Collections.synchronizedMap(new WeakHashMap<>());

    private volatile Object transferredPlayer;

    PhotoViewerAgent(
            HostProfile host,
            MediaControllerAgent media,
            VideoBackgroundSession session,
            HostTheme theme,
            HostStrings strings,
            VideoSleepTimerSheet sheet,
            PlayerUiAgent playerUi,
            HostBridge.Logger logger
    ) {
        this.host = host;
        this.media = media;
        this.session = session;
        this.theme = theme;
        this.strings = strings;
        this.sheet = sheet;
        this.playerUi = playerUi;
        this.logger = logger != null ? logger : (msg, t) -> {};
        this.bridge = host.bridge;
        this.photo = host.photo;
    }

    Object getTransferredPlayer() {
        return transferredPlayer;
    }

    void scheduleMenuInjection(Object photoViewer) {
        mainHandler.post(() -> injectMenu(photoViewer));
    }

    private void injectMenu(Object photoViewer) {
        synchronized (injected) {
            if (injected.containsKey(photoViewer)) return;
        }

        Object videoMenu = bridge.getField(photoViewer, photo.videoItemField);
        Object activityObj = bridge.invoke(photoViewer, photo.getParentActivity);
        if (videoMenu == null || !(activityObj instanceof Activity)) {
            retryInjection(photoViewer);
            return;
        }
        Activity activity = (Activity) activityObj;

        Method addSubItem = bridge.findMethod(videoMenu.getClass(), "addSubItem", 3, m -> {
            Class<?>[] p = m.getParameterTypes();
            return p[0] == int.class && p[1] == int.class && CharSequence.class.isAssignableFrom(p[2]);
        });
        if (addSubItem == null) {
            logger.log("ActionBarMenuItem.addSubItem 签名不匹配，放弃注入定时菜单", null);
            return;
        }

        Object itemObj = bridge.invoke(
                videoMenu,
                addSubItem,
                MENU_ID,
                timerIcon(activity),
                strings.get(activity, "VideoSleepTimer")
        );
        if (!(itemObj instanceof View)) {
            logger.log("创建定时菜单项失败", null);
            return;
        }
        View item = (View) itemObj;
        styleMenuItem(item);
        item.setOnClickListener(v -> {
            bridge.invokeNamed(videoMenu, "closeSubMenu");
            openTimerUi(photoViewer, activity);
        });

        Object listObj = bridge.invoke(videoMenu, addSubItem, MENU_ID + 1, timerIcon(activity),
                strings.get(activity, "VideoPlaylist"));
        if (listObj instanceof View && playerUi != null) {
            View listItem = (View) listObj;
            styleMenuItem(listItem);
            listItem.setOnClickListener(v -> {
                bridge.invokeNamed(videoMenu, "closeSubMenu");
                new VideoPlaylistSheet(host, theme, strings, logger).show(activity,
                        videoPlaylistOf(photoViewer, host.messageOf(photoViewer)), host.messageOf(photoViewer),
                        target -> playerUi.selectVideo(photoViewer, activity, target));
            });
        }

        synchronized (injected) {
            injected.put(photoViewer, new MenuEntry(item));
        }
        synchronized (attempts) {
            attempts.remove(photoViewer);
        }
        refreshMenuState(photoViewer);
        logger.log("已注入 PhotoViewer 定时菜单", null);
    }

    private void retryInjection(Object photoViewer) {
        int nextAttempt;
        synchronized (attempts) {
            Integer current = attempts.get(photoViewer);
            nextAttempt = (current != null ? current : 0) + 1;
            attempts.put(photoViewer, nextAttempt);
        }
        if (nextAttempt > MAX_INJECTION_ATTEMPTS) {
            synchronized (attempts) {
                attempts.remove(photoViewer);
            }
            logger.log("PhotoViewer 菜单多次重试仍未就绪，放弃本次注入", null);
            return;
        }
        mainHandler.postDelayed(() -> injectMenu(photoViewer), INJECTION_RETRY_MS * nextAttempt);
    }

    void refreshMenuState(Object photoViewer) {
        MenuEntry entry;
        synchronized (injected) {
            entry = photoViewer != null ? injected.get(photoViewer) : null;
        }
        if (entry == null) return;
        Context context = entry.item.getContext();
        if (context == null) return;

        TimerState state = session.snapshot();
        String subtext = "";
        if (state.active) {
            if (state.mode == VideoBackgroundSession.MODE_AFTER_CURRENT) {
                subtext = strings.get(context, "VideoSleepTimerAfterCurrent");
            } else {
                subtext = session.getFormattedRemainingTime();
            }
        }
        bridge.invokeNamed(entry.item, "setSubtext", subtext.isEmpty() ? null : subtext);
        bridge.invokeNamed(
                entry.item,
                "setEnabledByColor",
                state.active,
                MENU_TEXT_COLOR,
                ACCENT_COLOR
        );
        bridge.invokeNamed(
                entry.item,
                "setSelectorColor",
                state.active ? ACCENT_SELECTOR_COLOR : MENU_SELECTOR_COLOR
        );
    }

    void refreshAllMenus() {
        List<Object> viewers;
        synchronized (injected) {
            viewers = new ArrayList<>(injected.keySet());
        }
        for (Object viewer : viewers) {
            refreshMenuState(viewer);
        }
    }

    private void openTimerUi(Object photoViewer, Activity activity) {
        if (!host.isVideoMessage(host.messageOf(photoViewer))) {
            toast(activity, strings.get(activity, "VideoSleepTimerChoose"));
            return;
        }
        TimerState state = session.snapshot();
        boolean shown = sheet.show(
                activity,
                state,
                (mode, minutes) -> onTimerPicked(photoViewer, activity, mode, minutes),
                () -> cancelTimer(activity)
        );
        if (!shown) {
            logger.log("宿主 BottomSheet 不可用，改用系统对话框", null);
            showFallbackDialog(photoViewer, activity, state);
        }
    }

    private void onTimerPicked(Object photoViewer, Activity activity, int mode, int minutes) {
        Object message = host.messageOf(photoViewer);
        Object idVal = bridge.invoke(message, host.message.getId);
        int messageId = (idVal instanceof Number) ? ((Number) idVal).intValue() : 0;

        Object player = host.playerOf(photoViewer);
        if (player == null || !host.isVideoMessage(message)) {
            toast(activity, strings.get(activity, "VideoSleepTimerChoose"));
            return;
        }
        // Keep ownership in PhotoViewer until the user closes it themselves.
        bridge.invoke(player, host.player.setLooping, false);
        session.arm(mode, minutes, messageId);
        refreshAllMenus();

        String text;
        if (mode == VideoBackgroundSession.MODE_AFTER_CURRENT) {
            text = strings.get(activity, "VideoSleepTimerAfterCurrentSet");
        } else {
            text = strings.format(activity, "VideoSleepTimerSetFor", strings.duration(activity, minutes));
        }
        toast(activity, text);
    }

    private void cancelTimer(Activity activity) {
        session.disarm();
        refreshAllMenus();
        toast(activity, strings.get(activity, "VideoSleepTimerDisabled"));
    }

    private boolean transferToMediaController(Object photoViewer, Object player, Object message) {
        transferredPlayer = player;
        Object controller = media.controller();
        if (controller == null) return false;

        Method inject = host.media.injectVideoPlayer;
        if (inject != null) {
            // injectVideoPlayer synchronously broadcasts playback start while the
            // viewer is still visible. Chat listeners must not reclaim its surface.
            MessageIdentityMask.around(MessageIdentityMask.Spec.asMusicHidingVideo(message),
                    () -> bridge.invoke(controller, inject, player, message));
        } else if (photo.injectToMediaController != null) {
            bridge.invoke(photoViewer, photo.injectToMediaController);
        }
        if (bridge.getField(controller, host.media.videoPlayerField) != player) return false;

        bridge.setField(photoViewer, photo.videoPlayerField, null);
        bridge.setField(photoViewer, photo.playerInjectedField, true);
        bridge.setField(photoViewer, photo.isPlayingField, false);
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<Object> videoPlaylistOf(Object photoViewer, Object current) {
        Object imagesObj = bridge.getField(photoViewer, photo.imagesArrField);
        if (!(imagesObj instanceof List)) {
            return playerUi != null ? playerUi.playlistFor(current) : Collections.singletonList(current);
        }
        List<?> images = (List<?>) imagesObj;
        List<Object> result = new ArrayList<>();
        for (Object item : images) {
            if (item != null && host.isVideoMessage(item)) {
                result.add(item);
            }
        }
        return result.size() <= 1 && playerUi != null ? playerUi.playlistFor(current) : result;
    }

    void preservePlayerOnClose(Object photoViewer) {
        Object player = host.playerOf(photoViewer);
        Object message = host.messageOf(photoViewer);
        if (message != null && host.isVideoMessage(message) && player == null) {
            Object controller = media.controller();
            boolean accepted = Boolean.TRUE.equals(bridge.invoke(controller, host.media.playMessage, message, false));
            if (accepted) {
                logger.log("视频加载转入后台：message=" + host.messageIdOf(message), null);
                session.markBackgroundActive(true);
                media.postPlayStateChanged();
                if (playerUi != null) playerUi.notifyPlaybackStarted();
            } else logger.log("视频加载转后台失败，保留定时设置", null);
            return;
        }
        if (player == null || message == null || !host.isVideoMessage(message)) {
            logger.log("关闭视频时无法转后台：player=" + (player != null) + ", message=" + (message != null), null);
            return;
        }

        Object controller = media.controller();
        if (controller == null) return;
        Object currentMediaVideo = host.bridge.getField(controller, host.media.videoPlayerField);
        if (currentMediaVideo == player) {
            // A retained surface-handshake reference is not necessarily a media session.
            if (media.playingMessage() == null) {
                MessageIdentityMask.around(MessageIdentityMask.Spec.asMusicHidingVideo(message),
                        () -> bridge.invoke(controller, host.media.injectVideoPlayer, player, message));
            }
            // Original PhotoViewer.releasePlayer releases its own videoPlayer.
            // Ownership is detached by clearing that reference, not by fork flags.
            bridge.setField(photoViewer, photo.playerInjectedField, true);
            bridge.setField(photoViewer, photo.videoPlayerField, null);
            bridge.setField(photoViewer, photo.isPlayingField, false);

            session.markBackgroundActive(true);
            media.startMusicPlayerService();
            media.postPlayStateChanged();
            if (playerUi != null) {
                playerUi.notifyPlaybackStarted();
            }
            return;
        }

        Object isPlaying = host.bridge.invoke(player, host.player.isPlaying);
        boolean paused = !Boolean.TRUE.equals(isPlaying);

        Object posVal = host.bridge.invoke(player, host.player.getCurrentPosition);
        long position = 0L;
        if (posVal instanceof Number) {
            long p = ((Number) posVal).longValue();
            if (p > 0L && p != MediaControllerAgent.TIME_UNSET) position = p;
        }

        host.bridge.invoke(player, host.player.setLooping, false);
        boolean started = media.startBackgroundPlayback(
                player,
                message,
                videoPlaylistOf(photoViewer, message),
                position,
                (transferPlayer, transferMessage) -> transferToMediaController(photoViewer, transferPlayer, transferMessage)
        );
        if (started) {
            logger.log("视频转入后台成功：message=" + host.messageIdOf(message) + ", timer=" + session.isArmed(), null);
            session.markBackgroundActive(true);
            bridge.setField(photoViewer, photo.playerInjectedField, true);
            bridge.setField(photoViewer, photo.videoPlayerField, null);
            bridge.setField(photoViewer, photo.isPlayingField, false);

            if (paused) {
                host.bridge.setField(controller, host.media.isPausedField, true);
                Object vp = host.bridge.getField(controller, host.media.videoPlayerField);
                if (vp != null) {
                    host.bridge.invoke(vp, host.player.pause);
                }
            }
            media.postPlayStateChanged();
            if (playerUi != null) {
                playerUi.notifyPlaybackStarted();
            }
        }
    }

    private static final int PROGRESS_NONE = -1;
    private static final int PROGRESS_EMPTY = 0;
    private static final int PROGRESS_CANCEL = 1;
    private static final int PROGRESS_LOAD = 2;
    private static final int PROGRESS_PLAY = 3;
    private static final int PROGRESS_PAUSE = 4;

    boolean isPlayerAlive(Object photoViewer) {
        if (photoViewer == null) return false;
        Object player = host.playerOf(photoViewer);
        Object message = host.messageOf(photoViewer);
        return player != null && message != null && host.isVideoMessage(message);
    }

    void ensurePausePlayButtonVisible(Object photoViewer) {
        if (photoViewer == null) return;
        try {
            Object player = host.playerOf(photoViewer);
            if (player == null) return;
            Object playbackState = bridge.invoke(player, host.player.getPlaybackState);
            if (!(playbackState instanceof Number) || ((Number) playbackState).intValue() != MediaControllerAgent.EXO_STATE_READY) return;
            Object[] progressViews = (Object[]) bridge.getField(photoViewer, "photoProgressViews");
            if (progressViews != null && progressViews.length > 0 && progressViews[0] != null) {
                boolean isPlaying = false;
                if (player != null) {
                    Object isP = bridge.invoke(player, host.player.isPlaying);
                    isPlaying = Boolean.TRUE.equals(isP);
                }
                int targetState = isPlaying ? PROGRESS_PAUSE : PROGRESS_PLAY;
                bridge.invokeNamed(progressViews[0], "setBackgroundState", targetState, false, true);
                bridge.invokeNamed(progressViews[0], "setIndexedAlpha", 1, 1.0f, false);
            }
        } catch (Throwable t) {
            logger.log("更新视频播放按钮失败", t);
        }
    }

    void onBackgroundSessionEnded() {
        transferredPlayer = null;
        session.markBackgroundActive(false);
        refreshAllMenus();
    }

    private void showFallbackDialog(Object photoViewer, Activity activity, TimerState state) {
        List<String> labels = new ArrayList<>();
        for (int m : FALLBACK_MINUTES) {
            labels.add(strings.duration(activity, m));
        }
        labels.add(strings.get(activity, "VideoSleepTimerAfterCurrent"));
        if (state.active) {
            labels.add(strings.get(activity, "VideoSleepTimerCancel"));
        }

        new AlertDialog.Builder(activity)
                .setTitle(strings.get(activity, "VideoSleepTimer"))
                .setItems(labels.toArray(new String[0]), (dialog, which) -> {
                    if (which < FALLBACK_MINUTES.length) {
                        onTimerPicked(photoViewer, activity, VideoBackgroundSession.MODE_DURATION, FALLBACK_MINUTES[which]);
                    } else if (which == FALLBACK_MINUTES.length) {
                        onTimerPicked(photoViewer, activity, VideoBackgroundSession.MODE_AFTER_CURRENT, 0);
                    } else {
                        cancelTimer(activity);
                    }
                })
                .setNegativeButton(strings.get(activity, "Cancel"), null)
                .show();
    }

    private int timerIcon(Context context) {
        for (String name : new String[]{"baseline_timer_24", "msg_timer", "msg_autodelete", "menu_video_loop"}) {
            int id = context.getResources().getIdentifier(name, "drawable", context.getPackageName());
            if (id != 0) return id;
        }
        return 0;
    }

    private void styleMenuItem(View item) {
        bridge.invokeNamed(item, "setColors", MENU_TEXT_COLOR, MENU_TEXT_COLOR);
        bridge.invokeNamed(item, "setSelectorColor", MENU_SELECTOR_COLOR);
    }

    void toast(Context context, String text) {
        mainHandler.post(() -> {
            try {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            } catch (Throwable t) {
                logger.log("显示提示失败", t);
            }
        });
    }
}
