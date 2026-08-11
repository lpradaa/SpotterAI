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

    @Column(length = 280)
    private String biografia;

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

    public void setBiografia(String biografia) {
        this.biografia = biografia;
    }
}