package com.example.network.infrastructure.adapter.config;

/**
 * INFRASTRUCTURE — Redis Key Constants
 *
 * Single source of truth for all Redis keys.
 * Never hardcode key strings anywhere else.
 *
 * Naming convention: nm:ddos:{purpose}
 */
public final class RedisKeys {

    private RedisKeys() {}

    /**
     * Redis SET of known malicious IPs.
     *
     * Written by: scheduled sync job (every 24h) or admin API (for testing)
     * Read by:    DDoSProtectionFilter — SISMEMBER check on every request
     * TTL:        none — refreshed by replacing entire set
     * Type:       SET
     */
    public static final String DDOS_REPUTATION = "nm:ddos:reputation";
}