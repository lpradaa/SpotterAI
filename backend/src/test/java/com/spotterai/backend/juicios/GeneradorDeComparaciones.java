package com.spotterai.backend.juicios;

import com.spotterai.backend.matching.CalculadoraCompatibilidad;
import com.spotterai.backend.matching.PerfilDeMatch;
import com.spotterai.backend.matching.PuntuacionCompatibilidad;
import com.spotterai.backend.models.Disponibilidad;
import com.spotterai.backend.models.Levantamiento;
import com.spotterai.backend.models.Usuario;
import com.spotterai.backend.repositories.DisponibilidadRepository;
import com.spotterai.backend.repositories.EntrenamientoRepository;
import com.spotterai.backend.repositories.LevantamientoRepository;
import com.spotterai.backend.matching.Rutina;
import com.spotterai.backend.repositories.UsuarioRepository;
import com.spotterai.backend.textos.Textos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prepara las comparaciones que se le van a enseñar a una persona.
 *
 * <h2>Qué pregunta hay detrás</h2>
 *
 * <p>Del motor esta medido cuanto mueve cada peso y en que se apoya. Lo que no
 * esta medido es si <b>acierta</b>, y eso no se puede saber contando: hace falta
 * que alguien de fuera diga cual de dos candidatos elegiria, y comparar su orden
 * con el nuestro.
 *
 * <p>Crear mas usuarios en la base no sirve para esto. El motor ya se mide sobre
 * 1.770 parejas sinteticas; mas parejas son mas preguntas, y lo que falta son
 * <b>respuestas</b>. Y si las genera quien escribio el motor, la medicion
 * compara el motor consigo mismo.
 *
 * <h2>Por que los pares no se eligen al azar</h2>
 *
 * <p>Un candidato de 90 contra uno de 20 lo acierta cualquiera: ese par solo
 * comprueba que el motor no esta roto, y eso ya lo comprueban las pruebas. Los
 * que informan son dos tipos:
 *
 * <ul>
 *   <li><b>Cercanos</b> — pocos puntos de diferencia. Es donde el orden del
 *       motor es fragil y donde una discrepancia significa algo.</li>
 *   <li><b>Con factores enfrentados</b> — uno gana por horario y el otro por
 *       todo lo demas. Ahi es donde el reparto 40/12/10/8 se esta jugando algo,
 *       porque la respuesta depende de si el horario vale de verdad lo que le
 *       hemos puesto.</li>
 * </ul>
 *
 * <h2>Como se ejecuta</h2>
 *
 * <p>Contra la base real, porque los perfiles tienen que ser legibles: nadie
 * puede opinar sobre «Perfil 23» con la biografia «Biografía de prueba 0.37».
 * Escribe un HTML autocontenido que se abre con doble clic y no necesita
 * servidor ni conexion.
 *
 * <pre>DB_HOST=localhost ./mvnw test -Dtest=GeneradorDeComparaciones</pre>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT:3306}/${DB_NAME:spotterai_db}"
                + "?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false",
        // Sin esto se queda el driver de H2 que fija el perfil de test, y
        // rechaza la URL de MySQL antes de intentar nada.
        "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
        "spring.datasource.username=${DB_USER:root}",
        "spring.datasource.password=${DB_PASSWORD:}",
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
})
@EnabledIfEnvironmentVariable(named = "DB_HOST", matches = ".+")
class GeneradorDeComparaciones {

    /** Cuantas comparaciones se le piden a una persona. Veinte son cinco minutos. */
    private static final int CUANTAS = 20;

    /**
     * Desde cuantos perfiles distintos se juzga.
     *
     * <p>Juzgar es relativo a alguien: «con cual entrenarias» no tiene sentido
     * sin un tu. Se reparten las comparaciones entre varios personajes para que
     * el resultado no dependa de las manias de uno solo.
     */
    private static final int PERSONAJES = 4;

    /** Diferencia de puntos por debajo de la cual dos candidatos son «cercanos». */
    private static final int CERCANOS = 8;

