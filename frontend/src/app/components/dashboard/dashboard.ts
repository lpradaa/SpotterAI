import { Component, signal, computed, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UsuarioService, Match } from '../../services/usuario.service';
import { EventosService } from '../../services/eventos.service';
import { PerfilEstadoService } from '../../services/perfil-estado.service';
import { AvisosService } from '../../services/avisos.service';
import { RejillaSemana } from '../rejilla-semana/rejilla-semana';
import { Avatar } from '../avatar/avatar';
import { PerfilPublicoComponent } from '../perfil-publico/perfil-publico';
import { PerfilesService } from '../../services/perfiles.service';
import { ModalPerfilComponent, AvisoPerfil } from '../modal-perfil/modal-perfil';
import { FichaSugerenciaComponent } from '../ficha-sugerencia/ficha-sugerencia';
import { SesionesService, Sesion } from '../../services/sesiones.service';
import { Carga } from '../carga/carga';
import { ModalAccesible } from '../../directivas/modal-accesible';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RejillaSemana, Avatar, PerfilPublicoComponent,
            Carga, ModalPerfilComponent, FichaSugerenciaComponent, ModalAccesible],
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

  // --- Modal de perfil ---
  isModalOpen = false;

  /**
   * El perfil tal y como lo devuelve el servidor.
   *
   * El tablero solo lo lee —para la barra de arriba y para pintar la semana en
   * las tarjetas—; quien lo edita es <app-modal-perfil>, que se lo lleva entero
   * y avisa cuando ha guardado.
   */
  perfilCargado = signal<any>(null);

  /** Mi semana, para dibujar el solape en cada tarjeta. */
  misFranjas = computed<any[]>(() => this.perfilCargado()?.horarios ?? []);

  urlFoto = computed(() => this.perfiles.urlDeMedio(this.perfilCargado()?.fotoUrl ?? null));

  /**
   * Lo que le falta al perfil, en puntos de compatibilidad perdidos.  /**
   * Lo que le falta al perfil, en puntos de compatibilidad perdidos.
   *
   * Los números los da el backend, que es donde viven los pesos del cálculo.
   * Copiarlos aquí sería garantizar que algún día digan cosas distintas.
   */
  rendimiento = signal<{ puntosEnJuego: number, huecos: any[] } | null>(null);

  puntosEnJuego = computed(() => this.rendimiento()?.puntosEnJuego ?? 0);

  /** Solo el más caro: una lista de cinco cosas no la lee nadie. */
  huecoPrincipal = computed(() => this.rendimiento()?.huecos?.[0] ?? null);

  // --- Modal de entrenamiento ---
  isEntrenamientoModalOpen = false;
  historialEntrenamientos: any[] = [];
  nuevoEntrenamiento: any = { fecha: '', tipo: '', duracionMinutos: null, lugarONotas: '' };

  // --- Modal de sugerencias ---
  isSugerenciasOpen = signal(false);

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

        this.perfilCargado.set(data);
        this.rendimiento.set(data.rendimiento ?? null);
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
    this.isSugerenciasOpen.set(true);
  }

  cerrarSugerencias(): void {
    this.isSugerenciasOpen.set(false);
  }

  /** Conectar desde la ficha la cierra: ya no hay nada que decidir ahí. */
  conectarDesdeLaFicha(usuarioId: number): void {
    this.conectarConUsuario(usuarioId);
    this.cerrarSugerencias();
  }

  // --- Progreso semanal ---
  calcularProgresoSemanal(): void {
    // La meta viene del perfil, que viene de la base. Antes se leia de
    // localStorage con la clave meta_semanal_<nombre>, asi que cambiabas de
    // navegador y desaparecia, y dos usuarios homonimos compartian valor.
    const meta = this.perfilCargado()?.metaSemanal || 4;
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

  urlMedio(ruta: string | null): string | null {
    return this.perfiles.urlDeMedio(ruta);
  }

  /**
   * El modal ha guardado.
   *
   * Se recargan perfil y matches: cambiar horarios altera la compatibilidad con
   * todo el mundo. Y el guardián no puede seguir creyendo lo que sabía, porque
   * ahí dentro se pueden borrar todas las franjas.
   */
  alGuardarElPerfil(): void {
    this.cerrarModal();
    this.perfilEstado.olvidar();
    this.cargarMiPerfil();
    this.cargarMatches();
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
