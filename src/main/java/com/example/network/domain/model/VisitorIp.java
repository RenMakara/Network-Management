package com.example.network.domain.model;

import java.util.Objects;

/**
 * DOMAIN — Value Object
 *
 * Represents the real client IP address.
 * Extracted ONCE from X-Forwarded-For in the filter.
 * Two VisitorIp with the same address are identical (value semantics).
 */
public final class VisitorIp {

    private final String address;

    private VisitorIp(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("IP address must not be blank");
        }
        this.address = address.trim();
    }

    public static VisitorIp of(String address) {
        return new VisitorIp(address);
    }

    public String getAddress() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VisitorIp v)) return false;
        return Objects.equals(address, v.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return address;
    }
}