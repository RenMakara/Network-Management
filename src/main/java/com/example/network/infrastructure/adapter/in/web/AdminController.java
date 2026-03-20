package com.example.network.infrastructure.adapter.in.web;

import com.example.network.domain.port.out.GeoBlockRepository;
import com.example.network.domain.port.out.ReputationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * INFRASTRUCTURE — AdminController (Input Adapter)
 *
 * REST API for operator to manage the reputation list at runtime.
 * Excluded from DDoS filter by shouldNotFilter() pattern.
 *
 * In production: reputation list is populated by a scheduled sync job
 * that downloads from a threat intelligence feed every 24h.
 * These endpoints exist so you can test Layer 1 manually with curl.
 */
@RestController
@RequestMapping("/admin/ddos")
@RequiredArgsConstructor
public class AdminController {

    private final ReputationRepository reputationRepository;
    private final GeoBlockRepository geoBlockRepository;


    @GetMapping("/geo")
    public ResponseEntity<Map<String, Object>> geo() {
        Set<String> ips = reputationRepository.getAll();
        return ResponseEntity.ok(Map.of(
                "redisKey",      "nm:ddos:reputation",
                "type",          "Redis SET",
                "totalIPs",      ips.size(),
                "ips",           ips,
                "howToCheck",    "Redis CLI: SMEMBERS nm:ddos:reputation"
        ));
    }

    /**
     * View current reputation list + Redis key name.
     * GET /admin/ddos/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "layer1_reputation", Map.of(
                        "redisKey", "nm:ddos:reputation",
                        "count",    reputationRepository.getAll().size(),
                        "ips",      reputationRepository.getAll()),
//                "layer2_blocklist", Map.of(
//                        "redisKey", "nm:ddos:blocklist",
//                        "count",    blocklistRepository.getAll().size(),
//                        "ips",      blocklistRepository.getAll()),
                "layer3_geo", Map.of(
                        "redisKey",  "nm:ddos:geo:blocked",
                        "count",     geoBlockRepository.getAll().size(),
                        "countries", geoBlockRepository.getAll())
//                "layer4_bodySize", Map.of(
//                        "redisKey", "nm:ddos:body:size:limit",
//                        "maxBytes", bodySizePolicy.getMaxBytes(),
//                        "maxMB",    bodySizePolicy.getMaxBytes() / 1_048_576.0)
        ));
    }

    /**
     * Add an IP to the reputation list.
     * POST /admin/ddos/reputation
     * Body: {"ip": "1.2.3.4"}
     *
     * Simulates what the threat feed sync job does every 24h.
     */
    @PostMapping("/reputation")
    public ResponseEntity<Map<String, String>> addReputation(@RequestBody Map<String, String> body) {
        String ip = body.get("ip");
        if (ip == null || ip.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "ip field is required"));
        }
        reputationRepository.add(ip);
        return ResponseEntity.ok(Map.of(
                "action",        "added",
                "ip",            ip,
                "redisKey",      "nm:ddos:reputation",
                "redisCommand",  "SADD nm:ddos:reputation " + ip,
                "effect",        "Next request from this IP will be silently dropped (HTTP 204)"
        ));
    }

    /**
     * Remove an IP from the reputation list.
     * DELETE /admin/ddos/reputation/{ip}
     */
    @DeleteMapping("/reputation/{ip}")
    public ResponseEntity<Map<String, String>> removeReputation(@PathVariable String ip) {
        reputationRepository.remove(ip);
        return ResponseEntity.ok(Map.of(
                "action",       "removed",
                "ip",           ip,
                "redisCommand", "SREM nm:ddos:reputation " + ip,
                "effect",       "Requests from this IP will now pass Layer 1"
        ));
    }



    // Layer 3 for blocking by country code

    @PostMapping("/geo")
    public ResponseEntity<Map<String, String>> addGeoBlock(@RequestBody Map<String, String> body) {
        String code = body.get("countryCode");
        geoBlockRepository.add(code);
        return ResponseEntity.ok(Map.of(
                "action", "added", "layer", "3-geo", "countryCode", code,
                "effect", "Requests from " + code + " → HTTP 403 Forbidden"));
    }

    @DeleteMapping("/geo/{code}")
    public ResponseEntity<Map<String, String>> removeGeoBlock(@PathVariable String code) {
        geoBlockRepository.remove(code);
        return ResponseEntity.ok(Map.of("action", "removed", "layer", "3-geo", "countryCode", code));
    }
}