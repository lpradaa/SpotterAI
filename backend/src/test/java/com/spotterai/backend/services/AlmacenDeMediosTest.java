package com.spotterai.backend.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La comprobacion de bytes es lo que cierra el agujero de subir cualquier cosa
 * declarando un tipo permitido. El tipo lo elige quien sube; los bytes, no.
 */
class AlmacenDeMediosTest {

    private static byte[] conCabecera(int... bytes) {
        byte[] b = new byte[16];
        for (int i = 0; i < bytes.length; i++) b[i] = (byte) bytes[i];
        return b;
    }

    @Test
    @DisplayName("Reconoce las cabeceras de cada formato permitido")
    void reconoceLosFormatos() {
        assertTrue(AlmacenDeMedios.contenidoCoincide(conCabecera(0xFF, 0xD8, 0xFF, 0xE0), 16, "jpg"));
        assertTrue(AlmacenDeMedios.contenidoCoincide(
                conCabecera(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A), 16, "png"));
        assertTrue(AlmacenDeMedios.contenidoCoincide(conCabecera('G', 'I', 'F', '8'), 16, "gif"));
        assertTrue(AlmacenDeMedios.contenidoCoincide(
                conCabecera('R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'), 16, "webp"));
        assertTrue(AlmacenDeMedios.contenidoCoincide(
                conCabecera(0, 0, 0, 0, 'f', 't', 'y', 'p'), 16, "mp4"));
        assertTrue(AlmacenDeMedios.contenidoCoincide(
                conCabecera(0x1A, 0x45, 0xDF, 0xA3), 16, "webm"));
    }

    @Test
    @DisplayName("Un texto declarado como imagen no pasa")
    void textoDisfrazadoDeImagen() {
        byte[] texto = conCabecera('<', 'h', 't', 'm', 'l', '>', ' ', 'h', 'o', 'l', 'a', '!');

        assertFalse(AlmacenDeMedios.contenidoCoincide(texto, 16, "png"));
        assertFalse(AlmacenDeMedios.contenidoCoincide(texto, 16, "jpg"));
        assertFalse(AlmacenDeMedios.contenidoCoincide(texto, 16, "mp4"));
    }

    @Test
    @DisplayName("Un PNG declarado como jpg no pasa por jpg")
    void formatoQueNoCoincideConLoDeclarado() {
        byte[] png = conCabecera(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);

        // Antes esto se guardaba con extension .jpg y se servia como image/jpeg,
        // porque la extension salia del tipo que declaraba el cliente.
        assertFalse(AlmacenDeMedios.contenidoCoincide(png, 16, "jpg"));
        assertTrue(AlmacenDeMedios.contenidoCoincide(png, 16, "png"));
    }

    @Test
    @DisplayName("Un archivo demasiado corto para tener cabecera se rechaza")
    void archivoMinusculo() {
        assertFalse(AlmacenDeMedios.contenidoCoincide(conCabecera(0x89, 'P'), 2, "png"));
    }

    @Test
    @DisplayName("Una extension que no conocemos nunca se acepta")
    void extensionDesconocida() {
        assertFalse(AlmacenDeMedios.contenidoCoincide(conCabecera(0x89, 'P', 'N', 'G'), 16, "svg"));
    }
}
