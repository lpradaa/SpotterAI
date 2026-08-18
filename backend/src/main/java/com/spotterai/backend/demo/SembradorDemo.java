package com.spotterai.backend.demo;

import com.spotterai.backend.matching.Ejercicio;
import com.spotterai.backend.matching.Rutina;
import com.spotterai.backend.models.*;
import com.spotterai.backend.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Llena una base vacía con gente, horarios y planes.
 *
 * <p>Existe porque la promesa del README —clonar, un comando, y ya está— se
 * cumplía a medias: arrancaba, sí, pero lo que veías era una aplicación vacía.
 * Te registrabas y no había gimnasios que elegir, ni nadie con quien cruzarte,
 * ni por tanto nada del motor de compatibilidad, que es justamente lo único que
 * merece la pena enseñar. Todo el trabajo quedaba invisible en una instalación
 * nueva.
 *
 * <p>Solo con el perfil {@code demo} activo. Los datos de mentira dentro de una
 * migración se cuelan en cualquier instalación de verdad y ya no hay quien los
 * distinga de los reales; aquí se sabe siempre de dónde salió cada fila.
 *
 * <p>Todas las fechas son relativas a hoy. Con fechas fijas la demostración se
 * pudre sola: unas semanas después las sesiones «próximas» están en el pasado y
 * lo primero que ves es un plan caducado.
 *
 * <p>Nadie tiene foto, y es deliberado: las fotos son datos de usuario y no van
 * al repositorio. Las iniciales sobre color son el respaldo que la aplicación ya
 * usa cuando no hay foto, así que la demostración enseña el comportamiento real
 * en vez de uno inventado para la ocasión.
 */
