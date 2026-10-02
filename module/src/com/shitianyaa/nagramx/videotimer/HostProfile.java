package com.shitianyaa.nagramx.videotimer;

import android.app.Activity;
import android.content.Context;
import android.view.TextureView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 宿主 PhotoViewer 成员。
 */
final class PhotoViewerMembers {
    final Method getInstance;
    final Method hasInstance;
    final Method setParentActivity;
    final Method getParentActivity;
    final Method closePhoto;
    final Method isVisible;
    final Method playVideoOrWeb;
    final Method pauseVideoOrWeb;
    final Method getVideoPlayer;
    final Method getCurrentMessageObject;
    final Method injectToMediaController;
    final Method openPhoto;
    final Method openPhotoList;
    final Method setImageIndex;
    final Method onActionClick;
    final Method playOrStopAnimatedStickers;
    final Method seekAnimatedStickersTo;

    final Field videoItemField;
    final Field loopItemField;
    final Field videoPlayerField;
    final Field currentMessageField;
    final Field imagesArrField;
    final Field playerLoopingField;
    final Field manuallyPausedField;
    final Field isPlayingField;
    final Field playerInjectedField;
    final Field activityContextField;

    final Method nativeBackgroundStart;
    final Method nativeTimerSheet;
    final Field nativeTimerItemField;
    final Method updatePlayerState;

    PhotoViewerMembers(HostBridge bridge, Class<?> type) {
        getInstance = bridge.findMethod(type, "getInstance", 0, m -> Modifier.isStatic(m.getModifiers()));
        hasInstance = bridge.findMethod(type, "hasInstance", 0, m -> Modifier.isStatic(m.getModifiers()));
        setParentActivity = bridge.findMethod(type, "setParentActivity", 3, m ->
                m.getParameterTypes().length > 0 && m.getParameterTypes()[0] == Activity.class);
        getParentActivity = bridge.findMethod(type, "getParentActivity", 0, m ->
                Activity.class.isAssignableFrom(m.getReturnType()));
        closePhoto = bridge.findMethod(type, "closePhoto", 2, m -> {
            Class<?>[] p = m.getParameterTypes();
            return p.length == 2 && p[0] == boolean.class && p[1] == boolean.class;
        });
        isVisible = bridge.findMethod(type, "isVisible", 0);
        playVideoOrWeb = bridge.findMethod(type, "playVideoOrWeb", 0);
        pauseVideoOrWeb = bridge.findMethod(type, "pauseVideoOrWeb", 0);
        getVideoPlayer = bridge.findMethod(type, "getVideoPlayer", 0);
        getCurrentMessageObject = bridge.findMethod(type, "getCurrentMessageObject", 0);
        injectToMediaController = bridge.findMethod(type, "injectVideoPlayerToMediaController", 0);
        openPhoto = bridge.findMethod(type, "openPhoto", 6, m ->
                m.getParameterTypes()[0].getName().endsWith(".MessageObject") &&
                        m.getParameterTypes()[1] == long.class &&
                        m.getParameterTypes()[2] == long.class);
        openPhotoList = bridge.findMethod(type, "openPhoto", 6, m ->
                m.getParameterTypes()[0] == ArrayList.class && m.getParameterTypes()[1] == int.class);
        setImageIndex = bridge.findMethod(type, "setImageIndex", 1, m -> m.getParameterTypes()[0] == int.class);
        onActionClick = bridge.findMethod(type, "onActionClick", 1, m -> m.getParameterTypes()[0] == boolean.class);

        playOrStopAnimatedStickers = bridge.findMethod(type, "playOrStopAnimatedStickers", 1, m ->
                m.getParameterTypes()[0] == boolean.class);
        seekAnimatedStickersTo = bridge.findMethod(type, "seekAnimatedStickersTo", 1, m ->
                m.getParameterTypes()[0] == long.class);

        videoItemField = bridge.findField(type, "videoItem");
        loopItemField = bridge.findField(type, "loopItem");
        videoPlayerField = bridge.findField(type, "videoPlayer");
        currentMessageField = bridge.findField(type, "currentMessageObject");
        imagesArrField = bridge.findField(type, "imagesArr");
        playerLoopingField = bridge.findField(type, "playerLooping");
        manuallyPausedField = bridge.findField(type, "manuallyPaused");
        isPlayingField = bridge.findField(type, "isPlaying");
        playerInjectedField = bridge.findField(type, "playerInjected");
        activityContextField = bridge.findField(type, "activityContext");

        nativeBackgroundStart = bridge.findMethod(type, "startVideoBackgroundPlayback", 2, m -> {
            Class<?>[] p = m.getParameterTypes();
            return m.getReturnType() == boolean.class && p.length == 2 && p[0] == int.class && p[1] == int.class;
        });
        nativeTimerSheet = bridge.findMethod(type, "showVideoSleepTimerSheet", 0);
        nativeTimerItemField = bridge.findField(type, "videoSleepTimerItem");
        updatePlayerState = bridge.findMethod(type, "updatePlayerState", boolean.class, int.class);
    }
}

