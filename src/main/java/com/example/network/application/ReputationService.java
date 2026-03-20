package com.example.network.application;

import com.example.network.domain.model.ReputationCheckResult;
import com.example.network.domain.model.VisitorIp;
import com.example.network.domain.port.in.CheckReputationUseCase;
import com.example.network.domain.port.out.ReputationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * APPLICATION — ReputationService
 *
 * Implements CheckReputationUseCase.
 * Orchestrates: calls ReputationRepository (output port), returns domain result.
 *
 * Has @Service (Spring) but:
 *   - does NOT import Redis
 *   - does NOT import HTTP classes
 *   - only knows about domain model and ports
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReputationService implements CheckReputationUseCase {

    // Injected by Spring — actual impl is ReputationRedisAdapter
    private final ReputationRepository reputationRepository;

    @Override
    public ReputationCheckResult check(VisitorIp ip) {
        boolean malicious = reputationRepository.contains(ip.getAddress());

        if (malicious) {
            log.warn("[Reputation] BLOCKED ip={} — known malicious IP", ip);
            return ReputationCheckResult.malicious(ip);
        }

        log.debug("[Reputation] CLEAN ip={}", ip);
        return ReputationCheckResult.clean(ip);
    }
}
