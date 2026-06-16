package com.example.ratelimiter;

/**
 * Core implementation of the Token Bucket rate-limiting algorithm.
 * Uses high-precision millisecond math to ensure exact refill times.
 */
public class TokenBucket {

    private final long capacity;
    private final long refillTokensPerMinute;
    private long tokens;
    private long lastRefillTimeMillis;

    public TokenBucket(long capacity, long refillTokensPerMinute) {
        this.capacity = capacity;
        this.refillTokensPerMinute = refillTokensPerMinute;
        this.tokens = capacity; // Start full
        this.lastRefillTimeMillis = System.currentTimeMillis();
    }

    public synchronized boolean allowRequest() {
        refill();

        if (tokens > 0) {
            tokens--;
            return true;
        }

        return false;
    }

    public synchronized long getRemainingTokens() {
        refill();
        return tokens;
    }

    private void refill() {
        long nowMillis = System.currentTimeMillis();
        long timeElapsedMillis = nowMillis - lastRefillTimeMillis;

        // How many milliseconds does it take to generate exactly 1 token?
        long millisPerToken = 60000 / refillTokensPerMinute;

        // Calculate whole tokens generated in the elapsed time
        long tokensToAdd = timeElapsedMillis / millisPerToken;

        if (tokensToAdd > 0) {
            tokens = Math.min(capacity, tokens + tokensToAdd);
            
            // Advance the clock ONLY by the exact milliseconds consumed.
            // This preserves leftover fractions of a millisecond for the next request.
            lastRefillTimeMillis += (tokensToAdd * millisPerToken);
        }
    }
}