import { Component, OnInit, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { EventosService } from '../../services/eventos.service';
import { AvisosService } from '../../services/avisos.service';
import { SesionesService, Sesion } from '../../services/sesiones.service';
import { Avatar } from '../avatar/avatar';

@Component({
  selector: 'app-solicitudes',
  standalone: true,
  imports: [CommonModule, Avatar],
  templateUrl: './solicitudes.html',
  styleUrl: './solicitudes.scss'
})
export class SolicitudesComponent implements OnInit {
  private usuarioService = inject(UsuarioService);
  private eventos = inject(EventosService);
  private avisos = inject(AvisosService);
  private sesiones = inject(SesionesService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

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
          ? `Hecho. Entrenas con ${sesion.conNombre}.`
          : 'Propuesta rechazada.', acepta ? 'exito' : 'error');
        this.cargarSesiones();
        this.avisos.refrescar();
      },
      error: err => this.mostrarAviso(
        err?.error?.error ?? 'No se ha podido responder.', 'error')
    });
  }

  /** "Viernes 3 de julio", que es como se dice una fecha cuando se queda. */
  diaLargo(fecha: string): string {
    const d = new Date(`${fecha}T00:00:00`);
    return d.toLocaleDateString('es-ES', { weekday: 'long', day: 'numeric', month: 'long' });
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
            ? 'Aceptada. Ya podéis hablar desde Compañeros.'
            : 'Solicitud rechazada.',
          'exito');
      },
      error: err => {
        console.error('Error al responder:', err);
        this.respondiendo.update(s => {
          const copia = new Set(s);
          copia.delete(solicitudId);
          return copia;
        });
        this.mostrarAviso('No se ha podido procesar la respuesta.', 'error');
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
