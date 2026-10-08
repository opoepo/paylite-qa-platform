package io.paylite.sut.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/**
 * Single source of "now" for the whole service (ADR-0005).
 * Ticks in whole microseconds to match PostgreSQL timestamp precision,
 * so a value returned on creation equals the value read back (ADR-0015).
 * Tests can replace this bean to control time deterministically.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
