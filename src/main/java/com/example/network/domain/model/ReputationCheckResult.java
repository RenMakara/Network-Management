package com.example.network.domain.model;


/**
 * DOMAIN — Value Object
 *
 * The result of checking whether an IP is in the reputation list.
 * Immutable. Either the IP is known malicious, or it is clean.
 */
public final class ReputationCheckResult {

    private final VisitorIp ip;
    private final boolean malicious;

    private ReputationCheckResult(VisitorIp ip, boolean malicious) {
        this.ip = ip;
        this.malicious = malicious;
    }

    public static ReputationCheckResult clean(VisitorIp ip) {
        return new ReputationCheckResult(ip, false);
    }

    public static ReputationCheckResult malicious(VisitorIp ip) {
        return new ReputationCheckResult(ip, true);
    }

    public boolean isMalicious() {
        return malicious;
    }

    public boolean isClean() {
        return !malicious;
    }

    public VisitorIp getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return "ReputationCheckResult{ip=" + ip + ", malicious=" + malicious + "}";
    }
}