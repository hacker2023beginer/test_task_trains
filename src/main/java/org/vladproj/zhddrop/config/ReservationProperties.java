package org.vladproj.zhddrop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "reservation.hold")
public record ReservationProperties(Duration ttl) {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    public ReservationProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            ttl = DEFAULT_TTL;
        }
    }

    public static ReservationProperties defaults() {
        return new ReservationProperties(DEFAULT_TTL);
    }
}
