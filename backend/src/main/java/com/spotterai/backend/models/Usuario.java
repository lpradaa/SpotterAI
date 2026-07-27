package com.spotterai.backend.models;

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

    
    @ManyToOne
    @JoinColumn(name = "gimnasio_id")
    private Gimnasio gimnasio;

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