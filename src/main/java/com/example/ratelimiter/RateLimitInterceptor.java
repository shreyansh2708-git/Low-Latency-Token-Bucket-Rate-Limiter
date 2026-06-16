package com.example.ratelimiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    public static final String RATE_LIMIT_REMAINING_HEADER = "X-Rate-Limit-Remaining";

    // Dynamically injected from application.properties (defaults to 5 if missing)
    @Value("${rate-limit.capacity:5}")
    private long maxBucketCapacity;

    @Value("${rate-limit.refill-rate-per-minute:5}")
    private long refillTokensPerMinute;

    private final ConcurrentHashMap<String, TokenBucket> bucketsByClientIp = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        String clientIp = request.getRemoteAddr();

        // Pass dynamic config into the bucket creation
        TokenBucket bucket = bucketsByClientIp.computeIfAbsent(
                clientIp,
                ip -> new TokenBucket(maxBucketCapacity, refillTokensPerMinute)
        );

        if (bucket.allowRequest()) {
            response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(bucket.getRemainingTokens()));
            return true;
        }

        // Dynamically format the JSON string so the error message matches the injected properties
        String jsonError = String.format(
                "{\"error\": \"Too Many Requests\", \"message\": \"You have exceeded your limit of %d requests per minute.\"}",
                maxBucketCapacity
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, "0");
        response.getWriter().write(jsonError);
        response.getWriter().flush();

        return false;
    }
}