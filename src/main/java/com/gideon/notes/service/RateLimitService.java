package com.gideon.notes.service;

import com.github.benmanes.caffeine.cache.LoadingCache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final LoadingCache<String, Bucket> cache;

    public Bucket resolveBucket(String key, RateLimitType type) {
        String rateLimitKey = key + ":" + type.name();
        return cache.get(rateLimitKey, k -> createBucket(type));
    }

    private Bucket createBucket(RateLimitType type) {
        Bandwidth limit = switch (type) {
            case AUTH -> Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
            case API -> Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
            case NOTES_CREATE -> Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1)));
            case NOTES_UPDATE -> Bandwidth.classic(30, Refill.intervally(30, Duration.ofMinutes(1)));
        };

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public enum RateLimitType {
        AUTH,
        API,
        NOTES_CREATE,
        NOTES_UPDATE
    }
}