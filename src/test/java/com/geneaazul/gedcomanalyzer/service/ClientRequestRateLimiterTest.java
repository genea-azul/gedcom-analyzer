package com.geneaazul.gedcomanalyzer.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientRequestRateLimiterTest {

    private static final Duration WINDOW = Duration.ofHours(1);

    private static class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-26T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    public void allowsUpToMaxRequestsThenRejects() {
        ClientRequestRateLimiter limiter = new ClientRequestRateLimiter(new MutableClock(), ClientRequestRateLimiter.DEFAULT_MAX_TRACKED_KEYS);

        assertThat(limiter.tryAcquire("ip", 3, WINDOW)).isTrue();
        assertThat(limiter.tryAcquire("ip", 3, WINDOW)).isTrue();
        assertThat(limiter.tryAcquire("ip", 3, WINDOW)).isTrue();
        assertThat(limiter.tryAcquire("ip", 3, WINDOW)).isFalse();
    }

    @Test
    public void keysAreCountedIndependently() {
        ClientRequestRateLimiter limiter = new ClientRequestRateLimiter(new MutableClock(), ClientRequestRateLimiter.DEFAULT_MAX_TRACKED_KEYS);

        assertThat(limiter.tryAcquire("a", 1, WINDOW)).isTrue();
        assertThat(limiter.tryAcquire("a", 1, WINDOW)).isFalse();
        assertThat(limiter.tryAcquire("b", 1, WINDOW)).isTrue();
    }

    @Test
    public void requestsOutsideTheWindowStopCounting() {
        MutableClock clock = new MutableClock();
        ClientRequestRateLimiter limiter = new ClientRequestRateLimiter(clock, ClientRequestRateLimiter.DEFAULT_MAX_TRACKED_KEYS);

        assertThat(limiter.tryAcquire("ip", 2, WINDOW)).isTrue();
        clock.advance(Duration.ofMinutes(30));
        assertThat(limiter.tryAcquire("ip", 2, WINDOW)).isTrue();
        assertThat(limiter.tryAcquire("ip", 2, WINDOW)).isFalse();

        // First request expires; only the one from minute 30 remains in the window
        clock.advance(Duration.ofMinutes(31));
        assertThat(limiter.tryAcquire("ip", 2, WINDOW)).isTrue();
        assertThat(limiter.tryAcquire("ip", 2, WINDOW)).isFalse();
    }

    @Test
    public void rejectedRequestsDoNotExtendTheBlock() {
        MutableClock clock = new MutableClock();
        ClientRequestRateLimiter limiter = new ClientRequestRateLimiter(clock, ClientRequestRateLimiter.DEFAULT_MAX_TRACKED_KEYS);

        assertThat(limiter.tryAcquire("ip", 1, WINDOW)).isTrue();
        for (int i = 0; i < 10; i++) {
            clock.advance(Duration.ofMinutes(5));
            assertThat(limiter.tryAcquire("ip", 1, WINDOW)).isFalse();
        }
        clock.advance(Duration.ofMinutes(11));
        assertThat(limiter.tryAcquire("ip", 1, WINDOW)).isTrue();
    }

    @Test
    public void purgeDropsKeysWithNoRequestsInTheWindow() {
        MutableClock clock = new MutableClock();
        ClientRequestRateLimiter limiter = new ClientRequestRateLimiter(clock, ClientRequestRateLimiter.DEFAULT_MAX_TRACKED_KEYS);

        limiter.tryAcquire("old", 5, WINDOW);
        clock.advance(Duration.ofMinutes(45));
        limiter.tryAcquire("recent", 5, WINDOW);
        clock.advance(Duration.ofMinutes(20));

        limiter.purgeExpired();

        assertThat(limiter.trackedKeys()).isEqualTo(1);
        assertThat(limiter.tryAcquire("old", 1, WINDOW)).isTrue();
    }

    @Test
    public void concurrentRequestsNeverExceedTheLimit() throws Exception {
        ClientRequestRateLimiter limiter = new ClientRequestRateLimiter(new MutableClock(), ClientRequestRateLimiter.DEFAULT_MAX_TRACKED_KEYS);
        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(16)) {
            for (int i = 0; i < 500; i++) {
                executor.submit(() -> {
                    start.await();
                    if (limiter.tryAcquire("ip", 50, WINDOW)) {
                        allowed.incrementAndGet();
                    }
                    if (limiter.trackedKeys() > 0) {
                        limiter.purgeExpired();
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(allowed.get()).isEqualTo(50);
    }

    @Test
    public void trackedKeysAreBoundedAndOverflowFailsOpen() {
        ClientRequestRateLimiter limiter = new ClientRequestRateLimiter(new MutableClock(), 2);

        assertThat(limiter.tryAcquire("a", 1, WINDOW)).isTrue();
        assertThat(limiter.tryAcquire("b", 1, WINDOW)).isTrue();
        assertThat(limiter.tryAcquire("c", 1, WINDOW)).isTrue();
        assertThat(limiter.tryAcquire("c", 1, WINDOW)).isTrue();
        assertThat(limiter.trackedKeys()).isEqualTo(2);

        // Keys already tracked are still limited
        assertThat(limiter.tryAcquire("a", 1, WINDOW)).isFalse();
    }

    @Test
    public void purgeKeepsTimestampsInsideTheLongestWindowSeen() {
        MutableClock clock = new MutableClock();
        ClientRequestRateLimiter limiter = new ClientRequestRateLimiter(clock, ClientRequestRateLimiter.DEFAULT_MAX_TRACKED_KEYS);

        limiter.tryAcquire("long", 5, Duration.ofHours(2));
        limiter.tryAcquire("short", 5, Duration.ofMinutes(10));
        clock.advance(Duration.ofMinutes(90));

        limiter.purgeExpired();

        assertThat(limiter.trackedKeys()).isEqualTo(2);
    }

}
