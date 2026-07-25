package com.spotterai.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner initData(
            com.spotterai.backend.services.UsuarioService usuarioService) {
        return args -> {
            // Buscamos si ya existe el usuario de prueba para no duplicarlo
            if (usuarioService.buscarPorEmail("luis-tfg@test.com").isEmpty()) {
                
                com.spotterai.backend.dtos.UsuarioRegistroDTO nuevoUser = 
                        new com.spotterai.backend.dtos.UsuarioRegistroDTO();
                
                // Mapeamos los datos mínimos que pida vuestro DTO
                nuevoUser.setEmail("luis-tfg@test.com");
                nuevoUser.setPassword("123456"); 
                nuevoUser.setNombre("Luis Admin");
                
                // Si vuestro DTO tiene más campos obligatorios (edad, genero...),
                // rellénalos aquí abajo siguiendo el mismo ejemplo:
                // nuevoUser.setEdad(25);
                // nuevoUser.setGenero("Masculino");
                
                // Llamamos al método legítimo de tu compañero que encripta de verdad
                usuarioService.registrarUsuario(nuevoUser);
                System.out.println("🔥 LOG DE LUIS: ¡Usuario 'luis-tfg@test.com' creado con éxito desde Java! 🔥");
            }
        };
    }

}
