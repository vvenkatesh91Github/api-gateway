package org.project.apigateway.ratelimiters;

public interface RateLimiter {
    public boolean allowRequest();
}
