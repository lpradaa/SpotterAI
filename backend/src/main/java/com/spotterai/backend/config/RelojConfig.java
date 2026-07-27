package com.spotterai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * El reloj, como una dependencia mas.
 *
 * <p>Todo lo que decide cosas mirando la hora —si una sesion ya ha pasado, si
 * todavia se puede aceptar, cuando cae el proximo lunes— tiene que poder
 * probarse sin esperar a que llegue el momento. Con {@code LocalDateTime.now()}
 * repartido por el codigo, la unica forma de comprobar "no se puede aceptar una
 * sesion que ya empezo" seria escribir una prueba que tarda en fallar.
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
