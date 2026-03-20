package com.example.network.infrastructure.adapter.out.redis;

import com.example.network.domain.port.out.ReputationRepository;
import com.example.network.infrastructure.adapter.config.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * INFRASTRUCTURE — ReputationRedisAdapter (Output Adapter)
 *
 * Implements ReputationRepository using Redis SET.
 *
 * Redis operations used:
 *   SISMEMBER nm:ddos:reputation {ip}  — O(1) membership check
 *   SADD      nm:ddos:reputation {ip}  — add IP to set
 *   SREM      nm:ddos:reputation {ip}  — remove IP from set
 *   SMEMBERS  nm:ddos:reputation       — get all IPs (for admin/debug)
 *
 * Uses StringRedisTemplate (blocking) — fine with JDK 21 virtual threads.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReputationRedisAdapter implements ReputationRepository {

    private final StringRedisTemplate redis;

    /**
     * Check if IP is in the reputation list.
     *
     * Redis: SISMEMBER nm:ddos:reputation {ip}
     * Time complexity: O(1) — same speed for 100 or 100,000 IPs
     * Latency: ~0.3ms local Redis
     */
    @Override
    public boolean contains(String ip) {
        Boolean result = redis.opsForSet().isMember(RedisKeys.DDOS_REPUTATION, ip);
        log.debug("[Redis] SISMEMBER {} {} → {}", RedisKeys.DDOS_REPUTATION, ip, result);
        return Boolean.TRUE.equals(result);
    }

    /**
     * Add IP to the reputation list.
     *
     * Redis: SADD nm:ddos:reputation {ip}
     * In production: called by scheduled feed sync job every 24h.
     * In testing: called by admin REST endpoint.
     */
    @Override
    public void add(String ip) {
        redis.opsForSet().add(RedisKeys.DDOS_REPUTATION, ip);
        log.info("[Redis] SADD {} {} — IP added to reputation list", RedisKeys.DDOS_REPUTATION, ip);
    }

    /**
     * Remove IP from the reputation list.
     *
     * Redis: SREM nm:ddos:reputation {ip}
     */
    @Override
    public void remove(String ip) {
        redis.opsForSet().remove(RedisKeys.DDOS_REPUTATION, ip);
        log.info("[Redis] SREM {} {} — IP removed from reputation list", RedisKeys.DDOS_REPUTATION, ip);
    }

    /**
     * Get all IPs currently in the reputation list.
     *
     * Redis: SMEMBERS nm:ddos:reputation
     * Used by admin status endpoint — never called on hot path.
     */
    @Override
    public Set<String> getAll() {
        Set<String> members = redis.opsForSet().members(RedisKeys.DDOS_REPUTATION);
        return members != null ? members : Set.of();
    }
}