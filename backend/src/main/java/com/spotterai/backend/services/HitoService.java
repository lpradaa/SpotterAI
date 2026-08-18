package com.spotterai.backend.services;

import com.spotterai.backend.textos.ErrorDeNegocio;
import com.spotterai.backend.textos.ErrorDePermiso;
import com.spotterai.backend.dtos.HitoDTO;
import com.spotterai.backend.models.Hito;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.HitoRepository;
import com.spotterai.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Marcas y logros que la gente publica en su perfil.
 *
 * <p>Tope deliberado: un perfil no es un muro. Con veinte hitos la ficha deja de
 * poder leerse de un vistazo y el que importa se pierde entre los demas.
 */
@Service
public class HitoService {

    static final int MAX_POR_USUARIO = 12;
    static final int MAX_TITULO = 120;
    static final int MAX_DESCRIPCION = 500;

    private final HitoRepository hitoRepository;
    private final UsuarioRepository usuarioRepository;

    public HitoService(HitoRepository hitoRepository, UsuarioRepository usuarioRepository) {
        this.hitoRepository = hitoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<HitoDTO> mios(String email) {
        Usuario yo = buscar(email);
        return hitoRepository.findByUsuarioIdOrderByFechaDescIdDesc(yo.getId())
                .stream().map(UsuarioServiceImpl::aHitoDTO).toList();
    }

    public HitoDTO crear(String email, String titulo, String descripcion, LocalDate fecha,
                         String medioUrl, String medioTipo) {
        Usuario yo = buscar(email);

        String tituloLimpio = titulo == null ? "" : titulo.trim();
        if (tituloLimpio.isEmpty()) {
            throw ErrorDeNegocio.de("error.hito.sinTitulo");
        }
        if (hitoRepository.findByUsuarioIdOrderByFechaDescIdDesc(yo.getId()).size() >= MAX_POR_USUARIO) {
            throw new IllegalArgumentException(
                    "Has llegado al máximo de " + MAX_POR_USUARIO + " hitos. Borra alguno para añadir otro.");
        }

        Hito hito = new Hito();
        hito.setUsuario(yo);
        hito.setTitulo(recortar(tituloLimpio, MAX_TITULO));
        hito.setDescripcion(recortar(descripcion, MAX_DESCRIPCION));
        // Una marca conseguida mañana no existe todavia.
        hito.setFecha(fecha == null || fecha.isAfter(LocalDate.now()) ? LocalDate.now() : fecha);
        hito.setMedioUrl(medioUrl);
        hito.setMedioTipo(medioTipo);

        return UsuarioServiceImpl.aHitoDTO(hitoRepository.save(hito));
    }

    public void borrar(String email, Long hitoId) {
        Usuario yo = buscar(email);
        Hito hito = hitoRepository.findById(hitoId)
                .orElseThrow(() -> new IllegalArgumentException("Ese hito no existe."));

        if (!hito.getUsuario().getId().equals(yo.getId())) {
            throw ErrorDePermiso.de("error.permiso.hitoAjeno");
        }
        hitoRepository.delete(hito);
    }

    private Usuario buscar(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null) return null;
        String limpio = texto.trim();
        if (limpio.isEmpty()) return null;
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}
