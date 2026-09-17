package com.floristeria.floristeria.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    public static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Bean
    public Clock clockBogota() {
        return Clock.system(ZONA_BOGOTA);
    }
}
