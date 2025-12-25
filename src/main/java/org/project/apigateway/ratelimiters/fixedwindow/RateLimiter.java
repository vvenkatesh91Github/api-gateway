package org.project.apigateway.ratelimiters.fixedwindow;

public interface RateLimiter {
    public boolean allowRequest();
}
