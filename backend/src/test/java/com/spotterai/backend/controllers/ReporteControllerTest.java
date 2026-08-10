package com.spotterai.backend.controllers;

import com.spotterai.backend.seguridad.AdminEmails;
import com.spotterai.backend.seguridad.Reportes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Quién puede leer {@code GET /api/reportes}.
 *
 * <p>Es la primera vez en esta API que una ruta autenticada distingue entre
 * "cualquiera con sesión" y "esta persona en concreto", en vez de dejarlo todo
 * a las reglas por patrón de {@code SecurityConfig}. Justo por ser la primera
 * vez es donde más fácil es invertir la condición sin que nada más lo note: un
 * {@code if (!admins.esAdmin(...))} que se quede en {@code if (admins.esAdmin(...))}
 * compila igual y enseña los reportes a todo el mundo.
 */
class ReporteControllerTest {

    private Reportes reportes;
    private AdminEmails admins;
    private ReporteController controlador;

    @BeforeEach
    void preparar() {
        reportes = mock(Reportes.class);
        admins = mock(AdminEmails.class);
        controlador = new ReporteController(reportes, admins);
    }

    /** El ThreadLocal de Spring Security no debe sobrevivir a esta prueba. */
    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(email, null));
    }

    @Test
    @DisplayName("Quien no es admin recibe 404, no la lista")
    void quienNoEsAdminRecibe404() {
        autenticarComo("cualquiera@test.com");
        when(admins.esAdmin("cualquiera@test.com")).thenReturn(false);

        ResponseEntity<?> respuesta = controlador.todos();

        assertEquals(404, respuesta.getStatusCode().value());
    }

    @Test
    @DisplayName("Quien es admin recibe la lista")
    void quienEsAdminRecibeLaLista() {
        autenticarComo("admin@test.com");
        when(admins.esAdmin("admin@test.com")).thenReturn(true);
        when(reportes.todos()).thenReturn(java.util.List.of());

        ResponseEntity<?> respuesta = controlador.todos();

        assertEquals(200, respuesta.getStatusCode().value());
    }

    @Test
    @DisplayName("A quien no es admin no se le llega a pedir la lista al servicio")
    void aQuienNoEsAdminNoSeLePideNada() {
        // No solo que la respuesta sea 404: que ni siquiera se toque el
        // repositorio para alguien que no debería estar preguntando esto.
        autenticarComo("cualquiera@test.com");
        when(admins.esAdmin("cualquiera@test.com")).thenReturn(false);

        controlador.todos();

        org.mockito.Mockito.verify(reportes, org.mockito.Mockito.never()).todos();
    }
}