    @Autowired private UsuarioRepository usuarios;
    @Autowired private DisponibilidadRepository disponibilidades;
    @Autowired private LevantamientoRepository levantamientos;
    @Autowired private EntrenamientoRepository entrenamientos;
    @Autowired private Textos textos;

    /** Una comparacion: desde quien se juzga y entre quienes se elige. */
    private record Comparacion(PerfilDeMatch juez, PerfilDeMatch a, PerfilDeMatch b,
                               int puntosA, int puntosB, String motivo) {}

    @Test
    @DisplayName("Escribe el cuadernillo de comparaciones")
    void generar() throws Exception {
        List<PerfilDeMatch> gente = cargar();
        assertTrue(gente.size() >= 6,
                "Con menos de seis perfiles no hay comparaciones que merezca la pena enseñar");

        List<Comparacion> comparaciones = elegir(gente);
        assertTrue(comparaciones.size() >= 10,
                "Solo salieron " + comparaciones.size() + " comparaciones interesantes: "
                        + "o la poblacion es muy pequeña o todas las parejas puntuan igual");

        Path salida = Paths.get("..", "docs", "juicios", "comparaciones.html");
        Files.createDirectories(salida.getParent());
        Files.writeString(salida, html(comparaciones), StandardCharsets.UTF_8);

        System.out.println();
        System.out.println("=== Cuadernillo de comparaciones ===");
        System.out.printf("  %d comparaciones desde %d personajes, sobre %d perfiles%n",
                comparaciones.size(), PERSONAJES, gente.size());
        System.out.println("  " + salida.toAbsolutePath().normalize());
        System.out.println();
        comparaciones.forEach(c -> System.out.printf("  %-14s %s (%d) vs %s (%d)  [%s]%n",
                c.juez().usuario().getNombre(),
                c.a().usuario().getNombre(), c.puntosA(),
                c.b().usuario().getNombre(), c.puntosB(), c.motivo()));
        System.out.println();
    }

    // ------------------------------------------------------------------ datos

    private List<PerfilDeMatch> cargar() {
        return usuarios.findAll().stream()
                // Sin biografia no hay nada que leer, y sin horarios el motor no
                // puede puntuar: son los que el guardian deja pasar a Explorar.
                .filter(u -> u.getBiografia() != null && !u.getBiografia().isBlank())
                .map(u -> new PerfilDeMatch(u,
                        disponibilidades.findByUsuarioId(u.getId()),
                        levantamientos.findByUsuarioId(u.getId()),
                        null))
                .filter(p -> !p.horarios().isEmpty())
                .toList();
    }

    // ----------------------------------------------------------------- elegir

    private List<Comparacion> elegir(List<PerfilDeMatch> gente) {
        List<Comparacion> elegidas = new ArrayList<>();

        // Los personajes se reparten por la lista en vez de coger los primeros:
        // los primeros de la demo son los tres que mas encajan entre si, y
        // juzgar solo desde ellos daria un cuadernillo de casos faciles.
        int salto = Math.max(1, gente.size() / PERSONAJES);

        for (int i = 0; i < gente.size() && elegidas.size() < CUANTAS; i += salto) {
            PerfilDeMatch juez = gente.get(i);

            List<PerfilDeMatch> otros = gente.stream()
                    .filter(p -> !p.usuario().getId().equals(juez.usuario().getId()))
                    .toList();

            elegidas.addAll(paraEsteJuez(juez, otros,
                    CUANTAS / PERSONAJES - elegidas.size() / Math.max(1, PERSONAJES)));
        }

        return elegidas.stream().limit(CUANTAS).toList();
    }

