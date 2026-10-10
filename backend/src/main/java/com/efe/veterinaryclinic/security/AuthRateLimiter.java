package com.efe.veterinaryclinic.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuthRateLimiter {

    private final long capacity;
    private final long refillPeriodSeconds;
    private final Map<String, Bucket> bucketsByIp = new ConcurrentHashMap<>();

    public AuthRateLimiter(
            @Value("${app.rate-limit.auth.capacity}") long capacity,
            @Value("${app.rate-limit.auth.refill-period-seconds}") long refillPeriodSeconds) {

        this.capacity = capacity;
        this.refillPeriodSeconds = refillPeriodSeconds;
    }

    public Bucket resolveBucket(String ipAddress) {
        return bucketsByIp.computeIfAbsent(
                ipAddress,
                ip -> newBucket()
        );
    }

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(
                        capacity,
                        Duration.ofSeconds(refillPeriodSeconds)
                )
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
