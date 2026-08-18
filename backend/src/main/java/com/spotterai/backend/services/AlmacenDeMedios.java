package com.spotterai.backend.services;

import com.spotterai.backend.textos.ErrorDeNegocio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda las fotos y videos que sube la gente, y los vuelve a servir.
 *
 * <p>En disco y no en la base de datos: los binarios la hinchan, complican
 * cualquier copia y obligan a cargarlos en memoria para servirlos.
 */
@Service
public class AlmacenDeMedios {

    /** 15 MB. Un video de una serie cabe de sobra y un descuido no llena el disco. */
    static final long TAMANO_MAXIMO = 15L * 1024 * 1024;

    /**
     * Tipos permitidos y la extension con la que se guardan.
     *
     * Lista blanca y no negra: enumerar lo que se rechaza siempre deja algo
     * fuera. La extension sale de aqui y nunca del nombre que manda el cliente.
     */
    private static final Map<String, String> TIPOS_PERMITIDOS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "video/mp4", "mp4",
            "video/webm", "webm",
            "video/quicktime", "mov");

    private final Path carpeta;

    public AlmacenDeMedios(@Value("${spotterai.medios.carpeta:medios}") String carpeta) {
        this.carpeta = Paths.get(carpeta).toAbsolutePath().normalize();
    }

    /** Resultado de guardar: la ruta publica y si es imagen o video. */
    public record MedioGuardado(String url, String tipo) {}

    public MedioGuardado guardar(MultipartFile archivo) throws IOException {
        if (archivo == null || archivo.isEmpty()) {
            throw ErrorDeNegocio.de("error.medio.sinArchivo");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw ErrorDeNegocio.de("error.medio.demasiadoGrande", TAMANO_MAXIMO / (1024 * 1024));
        }

        String contentType = archivo.getContentType() == null
                ? "" : archivo.getContentType().toLowerCase(Locale.ROOT);
        String extension = TIPOS_PERMITIDOS.get(contentType);
        if (extension == null) {
            throw ErrorDeNegocio.de("error.medio.formatoNoAdmitido");
        }

        // El tipo declarado lo pone quien sube, asi que no basta con creerselo:
        // se comprueba que los primeros bytes sean de verdad lo que dice ser. Sin
        // esto, cualquier fichero pasaba con solo declarar image/png.
        byte[] cabecera = new byte[16];
        int leidos;
        try (InputStream sonda = archivo.getInputStream()) {
            leidos = sonda.readNBytes(cabecera, 0, cabecera.length);
        }
        if (!contenidoCoincide(cabecera, leidos, extension)) {
            throw ErrorDeNegocio.de("error.medio.noEsLoQueDice", extension);
        }

        Files.createDirectories(carpeta);

        // El nombre lo ponemos nosotros. Usar el del cliente permite escribir
        // fuera de la carpeta con "../../algo" y ademas machacar lo de otro.
        String nombre = UUID.randomUUID() + "." + extension;
        Path destino = carpeta.resolve(nombre);

        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
        }

        return new MedioGuardado("/api/medios/" + nombre,
                contentType.startsWith("video/") ? "video" : "imagen");
    }

    /**
     * Devuelve la ruta en disco de un medio ya guardado.
     *
     * Vuelve a comprobar que cae dentro de la carpeta aunque los nombres los
     * generemos nosotros: el que llega por la URL lo escribe quien quiera.
     */
    public Path localizar(String nombre) {
        Path candidato = carpeta.resolve(nombre).normalize();
        if (!candidato.startsWith(carpeta)) {
            throw new IllegalArgumentException("Ruta no válida.");
        }
        return candidato;
    }

    /**
     * Comprueba los bytes de cabecera contra el formato que se dice tener.
     *
     * No es una validacion completa del fichero —eso pedira decodificarlo— pero
     * si cierra el caso de subir cualquier cosa declarando un tipo permitido.
     */
    static boolean contenidoCoincide(byte[] b, int leidos, String extension) {
        if (leidos < 12) return false;

        return switch (extension) {
            case "jpg" -> b[0] == (byte) 0xFF && b[1] == (byte) 0xD8 && b[2] == (byte) 0xFF;
            case "png" -> b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                    && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A;
            case "gif" -> b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8';
            // RIFF....WEBP: el tamaño va en medio, de ahi el salto al byte 8.
            case "webp" -> b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                    && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            // MP4 y MOV comparten contenedor: "ftyp" empieza en el byte 4.
            case "mp4", "mov" -> b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p';
            // WebM es Matroska, que empieza con la firma EBML.
            case "webm" -> b[0] == (byte) 0x1A && b[1] == (byte) 0x45
                    && b[2] == (byte) 0xDF && b[3] == (byte) 0xA3;
            default -> false;
        };
    }

    public String tipoDeContenido(String nombre) {
        String extension = nombre.substring(nombre.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return TIPOS_PERMITIDOS.entrySet().stream()
                .filter(e -> e.getValue().equals(extension))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("application/octet-stream");
    }
}
