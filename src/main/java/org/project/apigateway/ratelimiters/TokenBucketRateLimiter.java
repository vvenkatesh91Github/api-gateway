package org.project.apigateway.ratelimiters;

public class TokenBucketRateLimiter implements RateLimiter {
    private final int capacity;
    private final double refillRatePerMillis;

    private double tokens;
    private long lastRefillTimestamp;

    public TokenBucketRateLimiter(int capacity, double refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerMillis = refillRatePerSecond / 1000.0;
        this.tokens = capacity;
        this.lastRefillTimestamp = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        refill();

        if (tokens >= 1) {
            tokens--;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTimestamp;

        double refillTokens = elapsed * refillRatePerMillis;
        tokens = Math.min(capacity, tokens + refillTokens);
        lastRefillTimestamp = now;
    }
}
