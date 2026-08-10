package com.spotterai.backend.seguridad;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Quién puede ver los reportes.
 *
 * <p>Es la única puerta entre "cualquiera con sesión" y "los datos de quién ha
 * reportado a quién", así que lo que falle aquí falla en el peor sentido
 * posible: o nadie ve nunca los reportes, o los ve cualquiera.
 */
class AdminEmailsTest {

    @Test
    @DisplayName("Sin nadie en la lista, nadie es admin, ni siquiera con una cuenta válida")
    void sinListaNadieEsAdmin() {
        AdminEmails admins = new AdminEmails("");

        assertFalse(admins.esAdmin("quien-sea@test.com"));
    }

    @Test
    @DisplayName("Quien está en la lista, es admin")
    void quienEstaEnLaListaEsAdmin() {
        AdminEmails admins = new AdminEmails("admin@spotterai.test");

        assertTrue(admins.esAdmin("admin@spotterai.test"));
    }

    @Test
    @DisplayName("Quien no está en la lista, no lo es")
    void quienNoEstaNoLoEs() {
        AdminEmails admins = new AdminEmails("admin@spotterai.test");

        assertFalse(admins.esAdmin("cualquiera@test.com"));
    }

    @Test
    @DisplayName("Varios correos, separados por comas")
    void variosCorreos() {
        AdminEmails admins = new AdminEmails("uno@test.com,dos@test.com, tres@test.com");

        assertTrue(admins.esAdmin("uno@test.com"));
        assertTrue(admins.esAdmin("dos@test.com"));
        // Con espacio después de la coma en la lista de origen: no debe
        // colarse un espacio en el correo comparado.
        assertTrue(admins.esAdmin("tres@test.com"));
    }

    @Test
    @DisplayName("No importan las mayúsculas, en ningún lado")
    void noImportanLasMayusculas() {
        AdminEmails admins = new AdminEmails("Admin@Spotterai.Test");

        assertTrue(admins.esAdmin("admin@spotterai.test"));
        assertTrue(admins.esAdmin("ADMIN@SPOTTERAI.TEST"));
    }

    @Test
    @DisplayName("Un correo nulo no es admin, y no revienta")
    void unCorreoNuloNoRevienta() {
        AdminEmails admins = new AdminEmails("admin@spotterai.test");

        assertFalse(admins.esAdmin(null));
    }
}