    private List<Comparacion> paraEsteJuez(PerfilDeMatch juez, List<PerfilDeMatch> otros,
                                           int cuantas) {
        record Candidato(PerfilDeMatch perfil, PuntuacionCompatibilidad puntuacion) {}

        List<Candidato> candidatos = otros.stream()
                .map(o -> new Candidato(o, CalculadoraCompatibilidad.calcular(juez, o)))
                .sorted(Comparator.comparingInt(c -> -c.puntuacion().total()))
                .toList();

        List<Comparacion> salen = new ArrayList<>();

        for (int i = 0; i < candidatos.size() - 1 && salen.size() < Math.max(2, cuantas); i++) {
            Candidato uno = candidatos.get(i);
            Candidato otro = candidatos.get(i + 1);

            int diferencia = uno.puntuacion().total() - otro.puntuacion().total();
            String motivo = motivoDelPar(uno.perfil(), otro.perfil(),
                    uno.puntuacion(), otro.puntuacion(), diferencia);

            // Los pares obvios se descartan: no informan y gastan la paciencia
            // de quien contesta, que es el recurso escaso de todo esto.
            if (motivo == null) continue;

            salen.add(new Comparacion(juez, uno.perfil(), otro.perfil(),
                    uno.puntuacion().total(), otro.puntuacion().total(), motivo));
        }

        return salen;
    }

    /**
     * Por que este par merece preguntarse, o null si no lo merece.
     *
     * <h2>Lo que enseño el primer cuadernillo</h2>
     *
     * <p>La primera version solo miraba la diferencia de puntos, y el resultado
     * fue un cuadernillo donde <b>8 de 14 pares tenian a los dos candidatos
     * iguales en tres o mas de los cinco campos duros</b> —en dos de ellos,
     * iguales en los cuatro primeros—. Tiene sentido visto de cerca: dos
     * candidatos empatan en puntuacion sobre todo cuando se parecen en todo.
     *
     * <p>Y eso rompe el experimento sin que se note. Si los factores duros no
     * pueden desempatar, quien contesta decide con lo unico que queda —la
     * biografia— y entonces la medicion no dice si el motor ordena bien: dice
     * que el motor no tenia nada que decir ahi. El primer anotador lo noto solo:
     * «me he guiado mas por las biografias que por todo lo demas».
     *
     * <p>Asi que ahora un par cercano solo entra si los candidatos <b>difieren
     * de verdad</b> en algo duro.
     */
    private static String motivoDelPar(PerfilDeMatch a, PerfilDeMatch b,
                                       PuntuacionCompatibilidad puntosA,
                                       PuntuacionCompatibilidad puntosB,
                                       int diferencia) {
        if (Math.abs(diferencia) <= CERCANOS) {
            // Sin esto, «cercanos» acaba significando «casi el mismo perfil», y
            // preguntarlo solo mide lo bien que se leen dos biografias.
            return camposDurosDistintos(a, b) >= 2 ? "cercanos" : null;
        }

        // Factores enfrentados: uno gana claramente por un factor y pierde por
        // otro. Es donde la respuesta depende de si el reparto de pesos es el
        // bueno, que es justo lo que no sabemos.
        String dominanteA = puntosA.factorDominante() == null
                ? "" : puntosA.factorDominante().nombre();
        String dominanteB = puntosB.factorDominante() == null
                ? "" : puntosB.factorDominante().nombre();

        if (!dominanteA.equals(dominanteB)) return "factores enfrentados";
        return null;
    }

    /** En cuantos de los cuatro campos declarados NO coinciden dos personas. */
    private static int camposDurosDistintos(PerfilDeMatch a, PerfilDeMatch b) {
        Usuario uno = a.usuario();
        Usuario otro = b.usuario();

        int distintos = 0;
        if (!java.util.Objects.equals(uno.getNivel(), otro.getNivel())) distintos++;
        if (!java.util.Objects.equals(uno.getObjetivos(), otro.getObjetivos())) distintos++;
        if (!java.util.Objects.equals(uno.getRutina(), otro.getRutina())) distintos++;
        if (!java.util.Objects.equals(idDeGimnasio(uno), idDeGimnasio(otro))) distintos++;
        return distintos;
    }

    private static Long idDeGimnasio(Usuario u) {
        return u.getGimnasio() == null ? null : u.getGimnasio().getId();
    }

    // ------------------------------------------------------------------- html

