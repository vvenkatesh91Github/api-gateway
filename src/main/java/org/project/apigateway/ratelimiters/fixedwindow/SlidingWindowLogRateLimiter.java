package org.project.apigateway.ratelimiters.fixedwindow;

import java.util.Deque;
import java.util.LinkedList;

public class SlidingWindowLogRateLimiter implements RateLimiter {
    private final int limit;
    private final long windowSizeMillis;
    private final Deque<Long> timestamps = new LinkedList<>();

    public SlidingWindowLogRateLimiter(int limit, long windowSizeMillis) {
        this.limit = limit;
        this.windowSizeMillis = windowSizeMillis;
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();

        while (!timestamps.isEmpty() && timestamps.peekFirst() <= now - windowSizeMillis) {
            timestamps.pollFirst();
        }

        if (timestamps.size() < limit) {
            timestamps.addLast(now);
            return true;
        }
        return false;
    }
}
