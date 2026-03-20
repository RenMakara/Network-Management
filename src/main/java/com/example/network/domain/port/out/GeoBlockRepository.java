package com.example.network.domain.port.out;

import java.util.Set;

/**
 * DOMAIN — Output Port
 *
 * Layer 3 — GeoBlocker (country-level blocking).
 *
 * Stores ISO 3166 country codes blocked by the operator.
 * The filter resolves IP → country via GeoIpService FIRST,
 * then calls this port to check if that country is blocked.
 *
 * Redis key: nm:ddos:geo:blocked (SET of 2-letter codes)
 */
public interface GeoBlockRepository {

    /** Redis: SISMEMBER nm:ddos:geo:blocked {countryCode} */
    boolean contains(String countryCode);

    /** Redis: SADD nm:ddos:geo:blocked {countryCode} */
    void add(String countryCode);

    /** Redis: SREM nm:ddos:geo:blocked {countryCode} */
    void remove(String countryCode);

    /** Redis: SMEMBERS nm:ddos:geo:blocked */
    Set<String> getAll();
}