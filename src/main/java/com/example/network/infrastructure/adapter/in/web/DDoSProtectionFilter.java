package com.example.network.infrastructure.adapter.in.web;


import com.example.network.domain.model.ReputationCheckResult;
import com.example.network.domain.model.VisitorIp;
import com.example.network.domain.port.in.CheckReputationUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * INFRASTRUCTURE — DDoSProtectionFilter (Input Adapter)
 *
 * @Order(1) — runs FIRST before every other filter.
 *
 * Layer 1: IP Reputation Blocker
 *   - Extracts real IP from X-Forwarded-For header
 *   - Calls CheckReputationUseCase (never touches Redis directly)
 *   - If malicious: HTTP 204 No Content (silent drop — reveals nothing to attacker)
 *   - If clean: chain.doFilter() → next filter
 *
 * Hexagonal rule:
 *   This filter is an INPUT ADAPTER. It calls the use case (port).
 *   It never imports Redis, StringRedisTemplate, or RedisKeys.
 *   Those details live in ReputationRedisAdapter (output adapter).
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DDoSProtectionFilter extends OncePerRequestFilter {

    // Injected use case — actually implemented by ReputationService
    private final CheckReputationUseCase checkReputationUseCase;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // ── Step 1: Extract real IP ────────────────────────────────────────
        // X-Forwarded-For is read in EXACTLY this one place.
        // No other class in the system reads this header.
        String rawIp = extractIp(request);
        VisitorIp ip = VisitorIp.of(rawIp);

        // ── Step 2: Layer 1 — IP Reputation check ─────────────────────────
        // Redis SISMEMBER nm:ddos:reputation {ip} — ~0.3ms
        ReputationCheckResult result = checkReputationUseCase.check(ip);

        if (result.isMalicious()) {
            // Silent drop — 204 No Content
            // Attacker sees nothing. No error message. No hint that they are blocked.
            log.warn("[DDoS-Layer1] BLOCKED ip={} — reputation match — silent drop", ip);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
            return; // stop here — do NOT call chain.doFilter()
        }

        // ── All checks passed — continue to next filter ────────────────────
        log.debug("[DDoS-Layer1] PASS ip={}", ip);
        chain.doFilter(request, response);
    }

    /**
     * Extract real client IP.
     *
     * X-Forwarded-For format: "client, proxy1, proxy2"
     * We take the FIRST value — that is the real client IP.
     *
     * X-Test-IP is a test-only header for local testing
     * when no real proxy sets X-Forwarded-For.
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        // Test-only header — makes curl testing easy without a real proxy
        String testIp = request.getHeader("X-Test-IP");
        if (testIp != null && !testIp.isBlank()) {
            return testIp.trim();
        }
        return request.getRemoteAddr();
    }
}