package android.os;

/** UI work is not executed here; lifecycle tests invoke the registered hooks directly. */
public class Handler {
    private static final java.util.List<Runnable> reopen = new java.util.ArrayList<>();
    public Handler(Looper looper) {}
    public boolean post(Runnable task) { return true; }
    public boolean postDelayed(Runnable task, long delay) {
        if (delay == 200L) reopen.add(task);
        return true;
    }
    public void removeCallbacks(Runnable task) {}
    public static void runReopen() { reopen.remove(0).run(); }
}
