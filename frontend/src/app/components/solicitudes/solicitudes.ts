import { Component, OnInit, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { IdiomaService } from '../../services/idioma.service';
import { UsuarioService } from '../../services/usuario.service';
import { EventosService } from '../../services/eventos.service';
import { AvisosService } from '../../services/avisos.service';
import { SesionesService, Sesion } from '../../services/sesiones.service';
import { Avatar } from '../avatar/avatar';
import { PerfilesService } from '../../services/perfiles.service';
import { tramoDe } from '../../utils/compatibilidad';

@Component({
  selector: 'app-solicitudes',
  standalone: true,
  imports: [CommonModule, Avatar, RouterLink],
  templateUrl: './solicitudes.html',
  styleUrl: './solicitudes.scss'
})
export class SolicitudesComponent implements OnInit {
  private usuarioService = inject(UsuarioService);
  private eventos = inject(EventosService);
  private avisos = inject(AvisosService);
  private sesiones = inject(SesionesService);
  private perfiles = inject(PerfilesService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);

  /** Tramo de compatibilidad, para colorear el número con la escala de siempre. */
  tramo = tramoDe;

  /** La foto de quien manda la solicitud, resuelta a URL servible. */
  foto(ruta: string | null | undefined): string | null {
    return this.perfiles.urlDeMedio(ruta ?? null);
  }

  solicitudesPendientes = signal<any[]>([]);

  /**
   * Propuestas de sesión sin contestar.
   *
   * Viven aquí además de en el tablero porque esta pantalla es exactamente eso:
   * lo que espera tu respuesta. Y porque la campana de la cabecera lleva aquí:
   * contar en ella algo que luego no estuviera sería mandar a alguien a una
   * pantalla donde no hay nada que hacer.
   */
  sesionesPropuestas = signal<Sesion[]>([]);

  /** Todo lo que espera respuesta, que es lo que cuenta la campana. */
  pendientes = computed(() =>
    this.solicitudesPendientes().length + this.sesionesPropuestas().length);

  /** Identificadores en curso, para no permitir dos clics sobre lo mismo. */
  private respondiendo = signal<Set<number>>(new Set());

  aviso = signal<{ texto: string, tipo: 'exito' | 'error' } | null>(null);
  private temporizadorAviso: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.cargarSolicitudes();
    this.cargarSesiones();

    this.eventos.sesiones
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.cargarSesiones());

    // Esta es la pantalla donde más raro quedaba no enterarse: puedes estar
    // mirándola cuando llega una y no verla hasta recargar.
    this.eventos.solicitudes
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(solicitud => {
        this.solicitudesPendientes.update(lista => [solicitud, ...lista]);
      });
  }

  cargarSesiones(): void {
    this.sesiones.mias().subscribe({
      next: lista => this.sesionesPropuestas.set(lista.filter(s => s.puedoResponder)),
      error: () => {}
    });
  }

  /** Aceptar o rechazar una propuesta. La lista y la campana se rehacen. */
  responderSesion(sesion: Sesion, acepta: boolean): void {
    const peticion = acepta
      ? this.sesiones.aceptar(sesion.id)
      : this.sesiones.rechazar(sesion.id);

    peticion.subscribe({
      next: () => {
        this.mostrarAviso(acepta
          ? this.i18n.t('sesion.hecho', { nombre: sesion.conNombre })
          : this.i18n.t('sesion.rechazada'), acepta ? 'exito' : 'error');
        this.cargarSesiones();
        this.avisos.refrescar();
      },
      error: err => this.mostrarAviso(
        err?.error?.error ?? this.i18n.t('sesion.errorResponder'), 'error')
    });
  }

  /** "Viernes 3 de julio", que es como se dice una fecha cuando se queda. */
  diaLargo(fecha: string): string {
    const d = new Date(`${fecha}T00:00:00`);
    return this.i18n.fecha(d, { weekday: 'long', day: 'numeric', month: 'long' });
  }

  hhmm(hora: string | null): string {
    return (hora ?? '').slice(0, 5);
  }

  cargarSolicitudes(): void {
    this.usuarioService.obtenerSolicitudesPendientes().subscribe({
      next: data => this.solicitudesPendientes.set(data || []),
      error: err => console.error('Error al cargar solicitudes:', err)
    });
  }

  enCurso(solicitudId: number): boolean {
    return this.respondiendo().has(solicitudId);
  }

  responder(solicitudId: number, estado: 'ACEPTADA' | 'RECHAZADA'): void {
    if (this.enCurso(solicitudId)) return;
    this.respondiendo.update(s => new Set(s).add(solicitudId));

    this.usuarioService.responderSolicitud(solicitudId, estado).subscribe({
      next: () => {
        this.solicitudesPendientes.update(lista => lista.filter(s => s.id !== solicitudId));
        this.respondiendo.update(s => {
          const copia = new Set(s);
          copia.delete(solicitudId);
          return copia;
        });
        this.avisos.refrescar();

        // Antes esto era un alert() del navegador, que bloquea la página y no
        // se parece a nada del resto de la interfaz.
        this.mostrarAviso(
          estado === 'ACEPTADA'
            ? this.i18n.t('solicitudes.aceptada')
            : this.i18n.t('solicitudes.rechazada'),
          'exito');
      },
      error: err => {
        console.error('Error al responder:', err);
        this.respondiendo.update(s => {
          const copia = new Set(s);
          copia.delete(solicitudId);
          return copia;
        });
        this.mostrarAviso(this.i18n.t('solicitudes.error'), 'error');
      }
    });
  }

  irAConversacion(): void {
    this.router.navigate(['/conexiones']);
  }

  private mostrarAviso(texto: string, tipo: 'exito' | 'error'): void {
    if (this.temporizadorAviso) clearTimeout(this.temporizadorAviso);
    this.aviso.set({ texto, tipo });
    this.temporizadorAviso = setTimeout(() => this.aviso.set(null), 4000);
  }
}
