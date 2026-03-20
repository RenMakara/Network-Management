package com.example.network.domain.port.in;


import com.example.network.domain.model.ReputationCheckResult;
import com.example.network.domain.model.VisitorIp;

/**
 * DOMAIN — Input Port (Use Case interface)
 *
 * Defines WHAT the application can do.
 * The filter calls this. The application service implements it.
 * The domain never knows about HTTP, Redis, or Spring.
 */
public interface CheckReputationUseCase {

    /**
     * Check whether the given IP is in the reputation list.
     *
     * @param ip the real client IP extracted from X-Forwarded-For
     * @return ReputationCheckResult — either malicious (block) or clean (pass)
     */
    ReputationCheckResult check(VisitorIp ip);
}