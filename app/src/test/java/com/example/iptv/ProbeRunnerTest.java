package com.example.iptv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ProbeRunner must never call a stream broken because the phone itself lost its connection.
 * A fake connection goes offline in the middle of a run: every probe fails while it is down.
 */
public class ProbeRunnerTest {

    /** Phone connection that can be switched off and on. */
    private static class FakeConnection implements ProbeRunner.Connectivity {
        final AtomicBoolean online = new AtomicBoolean(true);
        volatile NetworkMonitor.Listener listener;

        @Override public boolean hasValidatedNetwork() { return online.get(); }

        @Override public NetworkMonitor.Health probeInternet() {
            return online.get() ? new NetworkMonitor.Health(true, 50) : new NetworkMonitor.Health(false, Long.MAX_VALUE);
        }

        @Override public NetworkMonitor.Watch watch(NetworkMonitor.Listener l) {
            listener = l;
            return () -> listener = null;
        }

        void setOnline(boolean value) {
            online.set(value);
            NetworkMonitor.Listener l = listener;
            if (l != null) {
                if (value) l.onConnectionAvailable();
                else l.onConnectionLost();
            }
        }
    }

    private static class RecordingSink implements ProbeRunner.Sink<Integer> {
        final Map<Integer, Boolean> results = new ConcurrentHashMap<>();
        final AtomicInteger duplicates = new AtomicInteger();
        final AtomicInteger pauses = new AtomicInteger();
        final AtomicInteger resumes = new AtomicInteger();

        @Override public void onResult(Integer item, boolean working) {
            if (results.put(item, working) != null) duplicates.incrementAndGet();
        }
        @Override public void onBatch(int done, int total) { }
        @Override public void onPaused(String reason) { pauses.incrementAndGet(); }
        @Override public void onResumed() { resumes.incrementAndGet(); }
    }

    private static List<Integer> items(int n) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < n; i++) list.add(i);
        return list;
    }

    /** Even ids are real working streams, odd ids are really broken. */
    private static boolean reallyWorks(int id) {
        return id % 2 == 0;
    }

    @Test(timeout = 60_000)
    public void outageInTheMiddle_neverMarksWorkingStreamsBroken() throws Exception {
        FakeConnection net = new FakeConnection();
        RecordingSink sink = new RecordingSink();
        ProbeRunner<Integer> runner = new ProbeRunner<>(net, id -> {
            sleep(20);
            if (!net.online.get()) return ProbeRunner.Outcome.NETWORK_ERROR;
            return reallyWorks(id) ? ProbeRunner.Outcome.OK : ProbeRunner.Outcome.BROKEN;
        }, sink);

        // Drop the connection shortly after start, restore it a few seconds later
        Thread outage = new Thread(() -> {
            sleep(300);
            net.setOnline(false);
            sleep(4_000);
            net.setOnline(true);
        });
        outage.start();
        runner.run(items(1500));
        outage.join();

        assertEquals("every stream gets exactly one final result", 1500, sink.results.size());
        assertEquals(0, sink.duplicates.get());
        for (Map.Entry<Integer, Boolean> e : sink.results.entrySet()) {
            assertEquals("stream " + e.getKey(), reallyWorks(e.getKey()), e.getValue());
        }
        assertTrue("the run paused during the outage", sink.pauses.get() >= 1);
        assertTrue("and resumed afterwards", sink.resumes.get() >= 1);
    }

    @Test(timeout = 60_000)
    public void silentOutage_detectedByHealthProbe() throws Exception {
        // No "connection lost" callback (e.g. Wi-Fi up but the router lost its uplink)
        FakeConnection net = new FakeConnection() {
            @Override void setOnline(boolean value) { online.set(value); }
        };
        RecordingSink sink = new RecordingSink();
        ProbeRunner<Integer> runner = new ProbeRunner<>(net, id -> {
            sleep(15);
            if (!net.online.get()) return ProbeRunner.Outcome.NETWORK_ERROR;
            return reallyWorks(id) ? ProbeRunner.Outcome.OK : ProbeRunner.Outcome.BROKEN;
        }, sink);

        Thread outage = new Thread(() -> {
            sleep(200);
            net.setOnline(false);
            sleep(5_000);
            net.setOnline(true);
        });
        outage.start();
        runner.run(items(1500));
        outage.join();

        assertEquals(1500, sink.results.size());
        for (Map.Entry<Integer, Boolean> e : sink.results.entrySet()) {
            assertEquals("stream " + e.getKey(), reallyWorks(e.getKey()), e.getValue());
        }
        assertTrue(sink.pauses.get() >= 1);
    }

    @Test(timeout = 30_000)
    public void stop_endsTheRunAndKeepsOnlyFinalResults() throws Exception {
        FakeConnection net = new FakeConnection();
        RecordingSink sink = new RecordingSink();
        ProbeRunner<Integer> runner = new ProbeRunner<>(net, id -> {
            sleep(30);
            return ProbeRunner.Outcome.OK;
        }, sink);
        Thread stopper = new Thread(() -> {
            sleep(500);
            runner.cancel();
        });
        stopper.start();
        runner.run(items(100_000));
        stopper.join();

        assertTrue(runner.isCancelled());
        assertFalse("stopped long before the end", sink.results.size() >= 100_000);
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
