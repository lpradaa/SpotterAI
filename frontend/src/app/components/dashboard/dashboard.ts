import { Component, signal, computed, OnInit, inject, ChangeDetectorRef, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UsuarioService, Match } from '../../services/usuario.service';
import { EventosService } from '../../services/eventos.service';
import { PerfilEstadoService } from '../../services/perfil-estado.service';
import { AvisosService } from '../../services/avisos.service';
import { Avatar } from '../avatar/avatar';
import { PerfilesService } from '../../services/perfiles.service';
import { SesionesService, Sesion } from '../../services/sesiones.service';
import { ModalAccesible } from '../../directivas/modal-accesible';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, Avatar,
            ModalAccesible, RouterLink],
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

  /**
   * Lo que han hecho últimamente tus compañeros.
   *
   * Es lo que ocupa el sitio que dejó la lista de gente, y responde a otra
   * pregunta: la lista servía para buscar —eso vive ahora en Explorar— y esto
   * para saber qué hace la gente con la que ya entrenas.
   */
  actividad = signal<any[]>([]);

  // --- Progreso semanal ---
  completedDays = signal(0);
  totalDays = signal(4);
  progressPercentage = signal(0);

  // --- Gimnasios ---
  gimnasios: any[] = [];

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

  // --- Avisos ---
  toast: { show: boolean, message: string, type: 'success' | 'error' } = { show: false, message: '', type: 'success' };
  private toastTimeout: any;

  ngOnInit(): void {
    this.cargarHistorialEntrenamientos();
    this.cargarGimnasios();
    this.cargarMiPerfil();
    this.cargarSesiones();
    this.cargarActividad();
    this.escucharEventos();
  }

  /**
   * Lo que llega sin que lo hayas pedido.
   *
   * Las solicitudes se avisan pero ya no se listan aquí: se responden en
   * /solicitudes, que es la bandeja, y estaban en los dos sitios.
   */
  private escucharEventos(): void {
    this.eventos.solicitudes
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(solicitud => {
        this.mostrarToast(`${solicitud.emisorNombre} quiere entrenar contigo.`, 'success');
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
        // Un compañero nuevo estrena actividad que enseñar.
        this.cargarActividad();
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

  private cargarActividad(): void {
    this.usuarioService.getActividad().subscribe({
      next: lista => { this.actividad.set(lista || []); this.cdr.detectChanges(); },
      error: err => console.error('Error al cargar la actividad:', err)
    });
  }

  urlMedio(ruta: string | null): string | null {
    return this.perfiles.urlDeMedio(ruta);
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

  // --- Modal de entrenamiento ---
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
