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

    /**
     * Layer 2 — IP Blocklist (operator-managed permanent blocks)
     * Redis SET of IPs manually blocked by the operator.
     * Written by: admin API only. Audit trail in PostgreSQL (production).
     * Type: SET  TTL: none (permanent until operator removes)
     */
    public static final String DDOS_BLOCKLIST = "nm:ddos:blocklist";

    /**
     * Layer 3 — GeoBlocker
     * Redis SET of ISO 3166-1 alpha-2 country codes blocked by the operator.
     * e.g. "CN", "RU", "KP"
     * Type: SET  TTL: none
     */
    public static final String DDOS_GEO_BLOCKED = "nm:ddos:geo:blocked";
//
//    /**
//     * Layer 4 — Request body size limit
//     * Redis STRING storing the max allowed Content-Length in bytes.
//     * Default: 1,048,576 (1MB). Operator can change at runtime.
//     * Type: STRING  TTL: none
//     */
//    public static final String DDOS_BODY_SIZE_LIMIT = "nm:ddos:body:size:limit";
}