/**
 * 宿主 MediaController 成员。
 */
final class MediaControllerMembers {
    final Method getInstance;
    final Method injectVideoPlayer;
    final Method cleanupPlayer4;
    final Method cleanupPlayer2;
    final Method playMessage;
    final Method pauseMessage;
    final Method getPlayingMessageObject;
    final Method getPlaylist;
    final Method isMessagePaused;
    final Method playNextMessage;
    final Method playPreviousMessage;
    final Method playNextMessageWithoutOrder;
    final Method clearPlaylist;
    final Method sortPlaylist;
    final Method buildShuffledPlayList;
    final Method updateVideoState;
    final Method canStartMusicPlayerService;
    final Method setCurrentVideoVisible;
    final List<Method> setTextureView;
    final Method getDuration;
    final Method getProgressMs;
    final Method seekToProgress;
    final Method seekToProgressMs;
    final Method isSamePlayingMessage;
    final Method resetGoingToShowMessageObject;

    final Field videoPlayerField;
    final Field audioPlayerField;
    final Field isPausedField;
    final Field downloadingCurrentMessageField;
    final Field playingMessageField;
    final Field playlistField;
    final Field playlistMapField;
    final Field shuffledPlaylistField;
    final Field currentPlaylistNumField;
    final Field forceLoopCurrentPlaylistField;
    final Field currentTextureViewField;
    final Field currentAspectRatioFrameLayoutField;
    final Field currentTextureViewContainerField;
    final Field currentAspectRatioReadyField;
    final Field pipRoundVideoViewField;
    final Field pipSwitchingStateField;
    final Field baseActivityField;

    final int timerDurationMode;
    final int timerAfterCurrentMode;

