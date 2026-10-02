package android.content;

/** Records the destination passed to Android; does not simulate Activity routing. */
public class Intent {
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final int FLAG_ACTIVITY_CLEAR_TOP = 0x04000000;
    public static final int FLAG_ACTIVITY_SINGLE_TOP = 0x20000000;
    private String action;
    private int flags;
    private final java.util.Map<String, Number> extras = new java.util.HashMap<>();
    public Intent(Context context, Class<?> type) {}
    public Intent setAction(String value) { action = value; return this; }
    public String getAction() { return action; }
    public Intent putExtra(String key, int value) { extras.put(key, value); return this; }
    public Intent putExtra(String key, long value) { extras.put(key, value); return this; }
    public int getIntExtra(String key, int fallback) { return extras.containsKey(key) ? extras.get(key).intValue() : fallback; }
    public long getLongExtra(String key, long fallback) { return extras.containsKey(key) ? extras.get(key).longValue() : fallback; }
    public Intent addFlags(int value) { flags |= value; return this; }
    public int getFlags() { return flags; }
}
