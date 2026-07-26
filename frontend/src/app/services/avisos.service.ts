import { Injectable, computed, inject, signal } from '@angular/core';
import { EventosService } from './eventos.service';
import { MensajesService } from './mensajes.service';
import { UsuarioService } from './usuario.service';

/**
 * Lo que está esperando respuesta: solicitudes sin contestar y mensajes sin leer.
 *
 * Vive aquí y no en cada pantalla porque el aviso tiene que verse desde
 * cualquiera. El canal SSE ya empujaba las dos cosas, pero solo se enteraba
 * quien estuviera mirando justo esa vista: si te llegaba una solicitud estando
 * en el chat, no existía hasta que navegabas al tablero.
 */
@Injectable({ providedIn: 'root' })
export class AvisosService {

  private eventos = inject(EventosService);
  private mensajes = inject(MensajesService);
  private usuarios = inject(UsuarioService);

  private solicitudes = signal(0);
  private sinLeer = signal(0);

  readonly solicitudesPendientes = this.solicitudes.asReadonly();
  readonly mensajesSinLeer = this.sinLeer.asReadonly();
  readonly total = computed(() => this.solicitudes() + this.sinLeer());

  private escuchando = false;

  /** Carga inicial y enganche al canal. Idempotente: se puede llamar de más. */
  iniciar(): void {
    if (!localStorage.getItem('token')) return;

    this.refrescar();
    if (this.escuchando) return;
    this.escuchando = true;

    this.eventos.solicitudes.subscribe(() => this.solicitudes.update(n => n + 1));

    // Una respuesta a una solicitud tuya no cambia tus pendientes: los que
    // cuentan son los que tienes que contestar tú.
    this.eventos.mensajes.subscribe(() => this.sinLeer.update(n => n + 1));
  }

  /** Tras responder solicitudes o abrir conversaciones, para volver a la verdad. */
  refrescar(): void {
    if (!localStorage.getItem('token')) return;

    this.usuarios.obtenerSolicitudesPendientes().subscribe({
      next: lista => this.solicitudes.set(lista?.length ?? 0),
      error: () => {}
    });

    this.mensajes.totalSinLeer().subscribe({
      next: total => this.sinLeer.set(total),
      error: () => {}
    });
  }

  limpiar(): void {
    this.solicitudes.set(0);
    this.sinLeer.set(0);
  }
}
