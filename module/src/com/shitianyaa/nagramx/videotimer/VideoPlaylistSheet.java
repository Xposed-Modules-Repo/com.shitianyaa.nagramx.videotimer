package com.shitianyaa.nagramx.videotimer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

/** Video rows use the host's thumbnail loader, rather than audio player cells. */
final class VideoPlaylistSheet {
    interface Selection { boolean open(Object message); }
    private final HostProfile host;
    private final HostBridge bridge;
    private final HostTheme theme;
    private final HostStrings strings;
    private final HostBridge.Logger logger;

    VideoPlaylistSheet(HostProfile host, HostTheme theme, HostStrings strings, HostBridge.Logger logger) {
        this.host = host;
        this.bridge = host.bridge;
        this.theme = theme;
        this.strings = strings;
        this.logger = logger;
    }

    void show(Activity activity, List<Object> messages, Object current, Selection selection) {
        try {
            Class<?> type = bridge.loadClass("org.telegram.ui.ActionBar.BottomSheet");
            Object object = bridge.newInstance(type, (Context) activity, false, theme.getDarkProvider());
            if (!(object instanceof Dialog)) throw new IllegalStateException("Host BottomSheet unavailable");
            Dialog sheet = (Dialog) object;
            LinearLayout content = new LinearLayout(activity);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(dp(activity, 16), dp(activity, 18), dp(activity, 16), dp(activity, 16));
            TextView heading = text(activity, strings.get(activity, "VideoPlaylist"), 20, true);
            content.addView(heading, new LinearLayout.LayoutParams(-1, dp(activity, 44)));
            ScrollView scroll = new ScrollView(activity);
            LinearLayout rows = new LinearLayout(activity);
            rows.setOrientation(LinearLayout.VERTICAL);
            for (Object message : messages) {
                if (!host.isVideoMessage(message)) continue;
                boolean playing = host.dialogIdOf(message) == host.dialogIdOf(current)
                        && host.messageIdOf(message) == host.messageIdOf(current);
                LinearLayout row = new LinearLayout(activity);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(activity, 8), 0, dp(activity, 8));
                Object document = bridge.invoke(message, host.message.getDocument);
                Object thumbObject = bridge.newInstance("org.telegram.ui.Components.BackupImageView", (Context) activity);
                if (!(thumbObject instanceof View)) throw new IllegalStateException("Host thumbnail view unavailable");
                View thumbnail = (View) thumbObject;
                thumbnail.setBackgroundColor(0xFF343434);
                bridge.invokeNamed(thumbnail, "setRoundRadius", dp(activity, 8));
                Object thumbs = bridge.getField(document, "thumbs");
                Object thumb = bridge.invokeStatic(bridge.loadClass("org.telegram.messenger.FileLoader"),
                        "getClosestPhotoSizeWithSize", thumbs, 160);
                Object location = bridge.invokeStatic(bridge.loadClass("org.telegram.messenger.ImageLocation"),
                        "getForDocument", thumb, document);
                if (location != null) bridge.invokeNamed(thumbnail, "setImage", location, "96_64", null, 0, message);
                else logger.log("视频列表缩略图不可用：message=" + host.messageIdOf(message), null);
                row.addView(thumbnail, new LinearLayout.LayoutParams(dp(activity, 96), dp(activity, 64)));
                LinearLayout labels = new LinearLayout(activity);
                labels.setOrientation(LinearLayout.VERTICAL);
                labels.setPadding(dp(activity, 14), 0, 0, 0);
                TextView title = text(activity, title(message, document), 16, false);
                title.setMaxLines(2);
                title.setEllipsize(TextUtils.TruncateAt.END);
                labels.addView(title, new LinearLayout.LayoutParams(-1, -2));
                String duration = duration(message);
                String details = duration + " · " + strings.get(activity,
                        isLocal(message) ? "VideoStoredLocally" : "VideoNotStoredLocally");
                labels.addView(text(activity, playing ? strings.get(activity, "VideoPlaying") + " · " + details : details,
                        13, playing), new LinearLayout.LayoutParams(-1, -2));
                row.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
                row.setContentDescription(title.getText() + ", " + duration);
                row.setOnClickListener(view -> {
                    sheet.dismiss();
                    if (!selection.open(message)) Toast.makeText(activity,
                            strings.get(activity, "VideoPlaylistOpenFailed"), Toast.LENGTH_SHORT).show();
                });
                rows.addView(row, new LinearLayout.LayoutParams(-1, dp(activity, 88)));
            }
            scroll.addView(rows);
            int height = Math.min(dp(activity, 440), messages.size() * dp(activity, 88));
            content.addView(scroll, new LinearLayout.LayoutParams(-1, height));
            if (bridge.findMethod(type, "setCustomView", 1) == null) throw new IllegalStateException("Host setCustomView unavailable");
            bridge.invokeNamed(sheet, "setCustomView", content);
            bridge.invokeNamed(sheet, "fixNavigationBar");
            sheet.show();
        } catch (Throwable error) {
            logger.log("打开视频列表失败", error);
            Toast.makeText(activity, strings.get(activity, "VideoPlaylistOpenFailed"), Toast.LENGTH_SHORT).show();
        }
    }

    private String title(Object message, Object document) {
        Object caption = bridge.getField(message, host.message.captionField);
        if (caption instanceof CharSequence && ((CharSequence) caption).length() > 0) return caption.toString();
        Object name = bridge.invokeStatic(bridge.loadClass("org.telegram.messenger.FileLoader"), "getDocumentFileName", document);
        if (name instanceof String && !((String) name).isEmpty()) return (String) name;
        return "Video · " + host.messageIdOf(message);
    }

    private String duration(Object message) {
        Object value = bridge.invoke(message, host.message.getDuration);
        int seconds = value instanceof Number ? Math.max(0, ((Number) value).intValue()) : 0;
        return String.format(java.util.Locale.US, "%d:%02d", seconds / 60, seconds % 60);
    }

    private boolean isLocal(Object message) {
        Object owner = bridge.getField(message, host.message.messageOwnerField);
        Object attachPath = bridge.getField(owner, "attachPath");
        if (attachPath instanceof String && !((String) attachPath).isEmpty()
                && new java.io.File((String) attachPath).isFile()) return true;
        Object account = bridge.getField(message, host.message.currentAccountField);
        Object loader = bridge.invokeStatic(bridge.loadClass("org.telegram.messenger.FileLoader"), "getInstance",
                account instanceof Number ? ((Number) account).intValue() : 0);
        Object file = owner == null ? null : bridge.invokeNamed(loader, "getPathToMessage", owner, false, false);
        if (file instanceof java.io.File && ((java.io.File) file).isFile()) return true;
        Object document = bridge.invoke(message, host.message.getDocument);
        for (boolean cache : new boolean[]{false, true}) {
            Object documentFile = bridge.invokeNamed(loader, "getPathToAttach", document, cache);
            if (documentFile instanceof java.io.File && ((java.io.File) documentFile).isFile()) return true;
        }
        return false;
    }

    private TextView text(Context context, String value, int size, boolean accent) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(accent ? 0xFF73B4EC : 0xFFFAFAFA);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private int dp(Context context, int value) { return theme.dp(context, value); }
}