    MediaControllerMembers(HostBridge bridge, Class<?> type) {
        getInstance = bridge.findMethod(type, "getInstance", 0, m -> Modifier.isStatic(m.getModifiers()));
        injectVideoPlayer = bridge.findMethod(type, "injectVideoPlayer", 2, m ->
                !Modifier.isStatic(m.getModifiers()) &&
                        m.getParameterTypes()[0].getName().endsWith(".VideoPlayer") &&
                        m.getParameterTypes()[1].getName().endsWith(".MessageObject"));
        cleanupPlayer4 = bridge.findMethod(type, "cleanupPlayer", 4, m -> {
            for (Class<?> p : m.getParameterTypes()) {
                if (p != boolean.class) return false;
            }
            return true;
        });
        cleanupPlayer2 = bridge.findMethod(type, "cleanupPlayer", 2, m -> {
            for (Class<?> p : m.getParameterTypes()) {
                if (p != boolean.class) return false;
            }
            return true;
        });
        playMessage = bridge.findMethod(type, "playMessage", 2, m ->
                m.getParameterTypes()[0].getName().endsWith(".MessageObject") &&
                        m.getParameterTypes()[1] == boolean.class);
        pauseMessage = bridge.findMethod(type, "pauseMessage", 1, m ->
                m.getParameterTypes()[0].getName().endsWith(".MessageObject"));
        getPlayingMessageObject = bridge.findMethod(type, "getPlayingMessageObject", 0);
        getPlaylist = bridge.findMethod(type, "getPlaylist", 0);
        isMessagePaused = bridge.findMethod(type, "isMessagePaused", 0);
        playNextMessage = bridge.findMethod(type, "playNextMessage", 0);
        playPreviousMessage = bridge.findMethod(type, "playPreviousMessage", 0);
        playNextMessageWithoutOrder = bridge.findMethod(type, "playNextMessageWithoutOrder", 1, m ->
                m.getParameterTypes()[0] == boolean.class);
        clearPlaylist = bridge.findMethod(type, "clearPlaylist", 0);
        sortPlaylist = bridge.findMethod(type, "sortPlaylist", 0);
        buildShuffledPlayList = bridge.findMethod(type, "buildShuffledPlayList", 0);
        updateVideoState = bridge.findMethod(type, "updateVideoState", 5, m -> {
            Class<?>[] p = m.getParameterTypes();
            return p[0].getName().endsWith(".MessageObject") &&
                    p[1] == int[].class &&
                    p[2] == boolean.class &&
                    p[3] == boolean.class &&
                    p[4] == int.class;
        });
        canStartMusicPlayerService = bridge.findMethod(type, "canStartMusicPlayerService", 0, m ->
                m.getReturnType() == boolean.class);
        setCurrentVideoVisible = bridge.findMethod(type, "setCurrentVideoVisible", 1, m ->
                m.getParameterTypes()[0] == boolean.class);

        List<Method> tvList = new ArrayList<>();
        Method tv4 = bridge.findMethod(type, "setTextureView", 4, m ->
                TextureView.class.isAssignableFrom(m.getParameterTypes()[0]));
        if (tv4 != null) tvList.add(tv4);
        Method tv5 = bridge.findMethod(type, "setTextureView", 5, m ->
                TextureView.class.isAssignableFrom(m.getParameterTypes()[0]));
        if (tv5 != null) tvList.add(tv5);
        setTextureView = Collections.unmodifiableList(tvList);

        getDuration = bridge.findMethod(type, "getDuration", 0);
        getProgressMs = bridge.findMethod(type, "getProgressMs", 1, m ->
                m.getParameterTypes()[0].getName().endsWith(".MessageObject"));
        seekToProgress = bridge.findMethod(type, "seekToProgress", 2, m ->
                m.getParameterTypes()[1] == float.class);
        seekToProgressMs = bridge.findMethod(type, "seekToProgressMs", 2, m ->
                m.getParameterTypes()[1] == long.class);
        isSamePlayingMessage = bridge.findMethod(type, "isSamePlayingMessage", 1);
        resetGoingToShowMessageObject = bridge.findMethod(type, "resetGoingToShowMessageObject", 0);

        videoPlayerField = bridge.findField(type, "videoPlayer");
        audioPlayerField = bridge.findField(type, "audioPlayer");
        isPausedField = bridge.findField(type, "isPaused");
        downloadingCurrentMessageField = bridge.findField(type, "downloadingCurrentMessage");
        playingMessageField = bridge.findField(type, "playingMessageObject");
        playlistField = bridge.findField(type, "playlist");
        playlistMapField = bridge.findField(type, "playlistMap");
        shuffledPlaylistField = bridge.findField(type, "shuffledPlaylist");
        currentPlaylistNumField = bridge.findField(type, "currentPlaylistNum");
        forceLoopCurrentPlaylistField = bridge.findField(type, "forceLoopCurrentPlaylist");
        currentTextureViewField = bridge.findField(type, "currentTextureView");
        currentAspectRatioFrameLayoutField = bridge.findField(type, "currentAspectRatioFrameLayout");
        currentTextureViewContainerField = bridge.findField(type, "currentTextureViewContainer");
        currentAspectRatioReadyField = bridge.findField(type, "currentAspectRatioFrameLayoutReady");
        pipRoundVideoViewField = bridge.findField(type, "pipRoundVideoView");
        pipSwitchingStateField = bridge.findField(type, "pipSwitchingState");
        baseActivityField = bridge.findField(type, "baseActivity");

        Object dMode = bridge.getStaticField(type, "VIDEO_SLEEP_TIMER_DURATION");
        timerDurationMode = (dMode instanceof Number) ? ((Number) dMode).intValue() : VideoBackgroundSession.MODE_DURATION;
        Object aMode = bridge.getStaticField(type, "VIDEO_SLEEP_TIMER_AFTER_CURRENT");
        timerAfterCurrentMode = (aMode instanceof Number) ? ((Number) aMode).intValue() : VideoBackgroundSession.MODE_AFTER_CURRENT;
    }
}

/**
 * 宿主 VideoPlayer 成员。
 */
final class VideoPlayerMembers {
    final Method setDelegate;
    final Method setTextureView;
    final Method setSurfaceView;
    final Method setStreamType;
    final Method setLooping;
    final Method isLooping;
    final Method isPlaying;
    final Method getPlayWhenReady;
    final Method getPlaybackState;
    final Method getDuration;
    final Method getCurrentPosition;
    final Method play;
    final Method pause;
    final Method seekTo;

    VideoPlayerMembers(HostBridge bridge, Class<?> type) {
        setDelegate = bridge.findMethod(type, "setDelegate", 1);
        setTextureView = bridge.findMethod(type, "setTextureView", 1);
        setSurfaceView = bridge.findMethod(type, "setSurfaceView", 1);
        setStreamType = bridge.findMethod(type, "setStreamType", 1, m -> m.getParameterTypes()[0] == int.class);
        setLooping = bridge.findMethod(type, "setLooping", 1, m -> m.getParameterTypes()[0] == boolean.class);
        isLooping = bridge.findMethod(type, "isLooping", 0);
        isPlaying = bridge.findMethod(type, "isPlaying", 0);
        getPlayWhenReady = bridge.findMethod(type, "getPlayWhenReady", 0);
        getPlaybackState = bridge.findMethod(type, "getPlaybackState", 0);
        getDuration = bridge.findMethod(type, "getDuration", 0);
        getCurrentPosition = bridge.findMethod(type, "getCurrentPosition", 0);
        play = bridge.findMethod(type, "play", 0);
        pause = bridge.findMethod(type, "pause", 0);
        seekTo = bridge.findMethod(type, "seekTo", 1, m -> m.getParameterTypes()[0] == long.class);
    }
}

