package com.example.network.infrastructure.adapter.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * INFRASTRUCTURE — TestController
 *
 * Simple endpoint to verify a request reached the application
 * (passed all DDoS filter layers).
 *
 * If a request is blocked by DDoS filter — this is never reached.
 * If you get HTTP 200 here — the request passed Layer 1.
 */
@RestController
public class TestController {

    @GetMapping("/api/hello")
    public ResponseEntity<Map<String, String>> hello() {
        return ResponseEntity.ok(Map.of(
                "status",  "PASS",
                "message", "Request passed all DDoS layers",
                "layer1",  "IP Reputation — CLEAN"
        ));
    }

    @PostMapping("/api/data")
    public ResponseEntity<Map<String, String>> data(@RequestBody(required = false) String body) {
        return ResponseEntity.ok(Map.of(
                "status",  "PASS",
                "message", "POST request passed all DDoS layers"
        ));
    }
}