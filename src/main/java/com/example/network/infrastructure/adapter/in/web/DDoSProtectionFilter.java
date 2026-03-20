package com.example.network.infrastructure.adapter.in.web;


import com.example.network.application.GeoIpService;
import com.example.network.domain.model.ReputationCheckResult;
import com.example.network.domain.model.VisitorIp;
import com.example.network.domain.port.in.CheckReputationUseCase;
import com.example.network.domain.port.out.BlocklistRepository;
import com.example.network.domain.port.out.BodySizePolicy;
import com.example.network.domain.port.out.GeoBlockRepository;
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
import java.util.Optional;

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

    private final CheckReputationUseCase checkReputationUseCase;
    private final GeoBlockRepository geoBlockRepository;
    private final GeoIpService geoIpService;
    private final BlocklistRepository blocklistRepository;
    private final BodySizePolicy bodySizePolicy;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // ── Step 1: Extract real IP ────────────────────────────────────────
        // X-Forwarded-For is read in EXACTLY this one place.
        // No other class in the system reads this header.
        String rawIp = extractIp(request);
        VisitorIp ip = VisitorIp.of(rawIp);

        // Layer 1 — IP Reputation
        if (checkReputationUseCase.check(ip).isMalicious()) {
            log.warn("[DDoS-L1] BLOCKED ip={} reputation", ip);
            silentDrop(response);
            return;
        }

        // Layer 2 — IP Blocklist
        if (blocklistRepository.contains(rawIp)) {
            log.warn("[DDoS-L2] BLOCKED ip={} blocklist", ip);
            silentDrop(response);
            return;
        }

        // Layer 3 — GeoBlocker
        Optional<String> country = geoIpService.resolveCountry(rawIp);
        if (country.isPresent() && geoBlockRepository.contains(country.get())) {
            log.warn("[DDoS-L3] BLOCKED ip={} country={}", ip, country.get());
            geoBlocked(response, country.get());
            return;
        }

        // Layer 4 — Body Size
        long contentLength = request.getContentLengthLong();
        if (contentLength > bodySizePolicy.getMaxBytes()) {
            log.warn("[DDoS-L4] BLOCKED ip={} size={} limit={}", ip, contentLength, bodySizePolicy.getMaxBytes());
            payloadTooLarge(response);
            return;
        }

        log.debug("[DDoS] PASS ip={} country={}", ip, country.orElse("unknown"));
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

    private void geoBlocked(HttpServletResponse r, String countryCode) throws IOException {
        r.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
        r.setContentType("application/json");
        r.getWriter().write("{\"blocked\":true,\"reason\":\"geo_blocked\",\"country\":\"" + countryCode + "\"}");
    }

    private void silentDrop(HttpServletResponse r) throws IOException {
        r.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
    }

    private void payloadTooLarge(HttpServletResponse r) throws IOException {
        r.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE); // 413
        r.setContentType("application/json");
        r.getWriter().write("{\"blocked\":true,\"reason\":\"body_too_large\"}");
    }
}