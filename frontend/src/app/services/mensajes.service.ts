import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { api } from '../config/api';

export interface Mensaje {
  id: number;
  emisorId: number;
  emisorNombre: string;
  receptorId: number;
  contenido: string;
  fechaEnvio: string;
  /**
   * Si el otro ya lo ha abierto.
   *
   * Solo tiene sentido mirarlo en los tuyos: en los suyos siempre es cierto en
   * cuanto los ves, porque verlos es lo que lo pone a true.
   */
  leido: boolean;
}

/** Una fila de la lista de compañeros. */
export interface Conversacion {
  usuarioId: number;
  nombre: string;
  avatar: string | null;
  /** Su foto, si la tiene. Sin esto la lista pintaba iniciales a todo el mundo. */
  fotoUrl: string | null;
  ultimoMensaje: string | null;
  ultimaFecha: string | null;
  mioElUltimo: boolean;
  sinLeer: number;
}

/**
 * Mensajes y conversaciones.
 *
 * Antes esto vivia suelto dentro del componente, con la cabecera Authorization
 * escrita a mano en cada llamada aunque el jwtInterceptor ya la pone. Eso
 * ademas se saltaba el manejo de 401 del interceptor, o sea que una sesion
 * caducada en el chat no echaba a nadie al login.
 */
@Injectable({ providedIn: 'root' })
export class MensajesService {

  private http = inject(HttpClient);
  private readonly base = api('/api/mensajes');

  /** Compañeros con su último mensaje, ya ordenados por actividad. */
  conversaciones(): Observable<Conversacion[]> {
    return this.http.get<Conversacion[]>(`${this.base}/conversaciones`);
  }

  /** Trae el historial y, de paso, marca la conversación como leída. */
  historial(otroId: number): Observable<Mensaje[]> {
    return this.http.get<Mensaje[]>(`${this.base}/historial/${otroId}`);
  }

  enviar(receptorId: number, contenido: string): Observable<Mensaje> {
    // Objeto y no texto suelto: el endpoint le quitaba las comillas al cuerpo
    // crudo para deshacer el entrecomillado de JSON y se llevaba por delante
    // las que hubieras escrito tú.
    return this.http.post<Mensaje>(`${this.base}/enviar/${receptorId}`, { contenido });
  }

  /** Para un mensaje que llega con esa conversación ya abierta. */
  marcarLeida(otroId: number): Observable<void> {
    return this.http.post<void>(`${this.base}/leidos/${otroId}`, {});
  }

  totalSinLeer(): Observable<number> {
    return this.http.get<{ total: number }>(`${this.base}/sin-leer`).pipe(map(r => r.total));
  }
}
