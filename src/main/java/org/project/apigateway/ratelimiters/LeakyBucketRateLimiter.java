package org.project.apigateway.ratelimiters;

import java.util.concurrent.*;

public class LeakyBucketRateLimiter implements RateLimiter {
    private final int capacity;
    private final BlockingQueue<Long> queue;
    int leakRatePerSecond;

    public LeakyBucketRateLimiter(int capacity, int leakRatePerSecond) {
        this.capacity = capacity;
        this.leakRatePerSecond = leakRatePerSecond;
        this.queue = new LinkedBlockingQueue<Long>(capacity);
        this.leak();
    }

    public boolean allowRequest() {
        return queue.offer(System.currentTimeMillis());
    }

    public void leak() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long delayInMs = 1000 / leakRatePerSecond;

        scheduler.scheduleAtFixedRate(() -> {
            Long req = queue.poll();
            if (req != null) {
                // Leaked one request
            }
        }, 0, delayInMs, TimeUnit.MILLISECONDS);
    }
}
