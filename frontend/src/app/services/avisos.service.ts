import { Injectable, computed, inject, signal } from '@angular/core';
import { EventosService } from './eventos.service';
import { MensajesService } from './mensajes.service';
import { UsuarioService } from './usuario.service';
import { SesionesService } from './sesiones.service';

/**
 * Lo que está esperando respuesta: solicitudes sin contestar, mensajes sin leer
 * y sesiones propuestas.
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
  private sesiones = inject(SesionesService);

  private solicitudes = signal(0);
  private sinLeer = signal(0);

  /**
   * Propuestas de sesión sin contestar.
   *
   * Faltaba, y era una incoherencia: una sesión esperando tu respuesta es la
   * misma clase de cosa que una solicitud o un mensaje sin leer. Al salir del
   * tablero dejabas de saber que la tenías.
   */
  private sesionesPorResponder = signal(0);

  readonly solicitudesPendientes = this.solicitudes.asReadonly();
  readonly mensajesSinLeer = this.sinLeer.asReadonly();
  readonly sesionesPendientes = this.sesionesPorResponder.asReadonly();
  readonly total = computed(() =>
    this.solicitudes() + this.sinLeer() + this.sesionesPorResponder());

  private escuchando = false;

  /** Carga inicial y enganche al canal. Idempotente: se puede llamar de más. */
  iniciar(): void {
    // La sesion va en una galleta HttpOnly: lo que dice si hay alguien
    // dentro es el marcador que deja el login, no un token.
    if (!localStorage.getItem('userId')) return;

    this.refrescar();
    if (this.escuchando) return;
    this.escuchando = true;

    this.eventos.solicitudes.subscribe(() => this.solicitudes.update(n => n + 1));

    // Una respuesta a una solicitud tuya no cambia tus pendientes: los que
    // cuentan son los que tienes que contestar tú.
    this.eventos.mensajes.subscribe(() => this.sinLeer.update(n => n + 1));

    // Una propuesta que llega suma; una respondida o cancelada puede restar,
    // así que ahí se vuelve a preguntar en vez de adivinar el signo.
    this.eventos.sesiones.subscribe(() => this.sesionesPorResponder.update(n => n + 1));
    this.eventos.sesionesRespondidas.subscribe(() => this.refrescar());
  }

  /** Tras responder solicitudes o abrir conversaciones, para volver a la verdad. */
  refrescar(): void {
    // La sesion va en una galleta HttpOnly: lo que dice si hay alguien
    // dentro es el marcador que deja el login, no un token.
    if (!localStorage.getItem('userId')) return;

    this.usuarios.obtenerSolicitudesPendientes().subscribe({
      next: lista => this.solicitudes.set(lista?.length ?? 0),
      error: () => {}
    });

    this.mensajes.totalSinLeer().subscribe({
      next: total => this.sinLeer.set(total),
      error: () => {}
    });

    this.sesiones.pendientes().subscribe({
      next: total => this.sesionesPorResponder.set(total),
      error: () => {}
    });
  }

  limpiar(): void {
    this.solicitudes.set(0);
    this.sinLeer.set(0);
    this.sesionesPorResponder.set(0);
  }
}