/**
 * 宿主 MessageObject 成员。
 */
final class MessageObjectMembers {
    final Method isVideo;
    final Method isVideoMessageStatic;
    final Method isMusic;
    final Method isVoice;
    final Method isRoundVideo;
    final Method isLivePhoto;
    final Method getId;
    final Method getDialogId;
    final Method getDocument;
    final Method getDuration;
    final Method getMusicTitle;
    final Method getMusicTitle0;
    final Method getMusicTitle1;
    final Method getMusicAuthor;
    final Method getMusicAuthor0;
    final Method getMusicAuthor1;
    final Field captionField;

    final Field audioProgressField;
    final Field audioProgressMsField;
    final Field audioProgressSecField;
    final Field currentAccountField;
    final Field messageOwnerField;

    final Method isOutOwner;
    final Method getSenderId;

    MessageObjectMembers(HostBridge bridge, Class<?> type) {
        isVideo = bridge.findMethod(type, "isVideo", 0);
        isVideoMessageStatic = bridge.findMethod(type, "isVideoMessage", 1, m ->
                Modifier.isStatic(m.getModifiers()) && m.getParameterTypes()[0].getName().endsWith("$Message"));
        isMusic = bridge.findMethod(type, "isMusic", 0);
        isVoice = bridge.findMethod(type, "isVoice", 0);
        isRoundVideo = bridge.findMethod(type, "isRoundVideo", 0);
        isLivePhoto = bridge.findMethod(type, "isLivePhoto", 0);
        getId = bridge.findMethod(type, "getId", 0);
        getDialogId = bridge.findMethod(type, "getDialogId", 0);
        getDocument = bridge.findMethod(type, "getDocument", 0);
        getDuration = bridge.findMethod(type, "getDuration", 0);
        getMusicTitle0 = bridge.findMethod(type, "getMusicTitle", 0);
        getMusicTitle1 = bridge.findMethod(type, "getMusicTitle", 1, m -> m.getParameterTypes()[0] == boolean.class);
        getMusicTitle = getMusicTitle1 != null ? getMusicTitle1 : getMusicTitle0;

        getMusicAuthor0 = bridge.findMethod(type, "getMusicAuthor", 0);
        getMusicAuthor1 = bridge.findMethod(type, "getMusicAuthor", 1, m -> m.getParameterTypes()[0] == boolean.class);
        getMusicAuthor = getMusicAuthor1 != null ? getMusicAuthor1 : getMusicAuthor0;

        captionField = bridge.findField(type, "caption");

        audioProgressField = bridge.findField(type, "audioProgress");
        audioProgressMsField = bridge.findField(type, "audioProgressMs");
        audioProgressSecField = bridge.findField(type, "audioProgressSec");
        currentAccountField = bridge.findField(type, "currentAccount");
        messageOwnerField = bridge.findField(type, "messageOwner");

        isOutOwner = bridge.findMethod(type, "isOutOwner", 0);
        getSenderId = bridge.findMethod(type, "getSenderId", 0);
    }
}

/**
 * TLRPC 成员。
 */
final class TlMembers {
    final Field messageText;
    final Field fwdFrom;
    final Field fromId;
    final Field peerId;
    final Field fwdFromName;
    final Field fwdFromId;

    TlMembers(HostBridge bridge) {
        Class<?> messageClass = bridge.loadClass("org.telegram.tgnet.TLRPC$Message");
        Class<?> fwdHeaderClass = bridge.loadClass("org.telegram.tgnet.TLRPC$MessageFwdHeader");

        messageText = bridge.findField(messageClass, "message");
        fwdFrom = bridge.findField(messageClass, "fwd_from");
        fromId = bridge.findField(messageClass, "from_id");
        peerId = bridge.findField(messageClass, "peer_id");

        fwdFromName = bridge.findField(fwdHeaderClass, "from_name");
        fwdFromId = bridge.findField(fwdHeaderClass, "from_id");
    }
}

/**
 * 宿主 MusicPlayerService 成员。
 */
final class MusicServiceMembers {
    final Method createNotification;
    final Method updatePlaybackState;
    final Method onStartCommand;
    final Method onDestroy;
    final Class<?> sessionCallbackClass;
    final Method onSkipToNext;
    final Method onSkipToPrevious;

