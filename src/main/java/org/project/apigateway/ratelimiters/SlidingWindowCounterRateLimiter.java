package org.project.apigateway.ratelimiters;

public class SlidingWindowCounterRateLimiter implements RateLimiter {
    private final int limit;
    private final long windowSizeMillis;

    private long currentWindowStart;
    private int currentCount;
    private int previousCount;

    public SlidingWindowCounterRateLimiter(int limit, long windowSizeMillis) {
        this.limit = limit;
        this.windowSizeMillis = windowSizeMillis;
        this.currentWindowStart = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        long now = System.currentTimeMillis();
        long elapsed = now - currentWindowStart;

        if (elapsed >= windowSizeMillis) {
            previousCount = currentCount;
            currentCount = 0;
            currentWindowStart = now;
            elapsed = 0;
        }

        double weight = (double) (windowSizeMillis - elapsed) / windowSizeMillis;
        double effectiveCount = previousCount * weight + currentCount;

        if (effectiveCount >= limit) {
            return false;
        }

        currentCount++;
        return true;
    }
}
