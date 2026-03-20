package com.example.network.domain.port.out;

/**
 * DOMAIN — Output Port
 *
 * Layer 4 — Request body size limit.
 *
 * Stores the operator-configured maximum body size in bytes.
 * Default: 1MB (1,048,576 bytes).
 *
 * Unlike layers 1-3, this is not a SET of blocked values.
 * It is a single configurable threshold.
 *
 * Redis key: nm:ddos:body:size:limit (STRING)
 */
public interface BodySizePolicy {

    /**
     * Get the current maximum allowed request body size in bytes.
     * Redis: GET nm:ddos:body:size:limit
     * Default: 1,048,576 (1MB) if key is not set.
     */
    long getMaxBytes();

    /**
     * Update the body size limit at runtime.
     * Redis: SET nm:ddos:body:size:limit {bytes}
     * Takes effect immediately on the next request.
     */
    void setMaxBytes(long bytes);
}