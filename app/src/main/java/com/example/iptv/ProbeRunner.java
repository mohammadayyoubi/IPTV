package com.example.iptv;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Runs many stream probes in parallel without ever blaming a stream for the phone's own
 * connection problems.
 *
 * <p>How a failure is judged:
 * <ol>
 *   <li>A success is final: it proves the connection worked.</li>
 *   <li>A failure is only <i>tentative</i>. It becomes final ("broken") only after a later
 *       internet health probe ({@link NetworkMonitor#probeInternet}) succeeds <b>and</b> no outage
 *       was seen since that stream's probe started.</li>
 *   <li>If the health probe fails or is too slow, or Android reports the network lost, the run
 *       <b>pauses</b>: workers stop taking new streams, tentative failures go back into the queue,
 *       and the run resumes by itself once a health probe is good again.</li>
 * </ol>
 */
public class ProbeRunner<T> {

    public enum Outcome {
        /** Plays */
        OK,
        /** The server answered, but not with a stream (HTTP error, HTML page, ...) */
        BROKEN,
        /** No answer at all: timeout, DNS failure, connection refused... maybe our connection */
        NETWORK_ERROR
    }

    public interface Probe<T> {
        Outcome probe(T item);
    }

    /** Where connection health comes from; swapped for a fake in unit tests. */
    public interface Connectivity {
        boolean hasValidatedNetwork();
        NetworkMonitor.Health probeInternet();
        NetworkMonitor.Watch watch(NetworkMonitor.Listener listener);
    }

    /** The real phone connection. */
    public static Connectivity systemConnectivity(Context context) {
        Context app = context.getApplicationContext();
        return new Connectivity() {
            @Override public boolean hasValidatedNetwork() { return NetworkMonitor.hasValidatedNetwork(app); }
            @Override public NetworkMonitor.Health probeInternet() { return NetworkMonitor.probeInternet(); }
            @Override public NetworkMonitor.Watch watch(NetworkMonitor.Listener l) { return NetworkMonitor.watch(app, l); }
        };
    }

    /** Called on the coordinating (caller's) thread. */
    public interface Sink<T> {
        void onResult(T item, boolean working);
        /** After each batch of results: a good moment to write them out. */
        void onBatch(int done, int total);
        void onPaused(String reason);
        void onResumed();
    }

    private static final String TAG = "ProbeRunner";
    private static final int THREADS = 32;
    private static final long TICK_MS = 700;
    private static final long MIN_HEALTH_INTERVAL_MS = 2_000;
    private static final long MAX_HEALTH_INTERVAL_MS = 15_000;
    private static final long RECOVERY_POLL_MS = 3_000;

    // Only one run at a time; the dashboard's Stop button cancels it
    private static volatile ProbeRunner<?> active;

    private final Connectivity connectivity;
    private final Probe<T> probe;
    private final Sink<T> sink;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    private final ConcurrentLinkedDeque<T> pending = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedQueue<T> successes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Tentative<T>> failures = new ConcurrentLinkedQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final Object gateLock = new Object();
    private final Object wakeLock = new Object();
    private volatile boolean gateOpen = true;
    private volatile boolean finished;
    private volatile boolean connectionLost;
    private volatile long lastOutageAt;

    private static class Tentative<T> {
        final T item;
        final long startedAt;

        Tentative(T item, long startedAt) {
            this.item = item;
            this.startedAt = startedAt;
        }
    }

    public ProbeRunner(Context context, Probe<T> probe, Sink<T> sink) {
        this(systemConnectivity(context), probe, sink);
    }

    ProbeRunner(Connectivity connectivity, Probe<T> probe, Sink<T> sink) {
        this.connectivity = connectivity;
        this.probe = probe;
        this.sink = sink;
    }

    /** Stops the running probe run, if any (results so far are kept). */
    public static void cancelActive() {
        ProbeRunner<?> runner = active;
        if (runner != null) runner.cancel();
    }

    public void cancel() {
        cancelled.set(true);
        openGate();
        synchronized (wakeLock) {
            wakeLock.notifyAll();
        }
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    /** Blocking. Returns when every item has a final result, or when cancelled. */
    public void run(List<T> items) {
        active = this;
        pending.addAll(items);
        int total = items.size();
        int done = 0;

        NetworkMonitor.Watch watch = connectivity.watch(new NetworkMonitor.Listener() {
            @Override
            public void onConnectionLost() {
                // Stop handing out work at once; the coordinator confirms and pauses
                connectionLost = true;
                lastOutageAt = System.currentTimeMillis();
                closeGate();
                wake();
            }

            @Override
            public void onConnectionAvailable() {
                wake();
            }
        });

        Thread[] workers = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            workers[i] = new Thread(this::workerLoop, "probe-" + i);
            workers[i].start();
        }

        long lastHealthAt = System.currentTimeMillis();
        try {
            while (!cancelled.get()) {
                sleepTick();

                // 1. Successes are final
                T ok;
                while ((ok = successes.poll()) != null) {
                    sink.onResult(ok, true);
                    done++;
                }

                // 2. Failures wait for a health probe that proves our connection is fine
                long now = System.currentTimeMillis();
                boolean dueHealth = connectionLost
                        || (!failures.isEmpty() && now - lastHealthAt >= MIN_HEALTH_INTERVAL_MS)
                        || now - lastHealthAt >= MAX_HEALTH_INTERVAL_MS;
                if (dueHealth) {
                    NetworkMonitor.Health health = connectivity.probeInternet();
                    lastHealthAt = System.currentTimeMillis();
                    if (!health.isGood()) {
                        pauseUntilHealthy(health);
                        lastHealthAt = System.currentTimeMillis();
                        if (cancelled.get()) break;
                    } else {
                        // e.g. Wi-Fi → mobile handover: the network changed but internet works
                        connectionLost = false;
                        openGate();
                        Tentative<T> f;
                        int requeued = 0;
                        while ((f = failures.poll()) != null) {
                            if (f.startedAt > lastOutageAt) {
                                sink.onResult(f.item, false);
                                done++;
                            } else {
                                // Probed during an outage: inconclusive, try again
                                pending.addFirst(f.item);
                                requeued++;
                            }
                        }
                        if (requeued > 0) Log.d(TAG, "Re-queued " + requeued + " probes from an outage");
                    }
                }

                sink.onBatch(done, total);
                if (pending.isEmpty() && inFlight.get() == 0 && successes.isEmpty() && failures.isEmpty()) {
                    break;
                }
            }
        } finally {
            finished = true;
            openGate();
            watch.stop();
            for (Thread worker : workers) {
                try {
                    worker.join(TimeUnit.SECONDS.toMillis(15));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // Successes that landed while stopping are still real results
            T ok;
            while ((ok = successes.poll()) != null) {
                sink.onResult(ok, true);
                done++;
            }
            sink.onBatch(done, total);
            if (active == this) active = null;
        }
    }

    private void pauseUntilHealthy(NetworkMonitor.Health health) {
        closeGate();
        lastOutageAt = System.currentTimeMillis();
        // Everything that failed so far may have failed because of us: retry it later
        Tentative<T> f;
        while ((f = failures.poll()) != null) pending.addFirst(f.item);

        String reason = health.online ? "Connection too slow" : "No internet connection";
        Log.w(TAG, "Pausing: " + reason);
        sink.onPaused(reason);
        while (!cancelled.get()) {
            synchronized (wakeLock) {
                try {
                    wakeLock.wait(RECOVERY_POLL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            if (cancelled.get()) return;
            if (!connectivity.hasValidatedNetwork()) continue;
            NetworkMonitor.Health retry = connectivity.probeInternet();
            if (retry.isGood()) break;
            sink.onPaused(retry.online ? "Connection too slow" : "No internet connection");
        }
        // Probes still in flight started before / during the outage: their failures get re-queued
        lastOutageAt = System.currentTimeMillis();
        connectionLost = false;
        Log.i(TAG, "Connection is back, resuming");
        sink.onResumed();
        openGate();
    }

    private void workerLoop() {
        while (!finished && !cancelled.get()) {
            awaitGate();
            if (finished || cancelled.get()) return;
            T item = pending.poll();
            if (item == null) {
                sleepQuietly(50);
                continue;
            }
            inFlight.incrementAndGet();
            try {
                long startedAt = System.currentTimeMillis();
                Outcome outcome = probe.probe(item);
                // A network-type failure gets one more chance before it counts
                if (outcome == Outcome.NETWORK_ERROR && gateOpen && !cancelled.get()) {
                    sleepQuietly(400);
                    if (gateOpen) outcome = probe.probe(item);
                }
                if (outcome == Outcome.OK) {
                    successes.add(item);
                } else {
                    failures.add(new Tentative<>(item, startedAt));
                }
            } finally {
                inFlight.decrementAndGet();
            }
        }
    }

    private void awaitGate() {
        synchronized (gateLock) {
            while (!gateOpen && !finished && !cancelled.get()) {
                try {
                    gateLock.wait(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void closeGate() {
        gateOpen = false;
    }

    private void openGate() {
        synchronized (gateLock) {
            gateOpen = true;
            gateLock.notifyAll();
        }
    }

    private void wake() {
        synchronized (wakeLock) {
            wakeLock.notifyAll();
        }
    }

    private void sleepTick() {
        synchronized (wakeLock) {
            try {
                wakeLock.wait(TICK_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                cancelled.set(true);
            }
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