@Component
@Profile("demo")
public class SembradorDemo implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SembradorDemo.class);

    /**
     * Los vectores de las biografias, ya calculados.
     *
     * <p>Para un despliegue que corra sin el servicio de embeddings asi que sin esto el factor semantico quedaria
     * "sin datos" para toda la gente de demostracion, que es justo lo que la
     * demo existe para enseñar.
     */
    private final VectoresSembrados vectores = new VectoresSembrados();
    private final IntencionesSembradas intenciones = new IntencionesSembradas();

    /** Con quién se entra. Va en el README y en la pantalla de bienvenida. */
    public static final String EMAIL_DEMO = "demo@spotterai.test";
    public static final String CLAVE_DEMO = "Demo1234";

    /** Todas las cuentas sembradas lo comparten: es como se reconoce la demostracion. */
    static final String DOMINIO_DEMO = "@spotterai.test";

    private final UsuarioRepository usuarios;
    private final GimnasioRepository gimnasios;
    private final DisponibilidadRepository disponibilidades;
    private final LevantamientoRepository levantamientos;
    private final SolicitudRepository solicitudes;
    private final MensajeRepository mensajes;
    private final SesionRepository sesiones;
    private final EntrenamientoRepository entrenamientos;
    private final HitoRepository hitos;
    private final PasswordEncoder cifrador;
    private final Clock reloj;

    public SembradorDemo(UsuarioRepository usuarios, GimnasioRepository gimnasios,
                         DisponibilidadRepository disponibilidades, LevantamientoRepository levantamientos,
                         SolicitudRepository solicitudes, MensajeRepository mensajes,
                         SesionRepository sesiones, EntrenamientoRepository entrenamientos,
                         HitoRepository hitos, PasswordEncoder cifrador, Clock reloj) {
        this.usuarios = usuarios;
        this.gimnasios = gimnasios;
        this.disponibilidades = disponibilidades;
        this.levantamientos = levantamientos;
        this.solicitudes = solicitudes;
        this.mensajes = mensajes;
        this.sesiones = sesiones;
        this.entrenamientos = entrenamientos;
        this.hitos = hitos;
        this.cifrador = cifrador;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Idempotente: el contenedor se reinicia y el volumen de la base
        // sobrevive. Sembrar dos veces dejaria todo duplicado.
        //
        // Se pregunta por CUALQUIER cuenta del dominio y no por la principal.
        // Preguntando solo por esa, borrar la cuenta de demostracion —que desde
        // hace poco se puede hacer desde la propia aplicacion— hacia que el
        // siguiente arranque intentara sembrarlo todo otra vez y reventara
        // contra la primera de las otras trece, que si seguia ahi. Y no fallaba
        // suave: se caia la aplicacion entera al arrancar, en el camino que
        // ejecuta cualquiera que clone esto y haga `docker compose up`.
        if (usuarios.existsByEmailEndingWith(DOMINIO_DEMO)) {
            if (usuarios.findByEmail(EMAIL_DEMO).isEmpty()) {
                // Media demostracion: alguien borro la cuenta principal. No se
                // vuelve a sembrar —duplicaria a los demas— pero callarselo
                // dejaria a quien arranca esto probando una clave que ya no
                // existe y sin ninguna pista.
                log.warn("Hay datos de demostración pero falta la cuenta {}. "
                        + "Entra con otra (misma clave: {}) o vacía la base para sembrarla de cero.",
                        EMAIL_DEMO, CLAVE_DEMO);
            } else {
                log.info("Datos de demostración ya presentes. Entra con {} / {}", EMAIL_DEMO, CLAVE_DEMO);
            }
            return;
        }

        log.info("Sembrando datos de demostración…");

        Gimnasio centro = gimnasio("McFit Madrid Centro", "Gran Vía 32", "Madrid", "28013");
        Gimnasio chamberi = gimnasio("Basic-Fit Chamberí", "Bravo Murillo 5", "Madrid", "28015");
        Gimnasio retiro = gimnasio("Altafit Retiro", "Menéndez Pelayo 12", "Madrid", "28009");

        // --- Tú ---
        // Perfil completo a propósito: así el tablero no abre con el aviso de
        // "te falta algo" tapando lo que se quiere enseñar.
        Usuario yo = persona("Alex Moreno", EMAIL_DEMO, CLAVE_DEMO, 28, "Intermedio",
                "Hipertrofia", centro, Rutina.TORSO_PIERNA, "ascua",
                "Llevo dos años entrenando en serio. Busco a alguien constante para los días de fuerza.");
        franjas(yo, fija("Lunes", "18:00", "20:00"), fija("Miércoles", "18:00", "20:00"),
                suelta("Viernes", "18:00", "20:30"));
        marcas(yo, marca(Ejercicio.PRESS_BANCA, 90, 5), marca(Ejercicio.SENTADILLA, 130, 3),
                marca(Ejercicio.PESO_MUERTO, 160, 2));

        // --- Los que encajan mucho: mismo gimnasio, mismas horas fijas, fuerza parecida ---
        Usuario marta = persona("Marta Ibáñez", "marta@spotterai.test", CLAVE_DEMO, 27, "Intermedio",
                "Hipertrofia", centro, Rutina.TORSO_PIERNA, "ciruela",
                "Torso/pierna cuatro días. Prefiero entrenar acompañada en los básicos.");
        franjas(marta, fija("Lunes", "18:00", "20:00"), fija("Miércoles", "18:30", "20:30"),
                suelta("Sábado", "10:00", "12:00"));
        marcas(marta, marca(Ejercicio.PRESS_BANCA, 62, 5), marca(Ejercicio.SENTADILLA, 105, 4),
                marca(Ejercicio.PESO_MUERTO, 130, 3));

        Usuario javi = persona("Javi Ortega", "javi@spotterai.test", CLAVE_DEMO, 30, "Intermedio",
                "Fuerza", centro, Rutina.TORSO_PIERNA, "acero",
                "Vengo de powerlifting. Necesito a alguien que pueda ayudarme en banca pesada.");
        franjas(javi, fija("Lunes", "18:00", "20:00"), fija("Miércoles", "18:00", "20:00"),
                fija("Viernes", "18:00", "20:00"));
        marcas(javi, marca(Ejercicio.PRESS_BANCA, 100, 3), marca(Ejercicio.SENTADILLA, 145, 2),
                marca(Ejercicio.PESO_MUERTO, 180, 1));

        // --- Encajan a medias: coinciden en algo pero no en todo ---
        Usuario lucia = persona("Lucía Ferrer", "lucia@spotterai.test", CLAVE_DEMO, 25, "Principiante",
                "Pérdida de peso", centro, Rutina.FULL_BODY, "ambar",
                "Empecé en enero. Todavía me da respeto la zona de peso libre.");
        franjas(lucia, suelta("Lunes", "19:00", "21:00"), suelta("Jueves", "19:00", "21:00"));
        marcas(lucia, marca(Ejercicio.SENTADILLA, 45, 8), marca(Ejercicio.PRESS_BANCA, 30, 8));

        Usuario diego = persona("Diego Salas", "diego@spotterai.test", CLAVE_DEMO, 34, "Avanzado",
                "Fuerza", centro, Rutina.EMPUJE_TIRON_PIERNA, "pizarra",
                "Empuje/tirón/pierna. Entreno temprano casi siempre.");
        franjas(diego, fija("Martes", "07:00", "09:00"), fija("Jueves", "07:00", "09:00"),
                suelta("Miércoles", "19:00", "20:30"));
        marcas(diego, marca(Ejercicio.PRESS_BANCA, 115, 3), marca(Ejercicio.PESO_MUERTO, 200, 1));

        Usuario noa = persona("Noa Prieto", "noa@spotterai.test", CLAVE_DEMO, 29, "Intermedio",
                "Resistencia", centro, Rutina.LIBRE, "oliva",
                "Corro y hago algo de fuerza. Me amoldo a lo que haga falta.");
        franjas(noa, suelta("Miércoles", "18:00", "19:30"), suelta("Viernes", "18:00", "20:00"));
        marcas(noa, marca(Ejercicio.SENTADILLA, 70, 6));

        Usuario hugo = persona("Hugo Bermejo", "hugo@spotterai.test", CLAVE_DEMO, 22, "Intermedio",
                "Hipertrofia", centro, Rutina.WEIDER, "ascua",
                "Un grupo por día, sin prisa. Pecho los lunes, como todo el mundo.");
        franjas(hugo, fija("Lunes", "19:00", "21:00"), suelta("Martes", "19:00", "21:00"));
        marcas(hugo, marca(Ejercicio.PRESS_BANCA, 80, 6), marca(Ejercicio.REMO_BARRA, 70, 8));

        // --- Encajan poco: otro gimnasio, u horas que no cruzan ---
        Usuario carmen = persona("Carmen Vidal", "carmen@spotterai.test", CLAVE_DEMO, 31, "Intermedio",
                "Hipertrofia", chamberi, Rutina.TORSO_PIERNA, "ciruela",
                "En Chamberí, casi siempre por la tarde. Me muevo si hace falta.");
        franjas(carmen, fija("Lunes", "18:00", "20:00"), fija("Jueves", "18:00", "20:00"));
        marcas(carmen, marca(Ejercicio.HIP_THRUST, 120, 8), marca(Ejercicio.SENTADILLA, 90, 5));
        // El caso que existe para que se vea funcionar: coincide con la cuenta
        // demo el lunes de 18 a 20 y los dos van siempre, pero en otro gimnasio.
        // Sin esto puntuaria bajo y no habria forma de saber por que; marcandolo
        // sube, y la explicacion dice "alguno de los dos puede desplazarse".
        // Sin nadie con esto puesto, la funcion estaba viva y era invisible.
        sePuedeDesplazar(carmen);

        Usuario ruben = persona("Rubén Casas", "ruben@spotterai.test", CLAVE_DEMO, 26, "Principiante",
                "Hipertrofia", retiro, Rutina.FULL_BODY, "acero",
                "Recién mudado al barrio, buscando con quién ir.");
        franjas(ruben, suelta("Martes", "20:00", "22:00"), suelta("Sábado", "11:00", "13:00"));

        Usuario elena = persona("Elena Sanz", "elena@spotterai.test", CLAVE_DEMO, 38, "Avanzado",
                "Fuerza", chamberi, Rutina.EMPUJE_TIRON_PIERNA, "oliva",
                "Compito en -63 kg. Entreno de mañana.");
        franjas(elena, fija("Martes", "08:00", "10:00"), fija("Viernes", "08:00", "10:00"));
        marcas(elena, marca(Ejercicio.SENTADILLA, 120, 2), marca(Ejercicio.PESO_MUERTO, 145, 1));

        // --- Perfiles a medio rellenar: el motor tiene que saber decirlo ---
        Usuario tomas = persona("Tomás Gil", "tomas@spotterai.test", CLAVE_DEMO, null, "Intermedio",
                null, centro, null, "pizarra", null);
        franjas(tomas, suelta("Lunes", "18:00", "20:00"));

        Usuario irene = persona("Irene Lago", "irene@spotterai.test", CLAVE_DEMO, 24, null,
                "Hipertrofia", null, Rutina.FULL_BODY, "ambar",
                "Acabo de empezar, todavía rellenando el perfil.");

        Usuario pablo = persona("Pablo Nieto", "pablo@spotterai.test", CLAVE_DEMO, 33, "Intermedio",
                "Hipertrofia", centro, Rutina.TORSO_PIERNA, "ascua",
                "Entreno a mediodía, que es cuando puedo escaparme del trabajo.");
        franjas(pablo, fija("Lunes", "13:00", "14:30"), fija("Miércoles", "13:00", "14:30"));
        marcas(pablo, marca(Ejercicio.PRESS_BANCA, 85, 5));

        Usuario sara = persona("Sara Ollero", "sara@spotterai.test", CLAVE_DEMO, 27, "Intermedio",
                "Hipertrofia", centro, Rutina.TORSO_PIERNA, "oliva",
                "Vuelvo después de una lesión de hombro. Voy con calma en empuje.");
        franjas(sara, fija("Miércoles", "18:00", "20:00"), suelta("Viernes", "18:00", "20:00"));
        marcas(sara, marca(Ejercicio.SENTADILLA, 100, 5), marca(Ejercicio.PRESS_BANCA, 50, 8));

        // --- Lo que ya tienes con alguien ---
        // La demostración no empieza en cero: se entra y ya hay algo que hacer,
        // que es como se ve el producto entero de un vistazo.
        conectados(yo, marta);
        conectados(yo, javi);
        pendienteHacia(lucia, yo);   // por responder, en el tablero
        pendienteHacia(yo, sara);    // enviada por ti, esperando

        conversacion(marta, yo,
                "¡Hola! Vi que coincidimos los lunes",
                "Sí, y los dos vamos siempre. ¿Te va bien empezar la semana que viene?",
                "Perfecto. Yo empiezo por torso");

        conversacion(javi, yo,
                "¿Cuánto mueves en banca?",
                "90 por 5. Tú andas por ahí también, ¿no?",
                "100 por 3. Justo lo que buscaba, alguien que pueda aguantarme la barra");

        // Una propuesta esperando respuesta: nada más entrar ya hay algo que
        // decidir, y se ve la mitad del producto sin tocar nada.
        sesion(marta, yo, proximoDia(DayOfWeek.MONDAY), "18:00", "20:00", centro,
                Sesion.PROPUESTA, "Empiezo por torso, ¿te viene bien?");

        // Una ya aceptada, por delante.
        sesion(yo, javi, proximoDia(DayOfWeek.WEDNESDAY), "18:00", "20:00", centro,
                Sesion.ACEPTADA, "Banca pesada, necesito que me la aguantes");

        // Y una que ya pasó y falta apuntar: el último eslabón del recorrido.
        Sesion pasada = sesion(javi, yo, hoy().minusDays(3), "18:00", "20:00", centro,
                Sesion.ACEPTADA, null);
        pasada.setConfirmadaProponente(true);
        sesiones.save(pasada);

        // --- Historial, para que la barra de la semana no salga a cero ---
        // Un mes de historial de verdad, no tres sesiones sueltas.
        //
        // Desde que la constancia entra en la puntuacion, sembrar a todo el mundo
        // con uno o dos entrenamientos hacia que la demostracion enseñara a una
        // comunidad entera de gente que casi no va al gimnasio: todas las notas
        // salian con el descuento por evidencia y el motor parecia mas severo de
        // lo que es. Aqui cada uno entrena al ritmo que dice su perfil.
        rutinaDeUnMes(yo, 3, "Fuerza (Torso)", "McFit Madrid Centro");
        rutinaDeUnMes(marta, 4, "Fuerza (Torso)", "McFit Madrid Centro");
        rutinaDeUnMes(javi, 3, "Fuerza", "McFit Madrid Centro");
        rutinaDeUnMes(sara, 3, "Fuerza (Torso)", "McFit Madrid Centro");
        rutinaDeUnMes(hugo, 2, "Pecho", "McFit Madrid Centro");
        rutinaDeUnMes(diego, 4, "Empuje", "McFit Madrid Centro");
        rutinaDeUnMes(pablo, 2, "Fuerza (Torso)", "McFit Madrid Centro");
        rutinaDeUnMes(noa, 2, "Carrera y fuerza", null);
        rutinaDeUnMes(carmen, 3, "Fuerza (Torso)", "Basic-Fit Chamberí");
        rutinaDeUnMes(elena, 4, "Empuje", "Basic-Fit Chamberí");

        // Y dos que casi no aparecen, para que se vea que el motor lo nota: uno
        // que lo dejo hace semanas y otro que no ha registrado nunca nada.
        entrenamiento(lucia, hoy().minusDays(26), "Cardio suave", 30, null);

        hito(yo, "Primer 160 en muerto", "Dos años para llegar aquí.", hoy().minusDays(20));
        hito(javi, "100 kg en banca", "Por fin, a la tercera.", hoy().minusDays(9));
        hito(marta, "Un mes sin fallar un día", "Cuatro días por semana, cuatro semanas.", hoy().minusDays(5));

        log.info("Datos de demostración listos. Entra con {} / {}", EMAIL_DEMO, CLAVE_DEMO);
    }

    // ================= Apoyo =================

    private LocalDate hoy() {
        return LocalDate.now(reloj);
    }

    /**
     * El próximo día de la semana que toque, sin contar hoy.
     *
     * <p>Hoy no cuenta a propósito: si la demostración se arranca un lunes por
     * la noche, la «próxima sesión del lunes» sería una hora que ya ha pasado.
     */
    private LocalDate proximoDia(DayOfWeek dia) {
        LocalDate fecha = hoy().plusDays(1);
        while (fecha.getDayOfWeek() != dia) fecha = fecha.plusDays(1);
        return fecha;
    }

    private Gimnasio gimnasio(String nombre, String direccion, String ciudad, String cp) {
        Gimnasio g = new Gimnasio();
        g.setNombre(nombre);
        g.setDireccion(direccion);
        g.setCiudad(ciudad);
        g.setCodigoPostal(cp);
        return gimnasios.save(g);
    }

    private Usuario persona(String nombre, String email, String clave, Integer edad, String nivel,
                            String objetivos, Gimnasio gimnasio, Rutina rutina, String avatar,
                            String biografia) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(cifrador.encode(clave));
        u.setEdad(edad);
        u.setNivel(nivel);
        u.setObjetivos(objetivos);
        u.setGimnasio(gimnasio);
        u.setRutina(rutina == null ? null : rutina.name());
        u.setAvatar(avatar);
        u.setBiografia(biografia);
        u.setMetaSemanal(4);
        vectores.aplicarA(u);
        // Los ejes son los que puntuan desde la V19; el vector se sigue
        // sembrando mientras la columna exista.
        intenciones.aplicarA(u);
        return usuarios.save(u);
    }

    /**
     * Marca que esta persona se desplazaría a otro gimnasio.
     *
     * <p>Aparte de {@code persona(...)} y no un parámetro más: esa firma ya
     * tiene diez y añadirle un booleano suelto al final significaría escribir
     * {@code false} catorce veces para el único caso que lo usa.
     */
    private void sePuedeDesplazar(Usuario u) {
        u.setPuedoDesplazarme(true);
        usuarios.save(u);
    }

    /** Franja a la que se va siempre. Es la señal más fuerte del motor. */
    private static String[] fija(String dia, String desde, String hasta) {
        return new String[]{dia, desde, hasta, "si"};
    }

    /** Franja de "podría ir". */
    private static String[] suelta(String dia, String desde, String hasta) {
        return new String[]{dia, desde, hasta, "no"};
    }

    private void franjas(Usuario usuario, String[]... tramos) {
        for (String[] t : tramos) {
            disponibilidades.save(new Disponibilidad(t[0], LocalTime.parse(t[1]),
                    LocalTime.parse(t[2]), usuario, "si".equals(t[3])));
        }
    }

    private static Object[] marca(Ejercicio ejercicio, double peso, int repeticiones) {
        return new Object[]{ejercicio, peso, repeticiones};
    }

    private void marcas(Usuario usuario, Object[]... datos) {
        for (Object[] d : datos) {
            Levantamiento l = new Levantamiento();
            l.setUsuario(usuario);
            l.setEjercicio((Ejercicio) d[0]);
            l.setPeso((Double) d[1]);
            l.setRepeticiones((Integer) d[2]);
            levantamientos.save(l);
        }
    }

    private void conectados(Usuario uno, Usuario otro) {
        solicitud(uno, otro, "ACEPTADA");
    }

    private void pendienteHacia(Usuario emisor, Usuario receptor) {
        solicitud(emisor, receptor, "PENDIENTE");
    }

    private void solicitud(Usuario emisor, Usuario receptor, String estado) {
        Solicitud s = new Solicitud();
        s.setEmisor(emisor);
        s.setReceptor(receptor);
        s.setEstado(estado);
        solicitudes.save(s);
    }

    /**
     * Los mensajes se alternan entre los dos, empezando por el primero.
     *
     * <p>Repartidos hacia atras en el tiempo y no todos con la hora del
     * arranque. Antes quedaban todos en el mismo segundo, y se veia una charla
     * entera —"¿te viene bien el martes?", "hecho"— dicha de golpe: ni parece
     * una conversacion ni deja ver las marcas de dia del chat, que existen justo
     * para eso.
     *
     * <p>El ultimo cae hoy y los anteriores van subiendo hacia atras, un rato
     * antes cada uno y cambiando de dia cada dos. Asi la conversacion arranca
     * hace unos dias y termina reciente, que es la forma que tiene una de
     * verdad.
     */
    private void conversacion(Usuario uno, Usuario otro, String... textos) {
        LocalDateTime ahora = LocalDateTime.now(reloj);

        for (int i = 0; i < textos.length; i++) {
            Mensaje m = new Mensaje();
            m.setEmisor(i % 2 == 0 ? uno : otro);
            m.setReceptor(i % 2 == 0 ? otro : uno);
            m.setContenido(textos[i]);
            m.setLeido(true);

            int desdeElFinal = textos.length - 1 - i;
            m.setFechaEnvio(ahora
                    .minusDays(desdeElFinal / 2L)
                    .minusMinutes(desdeElFinal * 37L));

            mensajes.save(m);
        }
    }

    private Sesion sesion(Usuario proponente, Usuario invitado, LocalDate fecha,
                          String desde, String hasta, Gimnasio gimnasio, String estado, String nota) {
        Sesion s = new Sesion();
        s.setProponente(proponente);
        s.setInvitado(invitado);
        s.setFecha(fecha);
        s.setHoraInicio(LocalTime.parse(desde));
        s.setHoraFin(LocalTime.parse(hasta));
        s.setGimnasio(gimnasio);
        s.setEstado(estado);
        s.setNota(nota);
        s.setCreadaEn(LocalDateTime.now(reloj).minusDays(1));
        if (!Sesion.PROPUESTA.equals(estado)) {
            s.setRespondidaEn(LocalDateTime.now(reloj).minusHours(20));
        }
        return sesiones.save(s);
    }

    /**
     * Cuatro semanas entrenando al ritmo indicado.
     *
     * <p>Con una separacion regular entre sesiones, que es lo que hace un
     * habito: sembrar todas seguidas daria el mismo numero pero no se pareceria
     * a nadie.
     */
    private void rutinaDeUnMes(Usuario usuario, int porSemana, String tipo, String lugar) {
        int cada = Math.max(1, 7 / porSemana);
        for (int dia = 1; dia <= 28; dia += cada) {
            entrenamiento(usuario, hoy().minusDays(dia), tipo, 60 + (dia % 4) * 15, lugar);
        }
    }

    private void entrenamiento(Usuario usuario, LocalDate fecha, String tipo,
                               Integer minutos, String lugar) {
        Entrenamiento e = new Entrenamiento();
        e.setUsuario(usuario);
        e.setFecha(fecha);
        e.setTipo(tipo);
        e.setDuracionMinutos(minutos);
        e.setLugarONotas(lugar);
        entrenamientos.save(e);
    }

    private void hito(Usuario usuario, String titulo, String descripcion, LocalDate fecha) {
        Hito h = new Hito();
        h.setUsuario(usuario);
        h.setTitulo(titulo);
        h.setDescripcion(descripcion);
        h.setFecha(fecha);
        hitos.save(h);
    }
}