    private String html(List<Comparacion> comparaciones) throws Exception {
        String plantilla = Files.readString(
                Paths.get("src", "test", "resources", "juicios", "plantilla.html"),
                StandardCharsets.UTF_8);

        String datos = comparaciones.stream().map(c -> """
                {"juez": %s, "a": %s, "b": %s, "puntosA": %d, "puntosB": %d, "motivo": "%s"}"""
                        .formatted(ficha(c.juez()), ficha(c.a()), ficha(c.b()),
                                c.puntosA(), c.puntosB(), c.motivo()))
                .collect(Collectors.joining(",\n"));

        return plantilla.replace("/*COMPARACIONES*/", "[\n" + datos + "\n]");
    }

    /**
     * Lo que se le enseña de una persona. Sin la puntuacion: si la ven, anclan.
     *
     * <p>Todo se redacta con {@link Textos}, igual que en la aplicacion. Sin eso
     * el cuadernillo enseña {@code TORSO_PIERNA} y {@code PESO_MUERTO 130.0 kg},
     * que son claves de la base: quien contesta no sabe nada del proyecto y no
     * tiene por que descifrar nada para opinar.
     */
    private String ficha(PerfilDeMatch p) {
        Usuario u = p.usuario();

        String horarios = p.horarios().stream()
                .sorted(Comparator.comparing(Disponibilidad::getDiaSemana))
                .map(d -> "%s %s-%s%s".formatted(d.getDiaSemana(),
                        d.getHoraInicio(), d.getHoraFin(), d.isHabitual() ? " (fijo)" : ""))
                .collect(Collectors.joining(" · "));

        String marcas = p.levantamientos().isEmpty() ? "sin marcas apuntadas"
                : p.levantamientos().stream()
                        .map((Levantamiento l) -> "%s %s kg × %s".formatted(
                                textos.de(l.getEjercicio().nombre()),
                                // Sin el .0: los kilos de una marca son enteros
                                // y el decimal solo hace ruido al leerlo.
                                Math.round(l.getPeso().doubleValue()), l.getRepeticiones()))
                        .collect(Collectors.joining(" · "));

        // Cuantas veces ha entrenado de verdad el ultimo mes. Es el unico dato
        // del perfil que no se declara, y sin el la ficha solo cuenta lo que
        // cada uno dice de si mismo — que es la mitad de lo que hay.
        long haceUnMes = entrenamientos.findByUsuarioIdOrderByFechaDesc(u.getId()).stream()
                .filter(e -> e.getFecha() != null
                        && e.getFecha().isAfter(java.time.LocalDate.now().minusDays(30)))
                .count();

        String constancia = haceUnMes == 0
                ? "sin entrenamientos apuntados"
                : "%d entrenamientos el último mes (se propone %d por semana)"
                        .formatted(haceUnMes, u.getMetaSemanal());

        return """
                {"nombre": "%s", "iniciales": "%s", "color": "%s", "edad": %s, "nivel": "%s",
                 "objetivo": "%s", "rutina": "%s", "gimnasio": "%s", "bio": "%s",
                 "horarios": "%s", "marcas": "%s", "constancia": "%s"}"""
                .formatted(escapar(u.getNombre()), iniciales(u.getNombre()),
                        u.getAvatar() == null ? "acero" : u.getAvatar(),
                        u.getEdad(), escapar(u.getNivel()),
                        escapar(u.getObjetivos()),
                        escapar(Rutina.desde(u.getRutina())
                                .map(r -> textos.de(r.nombre())).orElse("—")),
                        escapar(u.getGimnasio() == null ? "—" : u.getGimnasio().getNombre()),
                        escapar(u.getBiografia()), escapar(horarios), escapar(marcas),
                        escapar(constancia));
    }

    /** Las mismas que pinta el avatar de la aplicacion. */
    private static String iniciales(String nombre) {
        String[] partes = nombre.trim().split("\s+");
        String primera = partes[0].substring(0, 1);
        return (partes.length > 1 ? primera + partes[1].substring(0, 1) : primera).toUpperCase();
    }

    private static String escapar(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", " ");
    }
}
