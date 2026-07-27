package com.spotterai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada.
 *
 * <p>Aquí había un {@code CommandLineRunner} heredado del TFG que creaba en cada
 * arranque, y en cualquier entorno, un usuario fijo con la contraseña "123456".
 * Con el repositorio público eso es una cuenta conocida y trivial en toda
 * instalación que alguien levante, sin que nada lo anuncie. Los datos de prueba
 * viven ahora en {@code demo.SembradorDemo}, que solo actúa con el perfil
 * {@code demo} puesto a mano.
 */
@SpringBootApplication
@EnableScheduling // latido del canal de eventos
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}
