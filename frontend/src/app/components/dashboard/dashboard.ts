import { Component, signal, computed, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UsuarioService, Match, ExplicacionMatch } from '../../services/usuario.service';
import { EventosService } from '../../services/eventos.service';
import { PerfilEstadoService } from '../../services/perfil-estado.service';
import { RejillaSemana } from '../rejilla-semana/rejilla-semana';
import { Avatar, COLORES_AVATAR } from '../avatar/avatar';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RejillaSemana, Avatar],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss'
})
export class DashboardComponent implements OnInit {
  private usuarioService = inject(UsuarioService);
  private router = inject(Router);
  private cdr = inject(ChangeDetectorRef);
  private eventos = inject(EventosService);
  private perfilEstado = inject(PerfilEstadoService);
  private destroyRef = inject(DestroyRef);

  userName = signal(localStorage.getItem('usuario_nombre') || 'Usuario');

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
    avatar: '', edad: null, genero: '', peso: null, nivel: 'Intermedio',
    objetivos: '', gimnasioId: null, nuevoGimnasioNombre: '', horarios: [], metaSemanal: 4
  };

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

        const metaGuardada = localStorage.getItem('meta_semanal_' + this.userName());
        const meta = metaGuardada ? parseInt(metaGuardada, 10) : 4;

        this.perfilForm = {
          avatar: data.avatar || '', edad: data.edad, genero: data.genero, peso: data.peso,
          nivel: data.nivel || 'Intermedio', objetivos: data.objetivos, gimnasioId: data.gimnasioId,
          horarios: data.horarios || [], metaSemanal: meta
        };
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
    const metaGuardada = localStorage.getItem('meta_semanal_' + this.userName());
    const meta = metaGuardada ? parseInt(metaGuardada, 10) : (this.perfilForm.metaSemanal || 4);
    this.perfilForm.metaSemanal = meta;
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
    localStorage.setItem('meta_semanal_' + this.userName(), nuevoValor.toString());
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

  responderSolicitud(solicitudId: number, estado: 'ACEPTADA' | 'RECHAZADA'): void {
    this.usuarioService.responderSolicitud(solicitudId, estado).subscribe({
      next: () => {
        this.mostrarToast(estado === 'ACEPTADA'
          ? 'Nuevo compañero añadido. Ya podéis hablar.'
          : 'Solicitud rechazada.');
        this.cargarSolicitudesPendientes();
        this.cargarMatches();
      },
      error: () => this.mostrarToast('Hubo un error al procesar la solicitud.', 'error')
    });
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
  seleccionarAvatar(color: string): void { this.perfilForm.avatar = color; }
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
    localStorage.setItem('meta_semanal_' + this.userName(), this.perfilForm.metaSemanal.toString());

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
