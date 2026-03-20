package com.example.network.infrastructure.adapter.out.redis;

import com.example.network.domain.port.out.BodySizePolicy;
import com.example.network.infrastructure.adapter.config.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * INFRASTRUCTURE — BodySizePolicyRedisAdapter (Output Adapter)
 *
 * Layer 4 — Request body size limit.
 * Implements BodySizePolicy using a Redis STRING.
 *
 * Redis operations:
 *   GET nm:ddos:body:size:limit         — read limit on every POST/PUT
 *   SET nm:ddos:body:size:limit {bytes} — operator changes at runtime
 *
 * Default: 1,048,576 bytes (1MB) when key is not set in Redis.
 * Change takes effect immediately on next request — no restart needed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BodySizePolicyRedisAdapter implements BodySizePolicy {

    private static final long DEFAULT_MAX_BYTES = 1_048_576L; // 1MB

    private final StringRedisTemplate redis;

    @Override
    public long getMaxBytes() {
        String val = redis.opsForValue().get(RedisKeys.DDOS_BODY_SIZE_LIMIT);
        if (val == null) {
            return DEFAULT_MAX_BYTES;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            log.warn("[Redis] Invalid body size limit value '{}', using default {}",
                    val, DEFAULT_MAX_BYTES);
            return DEFAULT_MAX_BYTES;
        }
    }

    @Override
    public void setMaxBytes(long bytes) {
        if (bytes <= 0) {
            throw new IllegalArgumentException("Body size limit must be positive, got: " + bytes);
        }
        redis.opsForValue().set(RedisKeys.DDOS_BODY_SIZE_LIMIT, String.valueOf(bytes));
        log.info("[Redis] SET {} {} — body size limit updated", RedisKeys.DDOS_BODY_SIZE_LIMIT, bytes);
    }
}