    MusicServiceMembers(HostBridge bridge, Class<?> type) {
        createNotification = bridge.findMethod(type, "createNotification", 2, m ->
                m.getParameterTypes()[0].getName().endsWith(".MessageObject") &&
                        m.getParameterTypes()[1] == boolean.class);
        updatePlaybackState = bridge.findMethod(type, "updatePlaybackState", 1, m ->
                m.getParameterTypes()[0] == long.class);
        onStartCommand = bridge.findMethod(type, "onStartCommand", 3);
        onDestroy = bridge.findMethod(type, "onDestroy", 0);

        Class<?> callback = null;
        if (type != null) {
            Class<?> callbackBase = bridge.loadClass("android.support.v4.media.session.MediaSessionCompat$Callback");
            if (callbackBase != null) {
                for (int i = 1; i <= 12; i++) {
                    Class<?> candidate = bridge.loadClass(type.getName() + "$" + i);
                    if (candidate != null && callbackBase.isAssignableFrom(candidate)) {
                        callback = candidate;
                        break;
                    }
                }
            }
        }
        sessionCallbackClass = callback;
        onSkipToNext = bridge.findMethod(sessionCallbackClass, "onSkipToNext", 0);
        onSkipToPrevious = bridge.findMethod(sessionCallbackClass, "onSkipToPrevious", 0);
    }
}

/**
 * 宿主播放器 UI 成员（FragmentContextView、AudioPlayerAlert、ChatActivity）。
 */
final class PlayerUiMembers {
    final Class<?> contextViewClass;
    final Class<?> alertClass;
    final Class<?> chatActivityClass;

    final Method checkPlayer;
    final Method contextViewUpdatePlaybackButton;
    final Field subtitleTextViewField;
    final Field titleTextViewField;
    final Field currentStyleField;
    final Field isSideMenuedField;
    final Field isLocationField;
    final int styleAudioPlayer;
    final List<Method> contextViewClickLambdas;
    final List<Constructor<?>> contextViewConstructors;

    final Constructor<?> alertConstructor;
    final Method alertDidReceivedNotification;
    final Method alertUpdateTitle;
    final List<Method> alertUpdateProgress;
    final Method cellDidPressedButton;
    final Method cellSetMessageObject;
    final Field cellRadialProgressField;
    final Method cellGetMessageObject;

    final Field launchActivityInstanceField;
    final Field alertInstanceField;

    final Method onRemoveFromParent;
    final Method openPhotoViewerForMessage;
    final Method updateTextureViewPosition;
    final Method updateMessagesVisiblePart;

