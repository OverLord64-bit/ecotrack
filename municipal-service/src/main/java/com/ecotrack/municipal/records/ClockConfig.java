package com.ecotrack.municipal.records;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class ClockConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
