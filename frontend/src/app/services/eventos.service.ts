import { Injectable, NgZone, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Subject } from 'rxjs';
import { firstValueFrom } from 'rxjs';

/** Estado del canal, para poder avisar en la interfaz cuando se cae. */
export type EstadoCanal = 'desconectado' | 'conectando' | 'conectado';

/**
 * Canal de eventos en tiempo real (SSE).
 *
 * Sustituye a no enterarse de nada hasta recargar. Publica tres cosas: mensajes
 * de chat, solicitudes que te llegan y respuestas a las que enviaste tú.
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

  private readonly apiUrl = 'http://localhost:8080/api/eventos';

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

  /** Mensaje de chat recibido, con su contenido completo. */
  readonly mensajes = this._mensajes.asObservable();
  /** Alguien te ha enviado una solicitud de conexión. */
  readonly solicitudes = this._solicitudes.asObservable();
  /** Han aceptado o rechazado una solicitud tuya. */
  readonly respuestas = this._respuestas.asObservable();

  async conectar(): Promise<void> {
    if (this.fuente || !localStorage.getItem('token')) return;

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