    PlayerUiMembers(HostBridge bridge, Class<?> contextView, Class<?> alert) {
        contextViewClass = contextView;
        alertClass = alert;
        chatActivityClass = bridge.loadClass("org.telegram.ui.ChatActivity");

        checkPlayer = bridge.findMethod(contextView, "checkPlayer", 1, m -> m.getParameterTypes()[0] == boolean.class);
        contextViewUpdatePlaybackButton = bridge.findMethod(contextView, "updatePlaybackButton", 1);
        subtitleTextViewField = bridge.findField(contextView, "subtitleTextView");
        titleTextViewField = bridge.findField(contextView, "titleTextView");
        currentStyleField = bridge.findField(contextView, "currentStyle");
        isSideMenuedField = bridge.findField(contextView, "isSideMenued");
        isLocationField = bridge.findField(contextView, "isLocation");

        Object style = bridge.getStaticField(contextView, "STYLE_AUDIO_PLAYER");
        styleAudioPlayer = (style instanceof Number) ? ((Number) style).intValue() : 0;

        List<Method> lambdas = new ArrayList<>();
        if (contextView != null) {
            try {
                for (Method m : contextView.getDeclaredMethods()) {
                    if ((m.isSynthetic() || m.getName().startsWith("lambda$")) &&
                            m.getReturnType() == void.class &&
                            m.getParameterTypes().length == 1 &&
                            m.getParameterTypes()[0].getName().equals("android.view.View")) {
                        m.setAccessible(true);
                        lambdas.add(m);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        contextViewClickLambdas = Collections.unmodifiableList(lambdas);

        List<Constructor<?>> cvCtors = new ArrayList<>();
        if (contextView != null) {
            try {
                for (Constructor<?> c : contextView.getDeclaredConstructors()) {
                    c.setAccessible(true);
                    cvCtors.add(c);
                }
            } catch (Throwable ignored) {
            }
        }
        contextViewConstructors = Collections.unmodifiableList(cvCtors);

        Constructor<?> alertCtor = null;
        if (alert != null) {
            try {
                for (Constructor<?> c : alert.getDeclaredConstructors()) {
                    if (c.getParameterTypes().length == 2 &&
                            c.getParameterTypes()[0].getName().equals("android.content.Context")) {
                        c.setAccessible(true);
                        alertCtor = c;
                        break;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        alertConstructor = alertCtor;

        alertDidReceivedNotification = bridge.findMethod(alert, "didReceivedNotification", 3);
        alertUpdateTitle = bridge.findMethod(alert, "updateTitle", 1, m -> m.getParameterTypes()[0] == boolean.class);

        List<Method> progressMethods = new ArrayList<>();
        Method p1 = bridge.findMethod(alert, "updateProgress", 1);
        if (p1 != null) progressMethods.add(p1);
        Method p2 = bridge.findMethod(alert, "updateProgress", 2);
        if (p2 != null) progressMethods.add(p2);
        alertUpdateProgress = Collections.unmodifiableList(progressMethods);

        Class<?> cellClass = bridge.loadClass("org.telegram.ui.Cells.AudioPlayerCell");
        cellDidPressedButton = bridge.findMethod(cellClass, "didPressedButton", 0);
        cellSetMessageObject = bridge.findMethod(cellClass, "setMessageObject", 1);
        cellRadialProgressField = bridge.findField(cellClass, "radialProgress");
        cellGetMessageObject = bridge.findMethod(cellClass, "getMessageObject", 0);

        Class<?> launchActivity = bridge.loadClass("org.telegram.ui.LaunchActivity");
        launchActivityInstanceField = bridge.findField(launchActivity, "instance");
        alertInstanceField = bridge.findField(alert, "lastInstance");

        onRemoveFromParent = bridge.findMethod(chatActivityClass, "onRemoveFromParent", 0);
        openPhotoViewerForMessage = bridge.findMethod(chatActivityClass, "openPhotoViewerForMessage", 2);
        updateTextureViewPosition = bridge.findMethod(chatActivityClass, "updateTextureViewPosition", 0);
        updateMessagesVisiblePart = bridge.findMethod(chatActivityClass, "updateMessagesVisiblePart", 1, m ->
                m.getParameterTypes()[0] == boolean.class);
    }
}

/**
 * 宿主探测环境与元数据映射。
 */
final class HostProfile {

    public static final String TARGET_PACKAGE = "nu.gpu.nagram";

    final HostBridge bridge;
    final int versionCode;
    final String versionName;

    final Class<?> photoViewerClass;
    final Class<?> mediaControllerClass;
    final Class<?> messageObjectClass;
    final Class<?> videoPlayerClass;
    final Class<?> musicPlayerServiceClass;
    final Class<?> pipRoundVideoViewClass;
    final Class<?> fileLoaderClass;

    final PhotoViewerMembers photo;
    final MediaControllerMembers media;
    final VideoPlayerMembers player;
    final MessageObjectMembers message;
    final MusicServiceMembers musicService;
    final PlayerUiMembers playerUi;

    final Class<?> messagesControllerClass;
    final Method messagesControllerGetInstance;
    final Method getPeerName;

    private final Class<?> dialogObjectClass;
    private final Method getPeerDialogId;

    final TlMembers tl;

    private final Class<?> userConfigClass;
    private final Method userConfigGetInstance;
    private final Method getClientUserId;

    private final Class<?> applicationLoaderClass;
    final Method cancelLoadFileByDocument;

    private HostProfile(
            HostBridge bridge,
            int versionCode,
            String versionName,
            Class<?> photoViewerClass,
            Class<?> mediaControllerClass,
            Class<?> messageObjectClass,
            Class<?> videoPlayerClass,
            Class<?> musicPlayerServiceClass,
            Class<?> pipRoundVideoViewClass,
            Class<?> fileLoaderClass,
            PhotoViewerMembers photo,
            MediaControllerMembers media,
            VideoPlayerMembers player,
            MessageObjectMembers message,
            MusicServiceMembers musicService,
            PlayerUiMembers playerUi
    ) {
        this.bridge = bridge;
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.photoViewerClass = photoViewerClass;
        this.mediaControllerClass = mediaControllerClass;
        this.messageObjectClass = messageObjectClass;
        this.videoPlayerClass = videoPlayerClass;
        this.musicPlayerServiceClass = musicPlayerServiceClass;
        this.pipRoundVideoViewClass = pipRoundVideoViewClass;
        this.fileLoaderClass = fileLoaderClass;

        this.photo = photo;
        this.media = media;
        this.player = player;
        this.message = message;
        this.musicService = musicService;
        this.playerUi = playerUi;

        this.messagesControllerClass = bridge.loadClass("org.telegram.messenger.MessagesController");
        this.messagesControllerGetInstance = bridge.findMethod(messagesControllerClass, "getInstance", 1, m ->
                Modifier.isStatic(m.getModifiers()) && m.getParameterTypes()[0] == int.class);
        this.getPeerName = bridge.findMethod(messagesControllerClass, "getPeerName", 1, m ->
                m.getParameterTypes()[0] == long.class);

        this.dialogObjectClass = bridge.loadClass("org.telegram.messenger.DialogObject");
        this.getPeerDialogId = bridge.findMethod(dialogObjectClass, "getPeerDialogId", 1, m ->
                Modifier.isStatic(m.getModifiers()) && m.getParameterTypes()[0].getName().endsWith("$Peer"));

        this.tl = new TlMembers(bridge);

        this.userConfigClass = bridge.loadClass("org.telegram.messenger.UserConfig");
        this.userConfigGetInstance = bridge.findMethod(userConfigClass, "getInstance", 1, m ->
                Modifier.isStatic(m.getModifiers()) && m.getParameterTypes()[0] == int.class);
        this.getClientUserId = bridge.findMethod(userConfigClass, "getClientUserId", 0);

        this.applicationLoaderClass = bridge.loadClass("org.telegram.messenger.ApplicationLoader");
        this.cancelLoadFileByDocument = bridge.findMethod(fileLoaderClass, "cancelLoadFile", 1, m ->
                m.getParameterTypes()[0].getName().endsWith("$Document"));
    }

    static HostProfile discover(ClassLoader classLoader, HostBridge.Logger logger) {
        HostBridge bridge = new HostBridge(classLoader, logger);
        Class<?> buildConfig = bridge.loadClass("org.telegram.messenger.BuildConfig");
        Class<?> photoViewer = bridge.loadClass("org.telegram.ui.PhotoViewer");
        Class<?> mediaController = bridge.loadClass("org.telegram.messenger.MediaController");

        if (buildConfig == null || photoViewer == null || mediaController == null) {
            logger.log("NagramX 必需类不存在，停止安装 Hook", null);
            return null;
        }

        int code = -1;
        for (String field : new String[]{"VERSION_CODE", "OFFICIAL_VERSION_CODE"}) {
            Object val = bridge.getStaticField(buildConfig, field);
            if (val instanceof Number) {
                code = ((Number) val).intValue();
                break;
            }
        }

        String name = null;
        for (String field : new String[]{"VERSION_NAME", "OFFICIAL_VERSION_NAME"}) {
            Object val = bridge.getStaticField(buildConfig, field);
            if (val instanceof String) {
                name = (String) val;
                break;
            }
        }

        Class<?> messageObject = bridge.loadClass("org.telegram.messenger.MessageObject");
        Class<?> videoPlayer = bridge.loadClass("org.telegram.ui.Components.VideoPlayer");
        MediaControllerMembers media = new MediaControllerMembers(bridge, mediaController);
        if (media.getInstance == null) {
            logger.log("找不到 MediaController.getInstance()，停止安装 Hook", null);
            return null;
        }

        Class<?> musicPlayerService = bridge.loadClass("org.telegram.messenger.MusicPlayerService");
        Class<?> pipRoundVideoView = bridge.loadClass("org.telegram.ui.Components.PipRoundVideoView");
        Class<?> fileLoader = bridge.loadClass("org.telegram.messenger.FileLoader");
        Class<?> contextView = bridge.loadClass("org.telegram.ui.Components.FragmentContextView");
        Class<?> audioPlayerAlert = bridge.loadClass("org.telegram.ui.Components.AudioPlayerAlert");

        return new HostProfile(
                bridge,
                code,
                name,
                photoViewer,
                mediaController,
                messageObject,
                videoPlayer,
                musicPlayerService,
                pipRoundVideoView,
                fileLoader,
                new PhotoViewerMembers(bridge, photoViewer),
                media,
                new VideoPlayerMembers(bridge, videoPlayer),
                new MessageObjectMembers(bridge, messageObject),
                new MusicServiceMembers(bridge, musicPlayerService),
                new PlayerUiMembers(bridge, contextView, audioPlayerAlert)
        );
    }

    int getSelectedAccount() {
        Object val = bridge.getStaticField(userConfigClass, "selectedAccount");
        return (val instanceof Number) ? ((Number) val).intValue() : 0;
    }

    long clientUserId(int account) {
        Object config = bridge.invoke(null, userConfigGetInstance, account);
        if (config == null) return 0L;
        Object val = bridge.invoke(config, getClientUserId);
        return (val instanceof Number) ? ((Number) val).longValue() : 0L;
    }

    Context getAppContext() {
        Object val = bridge.getStaticField(applicationLoaderClass, "applicationContext");
        return (val instanceof Context) ? (Context) val : null;
    }

    boolean hasCompleteNativeTimerUi() {
        return photo.nativeBackgroundStart != null && photo.nativeTimerItemField != null;
    }

    boolean canInjectTimerMenu() {
        return photo.setParentActivity != null && photo.getParentActivity != null && photo.videoItemField != null;
    }

    boolean canStartBackgroundPlayback() {
        return media.getInstance != null && media.injectVideoPlayer != null &&
                photo.closePhoto != null && photo.videoPlayerField != null &&
                player.setDelegate != null && musicPlayerServiceClass != null;
    }

    boolean canManagePlaylist() {
        return media.playlistField != null && media.playlistMapField != null && media.currentPlaylistNumField != null;
    }

    boolean canDressNotification() {
        return message.isMusic != null && musicService.createNotification != null;
    }

    String peerName(int account, long peerId) {
        if (peerId == 0L) return null;
        Object controller = bridge.invoke(null, messagesControllerGetInstance, account);
        if (controller == null) return null;
        Object val = bridge.invoke(controller, getPeerName, peerId);
        if (val instanceof String && !((String) val).trim().isEmpty()) {
            return (String) val;
        }
        return null;
    }

    String peerNameOf(int account, Object peer) {
        if (peer == null) return null;
        Object val = bridge.invoke(null, getPeerDialogId, peer);
        if (!(val instanceof Number)) return null;
        return peerName(account, ((Number) val).longValue());
    }

    Object mediaController() {
        return bridge.invoke(null, media.getInstance);
    }

    Object playerOf(Object photoViewer) {
        if (photoViewer == null) return null;
        Object p = bridge.invoke(photoViewer, photo.getVideoPlayer);
        if (p != null) return p;
        return bridge.getField(photoViewer, photo.videoPlayerField);
    }

    Object messageOf(Object photoViewer) {
        if (photoViewer == null) return null;
        Object m = bridge.invoke(photoViewer, photo.getCurrentMessageObject);
        if (m != null) return m;
        return bridge.getField(photoViewer, photo.currentMessageField);
    }

    long dialogIdOf(Object messageObject) {
        if (messageObject == null) return 0L;
        Object val = bridge.invoke(messageObject, message.getDialogId);
        return (val instanceof Number) ? ((Number) val).longValue() : 0L;
    }

    int messageIdOf(Object messageObject) {
        if (messageObject == null) return 0;
        Object val = bridge.invoke(messageObject, message.getId);
        return (val instanceof Number) ? ((Number) val).intValue() : 0;
    }

    boolean isVideoMessage(Object messageObject) {
        if (messageObject == null) return false;
        if (!isVideoIgnoringMask(messageObject)) return false;
        Object isLive = bridge.invoke(messageObject, message.isLivePhoto);
        if (Boolean.TRUE.equals(isLive)) return false;
        Object isRound = bridge.invoke(messageObject, message.isRoundVideo);
        return !Boolean.TRUE.equals(isRound);
    }

    private boolean isVideoIgnoringMask(Object messageObject) {
        Object owner = bridge.getField(messageObject, message.messageOwnerField);
        if (owner != null) {
            Object val = bridge.invoke(null, message.isVideoMessageStatic, owner);
            if (val instanceof Boolean) return (Boolean) val;
        }
        Object val = bridge.invoke(messageObject, message.isVideo);
        return Boolean.TRUE.equals(val);
    }

    String describeGaps() {
        List<String> gaps = new ArrayList<>();
        if (photo.videoItemField == null) gaps.add("PhotoViewer.videoItem");
        if (photo.videoPlayerField == null) gaps.add("PhotoViewer.videoPlayer");
        if (photo.closePhoto == null) gaps.add("PhotoViewer.closePhoto");
        if (photo.imagesArrField == null) gaps.add("PhotoViewer.imagesArr");
        if (media.injectVideoPlayer == null) gaps.add("MediaController.injectVideoPlayer");
        if (media.cleanupPlayer4 == null) gaps.add("MediaController.cleanupPlayer/4");
        if (media.playMessage == null) gaps.add("MediaController.playMessage");
        if (media.updateVideoState == null) gaps.add("MediaController.updateVideoState");
        if (media.canStartMusicPlayerService == null) gaps.add("MediaController.canStartMusicPlayerService");
        if (media.playlistField == null) gaps.add("MediaController.playlist");
        if (media.playlistMapField == null) gaps.add("MediaController.playlistMap");
        if (media.currentPlaylistNumField == null) gaps.add("MediaController.currentPlaylistNum");
        if (media.pipRoundVideoViewField == null) gaps.add("MediaController.pipRoundVideoView");
        if (player.setStreamType == null) gaps.add("VideoPlayer.setStreamType");
        if (player.setDelegate == null) gaps.add("VideoPlayer.setDelegate");
        if (pipRoundVideoViewClass == null) gaps.add("PipRoundVideoView");
        if (musicPlayerServiceClass == null) gaps.add("MusicPlayerService");
        if (message.isMusic == null) gaps.add("MessageObject.isMusic");
        if (musicService.createNotification == null) gaps.add("MusicPlayerService.createNotification");
        if (musicService.updatePlaybackState == null) gaps.add("MusicPlayerService.updatePlaybackState");
        if (getPeerName == null) gaps.add("MessagesController.getPeerName");
        if (message.isVideoMessageStatic == null) gaps.add("MessageObject.isVideoMessage(static)");
        if (getPeerDialogId == null) gaps.add("DialogObject.getPeerDialogId");
        if (tl.fwdFrom == null) gaps.add("TLRPC.Message.fwd_from");

        if (gaps.isEmpty()) return "无";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < gaps.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(gaps.get(i));
        }
        return sb.toString();
    }
}
