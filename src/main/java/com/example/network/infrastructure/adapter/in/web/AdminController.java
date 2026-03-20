package com.example.network.infrastructure.adapter.in.web;

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

    /**
     * View current reputation list + Redis key name.
     * GET /admin/ddos/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
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
}