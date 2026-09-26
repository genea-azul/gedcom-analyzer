package com.geneaazul.gedcomanalyzer.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sliding-window, per-key request counter kept in memory. Used for requests that are not
 * persisted and therefore cannot be counted from the database like regular searches.
 * Counts are lost on restart, which is acceptable for abuse protection.
 */
@Component
public class ClientRequestRateLimiter {

    // Bounds memory: beyond this many clients in the window, new keys are allowed without being tracked
    static final int DEFAULT_MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Deque<Instant>> requestsByKey = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int maxTrackedKeys;
    // Longest window requested so far; keys with no timestamp inside it can be dropped
    private final AtomicReference<Duration> longestWindow = new AtomicReference<>(Duration.ZERO);

    public ClientRequestRateLimiter() {
        this(Clock.systemUTC(), DEFAULT_MAX_TRACKED_KEYS);
    }

    ClientRequestRateLimiter(Clock clock, int maxTrackedKeys) {
        this.clock = clock;
        this.maxTrackedKeys = maxTrackedKeys;
    }

    /**
     * Records a request for {@code key} and returns whether it is within {@code maxRequests}
     * in the trailing {@code window}. Rejected requests are not recorded.
     */
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        longestWindow.accumulateAndGet(window, (a, b) -> a.compareTo(b) >= 0 ? a : b);
        if (requestsByKey.size() >= maxTrackedKeys && !requestsByKey.containsKey(key)) {
            return true;
        }
        Instant now = clock.instant();
        Instant windowStart = now.minus(window);
        AtomicBoolean allowed = new AtomicBoolean();

        // compute() is atomic per key, so a concurrent purge cannot orphan the deque being updated
        requestsByKey.compute(key, (_, timestamps) -> {
            Deque<Instant> deque = timestamps != null ? timestamps : new ArrayDeque<>();
            dropOlderThan(deque, windowStart);
            if (deque.size() < maxRequests) {
                deque.addLast(now);
                allowed.set(true);
            }
            return deque.isEmpty() ? null : deque;
        });

        return allowed.get();
    }

    @Scheduled(fixedDelayString = "PT10M", initialDelayString = "PT10M")
    public void purgeExpired() {
        Instant windowStart = clock.instant().minus(longestWindow.get());
        for (String key : requestsByKey.keySet()) {
            requestsByKey.computeIfPresent(key, (_, deque) -> {
                dropOlderThan(deque, windowStart);
                return deque.isEmpty() ? null : deque;
            });
        }
    }

    int trackedKeys() {
        return requestsByKey.size();
    }

    private static void dropOlderThan(Deque<Instant> deque, Instant windowStart) {
        while (!deque.isEmpty() && !deque.peekFirst().isAfter(windowStart)) {
            deque.pollFirst();
        }
    }

}
