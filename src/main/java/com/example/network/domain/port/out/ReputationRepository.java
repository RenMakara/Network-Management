package com.example.network.domain.port.out;


import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * DOMAIN — Output Port (Repository interface)
 *
 * Defines WHAT the domain needs from infrastructure.
 * The application service calls this port.
 * The Redis adapter implements it.
 * The domain never imports Redis, Spring Data, or any framework.
 */
@Service
public interface ReputationRepository {

    /**
     * Check if an IP is in the reputation list.
     * Implemented by Redis SISMEMBER nm:ddos:reputation
     *
     * @param ip the IP address string
     * @return true if IP is known malicious
     */
    boolean contains(String ip);

    /**
     * Add an IP to the reputation list.
     * Used by: scheduled sync job (every 24h in production)
     * Used by: admin API (for testing)
     */
    void add(String ip);

    /**
     * Remove an IP from the reputation list.
     */
    void remove(String ip);

    /**
     * Get all IPs currently in the reputation list.
     * Used by: admin status endpoint.
     */
    Set<String> getAll();
}