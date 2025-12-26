package org.project.apigateway.ratelimiters;

import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowRateLimiter implements RateLimiter {
    private final int limit;
    private final long windowSizeMillis;

    private long windowStart;
    private final AtomicInteger counter = new AtomicInteger(0);

    public FixedWindowRateLimiter(int limit, long windowSizeMillis) {
        this.limit = limit;
        this.windowSizeMillis = windowSizeMillis;
        this.windowStart = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();

        if (now - windowStart >= windowSizeMillis) {
            windowStart = now;
            counter.set(0);
        }

        return counter.incrementAndGet() <= limit;
    }
}
