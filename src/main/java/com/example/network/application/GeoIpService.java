package com.example.network.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * INFRASTRUCTURE — GeoIpService
 *
 * Resolves a client IP address to an ISO 3166-1 alpha-2 country code.
 *
 * In PRODUCTION: replace resolveCountry() body with MaxMind GeoLite2 lookup:
 *   DatabaseReader reader = new DatabaseReader.Builder(new File("GeoLite2-Country.mmdb")).build();
 *   InetAddress ip = InetAddress.getByName(ipAddress);
 *   CountryResponse response = reader.country(ip);
 *   return Optional.of(response.getCountry().getIsoCode());
 *
 * In TESTING: uses an in-memory map so tests run without any .mmdb file.
 * Lookup time: ~50μs in production (entire database loaded into JVM memory at startup).
 *
 * Returns Optional.empty() when:
 *   - IP is not in the test map
 *   - IP cannot be resolved (private range, malformed, etc.)
 * When empty → filter SKIPS the geo check (unknown ≠ blocked).
 */
@Component
@Slf4j
public class GeoIpService {

    /**
     * Test IP → country code mapping.
     * Used in tests and local development.
     * In production this is replaced by MaxMind GeoLite2 database.
     */
    private static final Map<String, String> TEST_GEO_MAP = Map.of(
            "1.2.3.4",     "CN",   // China
            "5.6.7.8",     "RU",   // Russia
            "9.10.11.12",  "KP",   // North Korea
            "10.0.0.1",    "US",   // United States
            "172.16.0.1",  "DE",   // Germany
            "192.168.1.1", "KH",   // Cambodia
            "8.8.8.8",     "US",   // Google DNS → US
            "1.1.1.1",     "AU"    // Cloudflare DNS → Australia
    );

    /**
     * Resolve IP address to country code.
     *
     * @param ipAddress the raw IP string
     * @return Optional containing 2-letter country code, or empty if unresolvable
     */
    public Optional<String> resolveCountry(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return Optional.empty();
        }

        // In production: MaxMind GeoLite2 database lookup here
        String country = TEST_GEO_MAP.get(ipAddress.trim());

        if (country != null) {
            log.debug("[GeoIP] {} → {}", ipAddress, country);
        } else {
            log.debug("[GeoIP] {} → unknown (not in test map)", ipAddress);
        }

        return Optional.ofNullable(country);
    }
}
