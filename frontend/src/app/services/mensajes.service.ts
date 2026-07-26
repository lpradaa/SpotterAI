import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface Mensaje {
  id: number;
  emisorId: number;
  emisorNombre: string;
  receptorId: number;
  contenido: string;
  fechaEnvio: string;
}

/** Una fila de la lista de compañeros. */
export interface Conversacion {
  usuarioId: number;
  nombre: string;
  avatar: string | null;
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
  private readonly api = 'http://localhost:8080/api/mensajes';

  /** Compañeros con su último mensaje, ya ordenados por actividad. */
  conversaciones(): Observable<Conversacion[]> {
    return this.http.get<Conversacion[]>(`${this.api}/conversaciones`);
  }

  /** Trae el historial y, de paso, marca la conversación como leída. */
  historial(otroId: number): Observable<Mensaje[]> {
    return this.http.get<Mensaje[]>(`${this.api}/historial/${otroId}`);
  }

  enviar(receptorId: number, contenido: string): Observable<Mensaje> {
    // Objeto y no texto suelto: el endpoint le quitaba las comillas al cuerpo
    // crudo para deshacer el entrecomillado de JSON y se llevaba por delante
    // las que hubieras escrito tú.
    return this.http.post<Mensaje>(`${this.api}/enviar/${receptorId}`, { contenido });
  }

  /** Para un mensaje que llega con esa conversación ya abierta. */
  marcarLeida(otroId: number): Observable<void> {
    return this.http.post<void>(`${this.api}/leidos/${otroId}`, {});
  }

  totalSinLeer(): Observable<number> {
    return this.http.get<{ total: number }>(`${this.api}/sin-leer`).pipe(map(r => r.total));
  }
}
