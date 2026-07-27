import { Component, signal, computed, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UsuarioService, Match, ExplicacionMatch } from '../../services/usuario.service';
import { EventosService } from '../../services/eventos.service';
import { PerfilEstadoService } from '../../services/perfil-estado.service';
import { AvisosService } from '../../services/avisos.service';
import { RejillaSemana } from '../rejilla-semana/rejilla-semana';
import { Avatar, COLORES_AVATAR } from '../avatar/avatar';
import { PerfilPublicoComponent } from '../perfil-publico/perfil-publico';
import { PerfilesService, Hito } from '../../services/perfiles.service';
import { SesionesService, Sesion } from '../../services/sesiones.service';
import { Carga } from '../carga/carga';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RejillaSemana, Avatar, PerfilPublicoComponent, Carga],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class DashboardComponent implements OnInit {
  private usuarioService = inject(UsuarioService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private eventos = inject(EventosService);
  private perfilEstado = inject(PerfilEstadoService);
  private avisos = inject(AvisosService);
  private perfiles = inject(PerfilesService);
  private sesionesService = inject(SesionesService);
  private destroyRef = inject(DestroyRef);

  userName = signal(localStorage.getItem('usuario_nombre') || 'Usuario');

  /**
   * Lo que tienes pendiente con alguien: propuestas por responder, sesiones por
   * delante y las que ya pasaron sin apuntar.
   *
   * Va por delante del resto del tablero. Buscar compañeros nuevos puede
   * esperar; contestarle a uno que ya tienes, no.
   */
  sesiones = signal<Sesion[]>([]);

  // --- Progreso semanal ---
  completedDays = signal(0);
  totalDays = signal(4);
  progressPercentage = signal(0);

  // --- Matches ---
  matches = signal<Match[]>([]);
  solicitudesPendientes: any[] = [];

  /** Candidatos que aún se pueden sugerir: ni conectados ni con solicitud en curso. */
  sugerencias = computed(() =>
    this.matches().filter(m => !m.yaConectado && !m.solicitudPendiente)
  );

  /** El mejor candidato sin conectar, para el titular de la tarjeta de bienvenida. */
  mejorSugerencia = computed(() => this.sugerencias()[0] ?? null);

  // --- Gimnasios ---
  gimnasios: any[] = [];
  nuevoGimnasioNombre: string = '';
  mostrarInputGimnasio: boolean = false;

  // --- Modal de perfil ---
  isModalOpen = false;
  /**
   * Colores de identidad. Antes esto era una rejilla de emoticonos, que además
   * de no parecer un producto serio no identificaba a nadie: dos personas con
   * el mismo emoji eran indistinguibles en una lista.
   */
  coloresAvatar = COLORES_AVATAR;
  diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

  perfilForm: any = {
    avatar: '', edad: null, genero: '', peso: null, nivel: '',
    objetivos: '', gimnasioId: null, nuevoGimnasioNombre: '', biografia: '',
    horarios: [], levantamientos: [], metaSemanal: 4, rutina: ''
  };

  /** Coincide con el limite de la columna y con el recorte del servidor. */
  readonly maxBiografia = 280;

  // --- Levantamientos ---
  /** El catálogo lo manda el backend: duplicarlo aquí es garantizar que diverjan. */
  ejerciciosDisponibles = signal<{ clave: string, nombre: string }[]>([]);

  /** Igual que el de ejercicios: el catálogo de rutinas lo manda el backend. */
  rutinasDisponibles = signal<{ clave: string, nombre: string }[]>([]);

  /** Cuántos están completos, que es lo que de verdad cuenta para el motor. */
  levantamientosPuestos = computed(() =>
    (this.perfilForm.levantamientos ?? [])
      .filter((l: any) => l.ejercicio && l.peso > 0 && l.repeticiones > 0).length);

  anadirLevantamiento(): void {
    if (this.perfilForm.levantamientos.length >= 3) return;
    this.perfilForm.levantamientos.push({ ejercicio: '', peso: null, repeticiones: null });
  }

  quitarLevantamiento(indice: number): void {
    this.perfilForm.levantamientos.splice(indice, 1);
  }

  /**
   * Lo que le falta al perfil, en puntos de compatibilidad perdidos.
   *
   * Los números los da el backend, que es donde viven los pesos del cálculo.
   * Copiarlos aquí sería garantizar que algún día digan cosas distintas.
   */
  rendimiento = signal<{ puntosEnJuego: number, huecos: any[] } | null>(null);

  puntosEnJuego = computed(() => this.rendimiento()?.puntosEnJuego ?? 0);

  /** Solo el más caro: una lista de cinco cosas no la lee nadie. */
  huecoPrincipal = computed(() => this.rendimiento()?.huecos?.[0] ?? null);

  restanteBiografia(): number {
    return this.maxBiografia - (this.perfilForm.biografia?.length ?? 0);
  }

  // --- Modal de entrenamiento ---
  isEntrenamientoModalOpen = false;
  historialEntrenamientos: any[] = [];
  nuevoEntrenamiento: any = { fecha: '', tipo: '', duracionMinutos: null, lugarONotas: '' };

  // --- Modal de sugerencias ---
  isSugerenciasOpen = signal(false);
  indiceActual = signal(0);
  explicacion = signal<ExplicacionMatch | null>(null);
  cargandoExplicacion = signal(false);

  /** El candidato que se está mostrando ahora mismo en el modal. */
  sugerenciaActual = computed(() => this.sugerencias()[this.indiceActual()] ?? null);

  // --- Avisos ---
  toast: { show: boolean, message: string, type: 'success' | 'error' } = { show: false, message: '', type: 'success' };
  private toastTimeout: any;

  ngOnInit(): void {
    this.cargarMatches();
    this.cargarHistorialEntrenamientos();
    this.cargarSolicitudesPendientes();
    this.cargarGimnasios();
    this.cargarMiPerfil();
    this.cargarSesiones();
    this.escucharEventos();
  }

  /**
   * Esto es lo que más se echaba en falta: una solicitud sin responder no se veía
   * hasta recargar la página, y mientras tanto se pierde un compañero.
   */
  private escucharEventos(): void {
    this.eventos.solicitudes
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(solicitud => {
        this.solicitudesPendientes = [solicitud, ...this.solicitudesPendientes];
        this.mostrarToast(`${solicitud.emisorNombre} quiere entrenar contigo.`, 'success');
        this.cdr.detectChanges();
      });

    // Una propuesta que llega mientras miras el tablero. Se avisa y se recarga
    // la lista entera en vez de insertarla a mano: son pocas y el orden lo
    // decide el servidor por fecha.
    this.eventos.sesiones
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((sesion: Sesion) => {
        this.mostrarToast(`${sesion.conNombre} propone entrenar contigo.`, 'success');
        this.cargarSesiones();
      });

    this.eventos.sesionesRespondidas
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((sesion: Sesion) => {
        if (sesion.estado === 'ACEPTADA') {
          this.mostrarToast(`${sesion.conNombre} ha aceptado entrenar contigo.`, 'success');
        }
        this.cargarSesiones();
      });

    this.eventos.respuestas
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(solicitud => {
        const aceptada = solicitud.estado === 'ACEPTADA';
        this.mostrarToast(
          aceptada
            ? `${solicitud.receptorNombre} ha aceptado tu solicitud.`
            : `${solicitud.receptorNombre} ha rechazado tu solicitud.`,
          aceptada ? 'success' : 'error'
        );
        // Cambia si el candidato pasa a conectado o vuelve a estar disponible.
        this.cargarMatches();
      });
  }

  private cargarMiPerfil(): void {
    this.usuarioService.getMiPerfil().subscribe({
      next: (data) => {
        if (!data) return;
        if (data.nombre) this.userName.set(data.nombre);

        this.perfilForm = {
          avatar: data.avatar || '', edad: data.edad, genero: data.genero, peso: data.peso,
          // Sin defecto inventado: rellenar 'Intermedio' hacía que la barra
          // mostrara un nivel que nadie había elegido, al lado de un aviso que
          // decía que faltaba justo eso.
          nivel: data.nivel || '', objetivos: data.objetivos || '', gimnasioId: data.gimnasioId,
          biografia: data.biografia || '',
          horarios: data.horarios || [],
          levantamientos: data.levantamientos || [],
          metaSemanal: data.metaSemanal || 4,
          rutina: data.rutina || ''
        };
        this.ejerciciosDisponibles.set(data.ejerciciosDisponibles || []);
        this.rutinasDisponibles.set(data.rutinasDisponibles || []);
        this.perfilForm.fotoUrl = data.fotoUrl ?? null;
        this.fotoActual.set(data.fotoUrl ?? null);
        this.rendimiento.set(data.rendimiento ?? null);
        this.cargarMisHitos();
        this.calcularProgresoSemanal();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error al cargar mi perfil:', err)
    });
  }

  // --- Gimnasios ---
  cargarGimnasios(): void {
    this.usuarioService.getGimnasios().subscribe({
      next: (data) => { this.gimnasios = data; this.cdr.detectChanges(); },
      error: (err) => console.error('Error al cargar gimnasios:', err)
    });
  }

  toggleNuevoGimnasio(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.mostrarInputGimnasio = (select.value === 'NUEVO');
    if (!this.mostrarInputGimnasio) this.nuevoGimnasioNombre = '';
    this.cdr.detectChanges();
  }

  mostrarToast(mensaje: string, tipo: 'success' | 'error' = 'success'): void {
    this.toast = { show: true, message: mensaje, type: tipo };
    this.cdr.detectChanges();

    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    this.toastTimeout = setTimeout(() => {
      this.toast.show = false;
      this.cdr.detectChanges();
    }, 3000);
  }

  // ---------------------------------------------------------------------------
  // Sugerencias
  //
  // La lista ya llega puntuada y ordenada por el backend, así que abrir el modal
  // es instantáneo: no hay ninguna espera artificial como antes. Lo que sí cuesta
  // una llamada al modelo es la explicación redactada, y por eso se pide de una
  // en una, solo para la ficha que se está viendo.
  // ---------------------------------------------------------------------------
  abrirSugerencias(): void {
    if (this.sugerencias().length === 0) {
      this.mostrarToast('No hay sugerencias nuevas por ahora.', 'error');
      return;
    }
    this.indiceActual.set(0);
    this.isSugerenciasOpen.set(true);
    this.cargarExplicacion();
  }

  cerrarSugerencias(): void {
    this.isSugerenciasOpen.set(false);
    this.explicacion.set(null);
  }

  siguienteSugerencia(): void {
    if (this.indiceActual() < this.sugerencias().length - 1) {
      this.indiceActual.update(i => i + 1);
      this.cargarExplicacion();
    }
  }

  anteriorSugerencia(): void {
    if (this.indiceActual() > 0) {
      this.indiceActual.update(i => i - 1);
      this.cargarExplicacion();
    }
  }

  private cargarExplicacion(): void {
    const candidato = this.sugerenciaActual();
    if (!candidato) return;

    this.explicacion.set(null);
    this.cargandoExplicacion.set(true);

    this.usuarioService.getExplicacionMatch(candidato.id).subscribe({
      next: (texto) => {
        // La respuesta puede llegar tarde: si el usuario ya ha pasado a otra
        // ficha, se descarta en vez de pintar la explicación de otra persona.
        if (this.sugerenciaActual()?.id !== candidato.id) return;
        this.explicacion.set(texto);
        this.cargandoExplicacion.set(false);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('No se pudo cargar la explicación del match:', err);
        if (this.sugerenciaActual()?.id !== candidato.id) return;
        this.cargandoExplicacion.set(false);
        this.cdr.detectChanges();
      }
    });
  }

  conectarConSugerenciaActual(): void {
    const candidato = this.sugerenciaActual();
    if (!candidato) return;
    this.conectarConUsuario(candidato.id);
    this.cerrarSugerencias();
  }

  // --- Progreso semanal ---
  calcularProgresoSemanal(): void {
    // La meta viene del perfil, que viene de la base. Antes se leia de
    // localStorage con la clave meta_semanal_<nombre>, asi que cambiabas de
    // navegador y desaparecia, y dos usuarios homonimos compartian valor.
    const meta = this.perfilForm.metaSemanal || 4;
    this.totalDays.set(meta);

    const hoy = new Date();
    const diaSemana = hoy.getDay() === 0 ? 7 : hoy.getDay();
    const lunes = new Date(hoy);
    lunes.setDate(hoy.getDate() - diaSemana + 1);
    lunes.setHours(0, 0, 0, 0);

    const totalEntrenos = this.historialEntrenamientos
      .filter(ent => new Date(ent.fecha) >= lunes).length;

    this.completedDays.set(totalEntrenos);
    this.progressPercentage.set(Math.min(100, (totalEntrenos / meta) * 100));
  }

  actualizarMetaDesdeSlider(event: Event): void {
    const nuevoValor = parseInt((event.target as HTMLInputElement).value, 10);
    this.perfilForm.metaSemanal = nuevoValor;
    // No se guarda al arrastrar: el control vive dentro del formulario de perfil
    // y se persiste al darle a guardar, como todo lo demas.
    this.calcularProgresoSemanal();
  }

  // --- Matches y solicitudes ---
  cargarMatches(): void {
    this.usuarioService.getMatches().subscribe({
      next: (data) => { this.matches.set(data || []); this.cdr.detectChanges(); },
      error: (err) => console.error('Error al cargar los matches:', err)
    });
  }

  conectarConUsuario(usuarioId: number): void {
    this.usuarioService.enviarSolicitudConexion(usuarioId).subscribe({
      next: () => {
        this.mostrarToast('Solicitud enviada correctamente.');
        this.matches.update(lista =>
          lista.map(m => m.id === usuarioId ? { ...m, solicitudPendiente: true } : m));
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.mostrarToast('No se pudo enviar la solicitud.', 'error');
      }
    });
  }

  cargarSolicitudesPendientes(): void {
    this.usuarioService.obtenerSolicitudesPendientes().subscribe({
      next: (data) => { this.solicitudesPendientes = data || []; this.cdr.detectChanges(); },
      error: (err) => console.error('Error al cargar la bandeja de solicitudes:', err)
    });
  }

  // --- Mi foto y mis marcas ---
  misHitos = signal<Hito[]>([]);
  subiendo = signal(false);
  nuevoHito = { titulo: '', descripcion: '', fecha: '', medioUrl: null as string | null, medioTipo: null as string | null };

  /**
   * Señal aparte y no un computed sobre perfilForm: perfilForm es un objeto
   * plano, así que cambiarle un campo no notifica a nadie y la foto no se
   * refrescaría al subirla.
   */
  fotoActual = signal<string | null>(null);
  subiendoFoto = signal(false);

  urlFoto = computed(() => this.perfiles.urlDeMedio(this.fotoActual()));

  alElegirFoto(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    if (!archivo) return;

    this.subiendoFoto.set(true);
    this.perfiles.subirMedio(archivo).subscribe({
      next: r => {
        this.perfilForm.fotoUrl = r.url;
        this.fotoActual.set(r.url);
        this.subiendoFoto.set(false);
        // Sin esto, elegir el mismo archivo dos veces seguidas no dispara change
        entrada.value = '';
        this.cdr.detectChanges();
      },
      error: err => {
        this.subiendoFoto.set(false);
        entrada.value = '';
        this.mostrarToast(
          typeof err?.error === 'string' ? err.error : 'No se ha podido subir la foto.', 'error');
      }
    });
  }

  quitarFoto(): void {
    this.perfilForm.fotoUrl = null;
    this.fotoActual.set(null);
    this.cdr.detectChanges();
  }

  urlMedio(ruta: string | null): string | null {
    return this.perfiles.urlDeMedio(ruta);
  }

  private cargarMisHitos(): void {
    this.perfiles.misHitos().subscribe({
      next: lista => { this.misHitos.set(lista); this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  /** Foto del hito que se está redactando. */
  alElegirMedio(evento: Event): void {
    const archivo = (evento.target as HTMLInputElement).files?.[0];
    if (!archivo) return;

    this.subiendo.set(true);
    this.perfiles.subirMedio(archivo).subscribe({
      next: r => {
        this.nuevoHito.medioUrl = r.url;
        this.nuevoHito.medioTipo = r.tipo;
        this.subiendo.set(false);
        this.cdr.detectChanges();
      },
      error: err => {
        this.subiendo.set(false);
        // El servidor explica el motivo (tipo no admitido, pasa de 15 MB…) y
        // ese texto es más útil que uno genérico nuestro.
        this.mostrarToast(typeof err?.error === 'string' ? err.error : 'No se ha podido subir el archivo.', 'error');
      }
    });
  }

  anadirHito(): void {
    if (!this.nuevoHito.titulo.trim()) return;

    this.perfiles.crearHito({
      titulo: this.nuevoHito.titulo,
      descripcion: this.nuevoHito.descripcion,
      fecha: this.nuevoHito.fecha || undefined,
      medioUrl: this.nuevoHito.medioUrl,
      medioTipo: this.nuevoHito.medioTipo
    }).subscribe({
      next: hito => {
        this.misHitos.update(lista => [hito, ...lista]);
        this.nuevoHito = { titulo: '', descripcion: '', fecha: '', medioUrl: null, medioTipo: null };
        this.cdr.detectChanges();
      },
      error: err => this.mostrarToast(
        typeof err?.error === 'string' ? err.error : 'No se ha podido guardar la marca.', 'error')
    });
  }

  borrarHito(hitoId: number): void {
    this.perfiles.borrarHito(hitoId).subscribe({
      next: () => {
        this.misHitos.update(lista => lista.filter(h => h.id !== hitoId));
        this.cdr.detectChanges();
      },
      error: () => this.mostrarToast('No se ha podido borrar.', 'error')
    });
  }

  /** A quién se está mirando, o null. */
  perfilAbierto = signal<number | null>(null);

  verPerfil(usuarioId: number): void {
    this.perfilAbierto.set(usuarioId);
  }

  cerrarPerfil(): void {
    this.perfilAbierto.set(null);
  }

  /** Desde el perfil se puede conectar o deshacer: la lista tiene que enterarse. */
  alCambiarRelacionDesdeElPerfil(): void {
    this.cargarMatches();
    this.avisos.refrescar();
  }

  /**
   * Retira una solicitud enviada. Vuelve a dejar a esa persona como candidata.
   */
  retirarSolicitud(usuario: Match): void {
    this.usuarioService.deshacerRelacion(usuario.id).subscribe({
      next: () => {
        this.matches.update(lista =>
          lista.map(m => m.id === usuario.id ? { ...m, solicitudPendiente: false } : m));
        this.mostrarToast(`Solicitud a ${usuario.nombre} retirada.`);
      },
      error: () => this.mostrarToast('No se ha podido retirar la solicitud.', 'error')
    });
  }

  responderSolicitud(solicitudId: number, estado: 'ACEPTADA' | 'RECHAZADA'): void {
    this.usuarioService.responderSolicitud(solicitudId, estado).subscribe({
      next: () => {
        this.mostrarToast(estado === 'ACEPTADA'
          ? 'Nuevo compañero añadido. Ya podéis hablar.'
          : 'Solicitud rechazada.');
        this.cargarSolicitudesPendientes();
        this.cargarMatches();
        this.avisos.refrescar();
      },
      error: () => this.mostrarToast('Hubo un error al procesar la solicitud.', 'error')
    });
  }

  // ================= Sesiones =================

  private cargarSesiones(): void {
    this.sesionesService.mias().subscribe({
      next: lista => {
        this.sesiones.set(lista);
        this.cdr.detectChanges();
      },
      error: err => console.error('Error al cargar las sesiones:', err)
    });
  }

  responderSesion(sesion: Sesion, acepta: boolean): void {
    const peticion = acepta
      ? this.sesionesService.aceptar(sesion.id)
      : this.sesionesService.rechazar(sesion.id);

    peticion.subscribe({
      next: () => {
        this.mostrarToast(acepta
          ? `Hecho. Entrenas con ${sesion.conNombre}.`
          : 'Propuesta rechazada.', acepta ? 'success' : 'error');
        this.cargarSesiones();
      },
      error: err => this.mostrarToast(
        err?.error?.error ?? 'No se ha podido responder.', 'error')
    });
  }

  /**
   * "Sí, entrenamos": el momento en el que un match acaba siendo una fila del
   * historial. Por eso se recarga también el progreso semanal, que es donde se
   * nota.
   */
  confirmarSesion(sesion: Sesion): void {
    this.sesionesService.confirmar(sesion.id).subscribe({
      next: () => {
        this.mostrarToast('Apuntado en tu historial.', 'success');
        this.cargarSesiones();
        this.cargarHistorialEntrenamientos();
      },
      error: err => this.mostrarToast(
        err?.error?.error ?? 'No se ha podido apuntar.', 'error')
    });
  }

  /** "Viernes 3 de julio", que es como se dice una fecha cuando se queda. */
  diaLargo(fecha: string): string {
    const d = new Date(`${fecha}T00:00:00`);
    return d.toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' });
  }

  /** Las horas llegan como "18:00:00" y en un plan sobran los segundos. */
  hhmm(hora: string | null): string {
    return (hora ?? '').slice(0, 5);
  }

  /**
   * Lleva a la conversación con ese compañero.
   *
   * Antes esto abría /chat/:id, una segunda implementación del chat copiada de
   * la de conexiones. Ahora los mensajes viven en un solo sitio y el compañero
   * llega como parámetro para abrirlo directamente.
   */
  irAlChat(usuarioId: number): void {
    this.router.navigate(['/conexiones'], { queryParams: { con: usuarioId } });
  }

  // --- Presentación ---

  /** Convierte los minutos de solape en algo legible: "4h 30min". */
  formatearSolape(minutos: number): string {
    if (!minutos) return '';
    const horas = Math.floor(minutos / 60);
    const resto = minutos % 60;
    if (horas === 0) return `${resto} min`;
    if (resto === 0) return horas === 1 ? '1 hora' : `${horas} horas`;
    return `${horas}h ${resto}min`;
  }

  /** Tramo de compatibilidad, para no repetir umbrales por toda la plantilla. */
  tramo(puntuacion: number): 'alta' | 'media' | 'baja' {
    if (puntuacion >= 70) return 'alta';
    if (puntuacion >= 40) return 'media';
    return 'baja';
  }

  // --- Modales de perfil y entrenamiento ---
  abrirModal(): void { this.isModalOpen = true; this.cdr.detectChanges(); }
  cerrarModal(): void { this.isModalOpen = false; this.cdr.detectChanges(); }
  /**
   * Elegir un color implica querer las iniciales: con foto puesta, el color no
   * se ve por ningún lado y el selector parecería estropeado.
   */
  seleccionarAvatar(color: string): void {
    this.perfilForm.avatar = color;
    this.quitarFoto();
  }
  agregarHorario(): void {
    this.perfilForm.horarios.push({
      diaSemana: 'Lunes', horaInicio: '10:00', horaFin: '12:00', habitual: false
    });
  }
  eliminarHorario(index: number): void { this.perfilForm.horarios.splice(index, 1); }

  /**
   * Tope de franjas marcadas como "Voy siempre".
   *
   * Sin tope todo el mundo las marcaría todas y el campo dejaría de distinguir
   * nada, igual que unos deslizadores que todos ponen al máximo. El backend
   * aplica el mismo límite: esto solo evita que la interfaz prometa algo que
   * luego se recorta al guardar.
   */
  readonly maxHabituales = 3;

  habitualesMarcadas(): number {
    return this.perfilForm.horarios.filter((h: any) => h.habitual).length;
  }

  /** Si esta franja puede pasar a habitual, o ya se ha agotado el cupo. */
  puedeMarcarHabitual(horario: any): boolean {
    return horario.habitual || this.habitualesMarcadas() < this.maxHabituales;
  }

  alternarHabitual(horario: any): void {
    if (!this.puedeMarcarHabitual(horario)) {
      this.mostrarToast(`Puedes marcar ${this.maxHabituales} franjas como fijas.`, 'error');
      return;
    }
    horario.habitual = !horario.habitual;
    this.cdr.detectChanges();
  }

  guardarPerfil(): void {
    if (this.mostrarInputGimnasio) {
      this.perfilForm.nuevoGimnasioNombre = this.nuevoGimnasioNombre;
      this.perfilForm.gimnasioId = null;
    }

    this.usuarioService.actualizarPerfil(this.perfilForm).subscribe({
      next: () => {
        this.mostrarToast('Perfil actualizado.');
        this.cerrarModal();
        // Aqui se pueden borrar todos los horarios, asi que el guardian no puede
        // seguir creyendo lo que sabia antes.
        this.perfilEstado.olvidar();
        // Cambiar horarios altera la compatibilidad con todo el mundo, así que se
        // recargan perfil y matches. Antes se llamaba a ngOnInit() a mano.
        this.cargarMiPerfil();
        this.cargarMatches();
      },
      error: () => this.mostrarToast('Hubo un error al guardar tu perfil.', 'error')
    });
  }

  cargarHistorialEntrenamientos(): void {
    this.usuarioService.getMisEntrenamientos().subscribe({
      next: (data) => {
        this.historialEntrenamientos = data;
        this.calcularProgresoSemanal();
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Error cargando entrenamientos:', err)
    });
  }

  abrirModalEntrenamiento(): void {
    this.nuevoEntrenamiento = {
      fecha: new Date().toISOString().split('T')[0],
      tipo: 'Fuerza (Torso)', duracionMinutos: null, lugarONotas: ''
    };
    this.isEntrenamientoModalOpen = true;
    this.cdr.detectChanges();
  }

  cerrarModalEntrenamiento(): void {
    this.isEntrenamientoModalOpen = false;
    this.cdr.detectChanges();
  }

  guardarEntrenamiento(): void {
    this.usuarioService.registrarEntrenamiento(this.nuevoEntrenamiento).subscribe({
      next: () => {
        this.mostrarToast('Entrenamiento registrado.');
        this.cargarHistorialEntrenamientos();
        this.cerrarModalEntrenamiento();
      },
      error: () => this.mostrarToast('Hubo un error al guardar tu entrenamiento.', 'error')
    });
  }
}
