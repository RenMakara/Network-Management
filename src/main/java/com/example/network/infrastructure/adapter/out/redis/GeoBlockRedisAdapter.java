package com.example.network.infrastructure.adapter.out.redis;

import com.example.network.domain.port.out.GeoBlockRepository;
import com.example.network.infrastructure.adapter.config.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * INFRASTRUCTURE — GeoBlockRedisAdapter (Output Adapter)
 *
 * Layer 3 — GeoBlocker.
 * Implements GeoBlockRepository using Redis SET of country codes.
 *
 * Redis operations:
 *   SISMEMBER nm:ddos:geo:blocked {countryCode}  — check after GeoIP resolve
 *   SADD      nm:ddos:geo:blocked {countryCode}  — operator blocks a country
 *   SREM      nm:ddos:geo:blocked {countryCode}  — operator unblocks a country
 *   SMEMBERS  nm:ddos:geo:blocked                — status endpoint only
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GeoBlockRedisAdapter implements GeoBlockRepository {

    private final StringRedisTemplate redis;

    @Override
    public boolean contains(String countryCode) {
        Boolean result = redis.opsForSet().isMember(RedisKeys.DDOS_GEO_BLOCKED, countryCode);
        log.debug("[Redis] SISMEMBER {} {} → {}", RedisKeys.DDOS_GEO_BLOCKED, countryCode, result);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void add(String countryCode) {
        redis.opsForSet().add(RedisKeys.DDOS_GEO_BLOCKED, countryCode);
        log.info("[Redis] SADD {} {} — country added to geo-block", RedisKeys.DDOS_GEO_BLOCKED, countryCode);
    }

    @Override
    public void remove(String countryCode) {
        redis.opsForSet().remove(RedisKeys.DDOS_GEO_BLOCKED, countryCode);
        log.info("[Redis] SREM {} {} — country removed from geo-block", RedisKeys.DDOS_GEO_BLOCKED, countryCode);
    }

    @Override
    public Set<String> getAll() {
        Set<String> members = redis.opsForSet().members(RedisKeys.DDOS_GEO_BLOCKED);
        return members != null ? members : Set.of();
    }
}
