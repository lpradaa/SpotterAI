import { Injectable, NgZone, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';
import { api } from '../config/api';
import { firstValueFrom } from 'rxjs';

/** Estado del canal, para poder avisar en la interfaz cuando se cae. */
export type EstadoCanal = 'desconectado' | 'conectando' | 'conectado';

/**
 * Canal de eventos en tiempo real (SSE).
 *
 * Sustituye a no enterarse de nada hasta recargar. Publica lo que llega sin que
 * lo hayas pedido: mensajes de chat, solicitudes, respuestas a las tuyas y
 * sesiones propuestas o respondidas.
 *
 * Sobre la reconexión: EventSource trae reintento automático, pero aquí no sirve
 * porque reabriría la misma URL con un ticket ya gastado y se llevaría un 401 en
 * bucle. Por eso ante cualquier error se cierra la conexión y se rehace el ciclo
 * entero —ticket nuevo incluido— con espera creciente.
 */
@Injectable({ providedIn: 'root' })
export class EventosService {

  private http = inject(HttpClient);
  private zona = inject(NgZone);

  private readonly apiUrl = api('/api/eventos');

  private static readonly ESPERA_INICIAL_MS = 1_000;
  private static readonly ESPERA_MAXIMA_MS = 30_000;

  private fuente: EventSource | null = null;
  private temporizador: ReturnType<typeof setTimeout> | null = null;
  private intentos = 0;
  /** Distingue "se ha caído" de "hemos cerrado nosotros", que no debe reintentar. */
  private queremosEstarConectados = false;

  readonly estado = signal<EstadoCanal>('desconectado');

  private readonly _mensajes = new Subject<any>();
  private readonly _solicitudes = new Subject<any>();
  private readonly _respuestas = new Subject<any>();
  private readonly _relacionesDeshechas = new Subject<any>();
  private readonly _sesiones = new Subject<any>();
  private readonly _sesionesRespondidas = new Subject<any>();

  /** Mensaje de chat recibido, con su contenido completo. */
  readonly mensajes = this._mensajes.asObservable();
  /** Alguien te ha enviado una solicitud de conexión. */
  readonly solicitudes = this._solicitudes.asObservable();
  /** Han aceptado o rechazado una solicitud tuya. */
  readonly respuestas = this._respuestas.asObservable();
  /** Alguien ha retirado su solicitud o ha dejado de ser compañero tuyo. */
  readonly relacionesDeshechas = this._relacionesDeshechas.asObservable();
  /** Un compañero propone quedar. Viaja la sesión entera. */
  readonly sesiones = this._sesiones.asObservable();
  /** Han aceptado, rechazado o cancelado una sesión tuya. */
  readonly sesionesRespondidas = this._sesionesRespondidas.asObservable();

  async conectar(): Promise<void> {
    // Ya no hay token que mirar: la sesion va en una galleta HttpOnly.
    // Lo que dice si hay alguien dentro es el marcador que deja el login.
    if (this.fuente || !localStorage.getItem('userId')) return;

    this.queremosEstarConectados = true;
    this.estado.set('conectando');

    let ticket: string;
    try {
      const respuesta = await firstValueFrom(
        this.http.post<{ ticket: string }>(`${this.apiUrl}/ticket`, {})
      );
      ticket = respuesta.ticket;
    } catch {
      // Sin ticket no hay canal. Puede ser que el backend esté caído o que el
      // token haya caducado; en ambos casos toca esperar y volver a intentarlo.
      this.programarReintento();
      return;
    }

    if (!this.queremosEstarConectados) return; // Han cerrado sesión mientras tanto

    const fuente = new EventSource(`${this.apiUrl}/suscribir?ticket=${encodeURIComponent(ticket)}`);
    this.fuente = fuente;

    fuente.addEventListener('conectado', () => this.zona.run(() => {
      this.intentos = 0;
      this.estado.set('conectado');
    }));

    fuente.addEventListener('mensaje', e => this.emitir(this._mensajes, e));
    fuente.addEventListener('solicitud', e => this.emitir(this._solicitudes, e));
    fuente.addEventListener('solicitud-respondida', e => this.emitir(this._respuestas, e));
    fuente.addEventListener('relacion-deshecha', e => this.emitir(this._relacionesDeshechas, e));
    fuente.addEventListener('sesion', e => this.emitir(this._sesiones, e));
    fuente.addEventListener('sesion-respondida', e => this.emitir(this._sesionesRespondidas, e));

    fuente.onerror = () => {
      // Se llega aquí tanto por un corte real como por el vencimiento normal de
      // los 30 minutos del servidor. No hay forma de distinguirlos ni falta:
      // en los dos casos la respuesta es rehacer la conexión.
      fuente.close();
      if (this.fuente === fuente) this.fuente = null;
      this.zona.run(() => this.estado.set('conectando'));
      this.programarReintento();
    };
  }

  desconectar(): void {
    this.queremosEstarConectados = false;
    this.intentos = 0;

    if (this.temporizador) {
      clearTimeout(this.temporizador);
      this.temporizador = null;
    }

    this.fuente?.close();
    this.fuente = null;
    this.estado.set('desconectado');
  }

  private emitir(destino: Subject<any>, evento: MessageEvent): void {
    let datos: any;
    try {
      datos = JSON.parse(evento.data);
    } catch {
      return; // Un evento ilegible no debe tumbar el canal
    }
    this.zona.run(() => destino.next(datos));
  }

  /** Espera creciente: 1 s, 2 s, 4 s... con tope, para no machacar un backend caído. */
  private programarReintento(): void {
    if (!this.queremosEstarConectados || this.temporizador) return;

    const espera = Math.min(
      EventosService.ESPERA_INICIAL_MS * 2 ** this.intentos,
      EventosService.ESPERA_MAXIMA_MS
    );
    this.intentos++;

    this.temporizador = setTimeout(() => {
      this.temporizador = null;
      this.conectar();
    }, espera);
  }
}
