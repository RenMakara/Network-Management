package com.example.network.infrastructure.adapter.out.redis;

import com.example.network.domain.port.out.BlocklistRepository;
import com.example.network.infrastructure.adapter.config.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * INFRASTRUCTURE — BlocklistRedisAdapter (Output Adapter)
 *
 * Layer 2 — IP Blocklist.
 * Implements BlocklistRepository using Redis SET.
 *
 * Redis operations:
 *   SISMEMBER nm:ddos:blocklist {ip}  — check on every request O(1)
 *   SADD      nm:ddos:blocklist {ip}  — operator adds via admin API
 *   SREM      nm:ddos:blocklist {ip}  — operator removes via admin API
 *   SMEMBERS  nm:ddos:blocklist       — status endpoint only
 *
 * Why separate from ReputationRedisAdapter:
 *   Different lifecycle (permanent vs 24h refresh),
 *   different owner (operator vs system),
 *   different domain concept — separate adapter per port.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlocklistRedisAdapter implements BlocklistRepository {

    private final StringRedisTemplate redis;

    @Override
    public boolean contains(String ip) {
        Boolean result = redis.opsForSet().isMember(RedisKeys.DDOS_BLOCKLIST, ip);
        log.debug("[Redis] SISMEMBER {} {} → {}", RedisKeys.DDOS_BLOCKLIST, ip, result);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void add(String ip) {
        redis.opsForSet().add(RedisKeys.DDOS_BLOCKLIST, ip);
        log.info("[Redis] SADD {} {} — IP added to blocklist", RedisKeys.DDOS_BLOCKLIST, ip);
    }

    @Override
    public void remove(String ip) {
        redis.opsForSet().remove(RedisKeys.DDOS_BLOCKLIST, ip);
        log.info("[Redis] SREM {} {} — IP removed from blocklist", RedisKeys.DDOS_BLOCKLIST, ip);
    }

    @Override
    public Set<String> getAll() {
        Set<String> members = redis.opsForSet().members(RedisKeys.DDOS_BLOCKLIST);
        return members != null ? members : Set.of();
    }
}