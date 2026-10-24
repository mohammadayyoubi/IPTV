package com.example.iptv;

/**
 * App-wide state of the one long admin task that may run at a time (import, update,
 * server check, logo refresh). The dashboard observes it to lock its action rows until the
 * task is done, also when the dashboard is closed and reopened meanwhile. Main thread only.
 */
public final class AdminTasks {

    public interface Observer {
        /**
         * @param paused  waiting for the internet connection to come back
         * @param canStop the task can be stopped from the dashboard
         * @param percent overall completion 0..100, or -1 when unknown
         */
        void onTaskStateChanged(boolean running, String status, boolean paused, boolean canStop, int percent);
    }

    private static boolean running;
    private static boolean paused;
    private static String status = "";
    private static int percent = -1;
    private static Runnable stopAction;
    private static Observer observer;

    private AdminTasks() {}

    public static boolean isRunning() {
        return running;
    }

    /** @return false when another task is still running */
    public static boolean start(String initialStatus) {
        if (running) return false;
        running = true;
        paused = false;
        stopAction = null;
        status = initialStatus;
        percent = -1;
        notifyObserver();
        return true;
    }

    public static void update(String newStatus) {
        update(newStatus, false);
    }

    /** Keeps the last known percentage. */
    public static void update(String newStatus, boolean isPaused) {
        update(newStatus, isPaused, percent);
    }

    public static void update(String newStatus, boolean isPaused, int newPercent) {
        if (!running) return;
        status = newStatus;
        paused = isPaused;
        if (newPercent >= 0) percent = newPercent;
        notifyObserver();
    }

    /** Offers a Stop button on the dashboard while this task runs (null to remove it). */
    public static void setStopAction(Runnable action) {
        stopAction = action;
        notifyObserver();
    }

    public static void stop() {
        if (stopAction != null) {
            Runnable action = stopAction;
            stopAction = null;
            status = "Stopping…";
            notifyObserver();
            action.run();
        }
    }

    public static void finish() {
        running = false;
        paused = false;
        stopAction = null;
        status = "";
        percent = -1;
        notifyObserver();
    }

    /** Registers the visible dashboard (null to detach) and replays the current state. */
    public static void observe(Observer o) {
        observer = o;
        notifyObserver();
    }

    private static void notifyObserver() {
        if (observer != null) observer.onTaskStateChanged(running, status, paused, stopAction != null, percent);
    }
}
