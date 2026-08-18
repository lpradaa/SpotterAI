package com.spotterai.backend.models;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;

@Entity
@Table(name = "Usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "nivel")
    private String nivel;

    @Column(name = "objetivos")
    private String objetivos;
    
    // 🔥 NUEVO CAMPO: Para guardar el emoji del usuario
    @Column(name = "avatar")
    private String avatar;

    private Integer edad;
    private String genero;
    private Float peso;

    /**
     * En que idioma se le escribe.
     *
     * <p>Solo lo usan los correos. Todo lo demas sale de la cabecera
     * Accept-Language de cada peticion, pero los correos los manda un barrido
     * que corre solo: cuando se escribe no hay ninguna peticion de la que sacar
     * el idioma, asi que hay que tenerlo guardado.
     *
     * <p>Lo pone el selector de idioma, no un campo del formulario: dos sitios
     * para decidir lo mismo acaban diciendo cosas distintas.
     */
    @Column(nullable = false, length = 5)
    private String idioma = "es";

    @Column(length = 280)
    private String biografia;

    /**
     * La biografia convertida en numeros, para poder compararla con la de otra
     * persona.
     *
     * <p>Se calcula al guardar el perfil, no al emparejar: emparejar es un
     * producto escalar sobre esto, y por eso el noveno factor del motor no
     * cuesta ni una llamada de red. El formato lo define {@code VectorDeTexto},
     * que es lo unico que lo escribe y lo lee.
     *
     * <p>Puede ser nulo, y es un estado normal: alguien sin biografia, o alguien
     * que la guardo mientras el servicio de embeddings estaba caido. La
     * calculadora lo trata como cualquier otro dato que falta.
     */
    @Column(name = "biografia_vector")
    private byte[] biografiaVector;

    /**
     * De que texto salio el vector.
     *
     * <p>Sin esto no hay forma de saber si el vector guardado describe la
     * biografia actual o una anterior: alguien edita su bio con el servicio de
     * embeddings caido y el vector viejo se queda hablando de quien esa persona
     * ya no dice ser. Con la huella, un vector desfasado se detecta al comparar
     * y se recalcula.
     */
    @Column(name = "biografia_vector_de", length = 64)
    private String biografiaVectorDe;

    /**
     * Lo que la biografia dice sobre como quiere entrenar esta persona.
     *
     * <p>Sustituye al vector de 384 numeros, que comparaba dos biografias con la
     * similitud del coseno y resulto medir <b>parecido de redaccion</b> y no
     * compatibilidad: dos personas que querian lo contrario dicho con la misma
     * estructura puntuaban 0,843 y dos que querian lo mismo dicho con sus
     * palabras, 0,499. Esta medido en {@code docs/medir-el-motor.md}.
     *
     * <p>Tres numeros con nombre en vez de 384 opacos, y eso cambia dos cosas:
     * el factor se puede explicar en pantalla —«los dos buscais que os
     * exijan»— y un valor raro se ve mirando la fila en la base.
     *
     * <p><b>Null no es cero.</b> Es «esta persona no ha dicho nada de esto», y
     * la mitad de las biografias reales no hablan de la mitad de los ejes.
     * Ponerle un cero seria colocar a todo el que calla justo en el medio, que
     * es una opinion que nadie ha dado.
     */
    @Column(name = "intencion_exigencia")
    private Double intencionExigencia;

    @Column(name = "intencion_ambicion")
    private Double intencionAmbicion;

    @Column(name = "intencion_flexibilidad")
    private Double intencionFlexibilidad;

    /** De que texto salieron. Mismo papel que {@code biografiaVectorDe}. */
    @Column(name = "intenciones_de", length = 64)
    private String intencionesDe;

    /**
     * Entrenamientos que se propone hacer a la semana.
     *
     * Estaba en localStorage con la clave meta_semanal_<nombre>, asi que se
     * perdia al cambiar de navegador y dos personas con el mismo nombre
     * compartian el valor.
     */
    @Column(name = "meta_semanal")
    private Integer metaSemanal = 4;

    /**
     * Ruta de la foto de perfil, o null.
     *
     * Sin ella se usan las iniciales sobre color, que identifican igual de bien
     * y no obligan a nadie a subir nada para empezar a usar la aplicacion.
     */
    @Column(name = "foto_url")
    private String fotoUrl;

    /**
     * Clave del enum {@code Rutina}, o null si todavia no lo ha dicho.
     *
     * Se guarda como texto y no como enum de JPA a proposito: asi una fila con un
     * valor viejo o desconocido se lee como "sin rutina" en vez de reventar al
     * cargar el usuario entero.
     */
    @Column(length = 30)
    private String rutina;

    /**
     * Si quiere recibir avisos por correo.
     *
     * <p>true por defecto, porque quien se registra en una aplicacion para
     * emparejarse con gente espera que le avisen cuando alguien le escribe. Lo
     * que no puede faltar es la salida, y la salida es esto: una preferencia de
     * cada persona y no una propiedad del servidor, que es lo unico que habia y
     * apagaba los avisos de todo el mundo a la vez.
     */
    @Column(name = "avisos_por_correo", nullable = false)
    private boolean avisosPorCorreo = true;

    /**
     * La llave para darse de baja desde el propio correo, sin iniciar sesion.
     *
     * <p>Tiene que poder usarse sin sesion: quien quiere dejar de recibir
     * correos no va a iniciar sesion para conseguirlo, y obligarle a ello es la
     * forma educada de no dejarle salir.
     *
     * <p>Aleatoria y no derivada del id, para que nadie pueda dar de baja a
     * nadie probando numeros. No autentica ni abre sesion: lo unico que permite
     * es cambiar esta preferencia.
     *
     * <p>Se genera la primera vez que se le manda un aviso a esa persona, que es
     * el primer momento en que el enlace tiene que existir.
     */
    @Column(name = "token_avisos", length = 64, unique = true)
    private String tokenAvisos;

    /**
     * Desde cuando valen los tokens de esta persona.
     *
     * <p>Un JWT no se puede retirar: esta firmado y vale hasta que caduca, lo
     * tenga quien lo tenga. Esta marca es la forma barata de invalidarlos
     * igualmente, y hace falta justo donde mas importa: al cambiar la
     * contraseña. Si alguien te habia robado la sesion, cambiarla sin esto no lo
     * echa —sigue dentro veinticuatro horas—, y eso convierte el cambio de
     * contraseña en un gesto que tranquiliza sin arreglar nada.
     */
    @Column(name = "sesiones_validas_desde")
    private LocalDateTime sesionesValidasDesde;

    /**
     * La huella del token de restablecimiento, no el token.
     *
     * <p>Aqui se guarda en hash y {@link #tokenAvisos} no, y la diferencia no es
     * capricho: con la llave de baja, quien lea la base puede dejar a alguien
     * sin correos; con esta, puede entrar en su cuenta. Mientras vive es una
     * credencial en toda regla, asi que el valor de verdad solo existe dentro
     * del correo que se manda.
     */
    @Column(name = "token_reset", length = 64, unique = true)
    private String tokenReset;

    /** Corta a proposito: un enlace de recuperacion no tiene por que durar. */
    @Column(name = "token_reset_expira")
    private LocalDateTime tokenResetExpira;

    
    @ManyToOne
    @JoinColumn(name = "gimnasio_id")
    private Gimnasio gimnasio;

    /**
     * Si se desplazaria a otro gimnasio para entrenar acompañado.
     *
     * <p>Es el unico dato del emparejamiento que no se puede deducir de ningun
     * otro: ni del horario, ni del gimnasio, ni del historial. Solo lo sabe
     * quien lo decide, y por eso se pregunta en vez de inferirlo.
     *
     * <p>Falso por defecto. Suponer que si seria suponer que todo el mundo se
     * mueve, que es justo lo contrario de lo que pasa.
     */
    @Column(name = "puedo_desplazarme", nullable = false)
    private boolean puedoDesplazarme = false;

    @OneToMany(mappedBy = "usuario")
    private List<Disponibilidad> disponibilidades;

    // 🔥 NUEVO: Relación con el historial de entrenamientos
    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL)
    private List<Entrenamiento> entrenamientos;

    // Constructor vacío obligatorio para que Hibernate funcione
    public Usuario() {}

    // --- GETTERS Y SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Integer getEdad() { return edad; }
    public void setEdad(Integer edad) { this.edad = edad; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }

    public Float getPeso() { return peso; }
    public void setPeso(Float peso) { this.peso = peso; }

    public boolean isPuedoDesplazarme() { return puedoDesplazarme; }
    public void setPuedoDesplazarme(boolean puedoDesplazarme) { this.puedoDesplazarme = puedoDesplazarme; }

    public Gimnasio getGimnasio() { return gimnasio; }
    public void setGimnasio(Gimnasio gimnasio) { this.gimnasio = gimnasio; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public String getObjetivos() { return objetivos; }
    public void setObjetivos(String objetivos) { this.objetivos = objetivos; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Integer getMetaSemanal() { return metaSemanal; }
    public void setMetaSemanal(Integer metaSemanal) { this.metaSemanal = metaSemanal; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getRutina() { return rutina; }
    public void setRutina(String rutina) { this.rutina = rutina; }

    public boolean isAvisosPorCorreo() { return avisosPorCorreo; }
    public void setAvisosPorCorreo(boolean avisosPorCorreo) { this.avisosPorCorreo = avisosPorCorreo; }
    public String getTokenAvisos() { return tokenAvisos; }
    public void setTokenAvisos(String tokenAvisos) { this.tokenAvisos = tokenAvisos; }

    public LocalDateTime getSesionesValidasDesde() { return sesionesValidasDesde; }
    public void setSesionesValidasDesde(LocalDateTime f) { this.sesionesValidasDesde = f; }
    public String getTokenReset() { return tokenReset; }
    public void setTokenReset(String tokenReset) { this.tokenReset = tokenReset; }
    public LocalDateTime getTokenResetExpira() { return tokenResetExpira; }
    public void setTokenResetExpira(LocalDateTime f) { this.tokenResetExpira = f; }

    public List<Disponibilidad> getDisponibilidades() { return disponibilidades; }
    public void setDisponibilidades(List<Disponibilidad> disponibilidades) { this.disponibilidades = disponibilidades; }

    public List<Entrenamiento> getEntrenamientos() { return entrenamientos; }
    public void setEntrenamientos(List<Entrenamiento> entrenamientos) { this.entrenamientos = entrenamientos; }

    public String getBiografia() {
        return biografia;
    }

    public Double getIntencionExigencia() { return intencionExigencia; }
    public void setIntencionExigencia(Double v) { this.intencionExigencia = v; }

    public Double getIntencionAmbicion() { return intencionAmbicion; }
    public void setIntencionAmbicion(Double v) { this.intencionAmbicion = v; }

    public Double getIntencionFlexibilidad() { return intencionFlexibilidad; }
    public void setIntencionFlexibilidad(Double v) { this.intencionFlexibilidad = v; }

    public String getIntencionesDe() { return intencionesDe; }
    public void setIntencionesDe(String v) { this.intencionesDe = v; }

    public byte[] getBiografiaVector() { return biografiaVector; }
    public void setBiografiaVector(byte[] biografiaVector) { this.biografiaVector = biografiaVector; }

    public String getBiografiaVectorDe() { return biografiaVectorDe; }
    public void setBiografiaVectorDe(String biografiaVectorDe) { this.biografiaVectorDe = biografiaVectorDe; }

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }
}