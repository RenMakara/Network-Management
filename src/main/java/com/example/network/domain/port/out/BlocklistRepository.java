package com.example.network.domain.port.out;

import org.springframework.stereotype.Service;


import java.util.Set;

/**
 * DOMAIN — Output Port
 *
 * Layer 2 — IP Blocklist (operator-managed permanent blocks).
 *
 * Different from ReputationRepository:
 *   - Reputation = automated, system-owned, 24h refresh
 *   - Blocklist  = manual, operator-decision, permanent, audit trail required
 *
 * Redis key: nm:ddos:blocklist (SET)
 */
public interface BlocklistRepository {

    /** Redis: SISMEMBER nm:ddos:blocklist {ip} */
    boolean contains(String ip);

    /** Redis: SADD nm:ddos:blocklist {ip} */
    void add(String ip);

    /** Redis: SREM nm:ddos:blocklist {ip} */
    void remove(String ip);

    /** Redis: SMEMBERS nm:ddos:blocklist */
    Set<String> getAll();
}