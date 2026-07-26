import { Component, OnInit, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { EventosService } from '../../services/eventos.service';
import { AvisosService } from '../../services/avisos.service';
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
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);

  solicitudesPendientes = signal<any[]>([]);

  /** Identificadores en curso, para no permitir dos clics sobre lo mismo. */
  private respondiendo = signal<Set<number>>(new Set());

  aviso = signal<{ texto: string, tipo: 'exito' | 'error' } | null>(null);
  private temporizadorAviso: ReturnType<typeof setTimeout> | null = null;

  ngOnInit(): void {
    this.cargarSolicitudes();

    // Esta es la pantalla donde más raro quedaba no enterarse: puedes estar
    // mirándola cuando llega una y no verla hasta recargar.
    this.eventos.solicitudes
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(solicitud => {
        this.solicitudesPendientes.update(lista => [solicitud, ...lista]);
      });
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
