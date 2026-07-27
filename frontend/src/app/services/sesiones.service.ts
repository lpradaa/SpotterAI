import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { api } from '../config/api';

/**
 * Una sesión tal y como la ve quien la mira.
 *
 * Los `puedo…` los decide el servidor. Deducirlos aquí sería tener las mismas
 * reglas escritas dos veces, y de esas dos copias una acabaría mintiendo.
 */
export interface Sesion {
  id: number;
  conId: number;
  conNombre: string;
  conAvatar: string | null;
  conFotoUrl: string | null;
  fecha: string;
  horaInicio: string;
  horaFin: string;
  gimnasioNombre: string | null;
  estado: 'PROPUESTA' | 'ACEPTADA' | 'RECHAZADA' | 'CANCELADA';
  nota: string | null;
  mia: boolean;
  puedoResponder: boolean;
  puedoCancelar: boolean;
  puedoConfirmar: boolean;
  yaConfirmada: boolean;
}

/** Lo que el formulario trae ya puesto, sacado del solape real. */
export interface SugerenciaSesion {
  hayFranjas: boolean;
  fecha: string | null;
  horaInicio: string | null;
  horaFin: string | null;
  ambosFijos: boolean;
  gimnasioNombre: string | null;
}

@Injectable({ providedIn: 'root' })
export class SesionesService {

  private http = inject(HttpClient);
  private readonly url = api('/api/sesiones');

  /** Por responder, por delante y por apuntar. */
  mias(): Observable<Sesion[]> {
    return this.http.get<Sesion[]>(this.url);
  }

  /**
   * La sesión viva con una persona.
   *
   * El servidor responde 204 cuando no hay ninguna, que no es un error: es lo
   * normal. Angular entrega el cuerpo vacío como null, así que se normaliza aquí
   * para que quien llame no tenga que distinguir entre null y undefined.
   */
  conMigo(otroId: number): Observable<Sesion | null> {
    return this.http.get<Sesion>(`${this.url}/con/${otroId}`)
      .pipe(map(s => s ?? null), catchError(() => of(null)));
  }

  sugerencia(otroId: number): Observable<SugerenciaSesion> {
    return this.http.get<SugerenciaSesion>(`${this.url}/sugerencia/${otroId}`);
  }

  proponer(otroId: number, datos: {
    fecha: string; horaInicio: string; horaFin: string; nota?: string | null;
  }): Observable<Sesion> {
    return this.http.post<Sesion>(`${this.url}/proponer/${otroId}`, datos);
  }

  aceptar(id: number): Observable<Sesion> {
    return this.http.post<Sesion>(`${this.url}/${id}/aceptar`, {});
  }

  rechazar(id: number): Observable<Sesion> {
    return this.http.post<Sesion>(`${this.url}/${id}/rechazar`, {});
  }

  cancelar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }

  /** "Sí, entrenamos": lo apunta en tu historial, solo en el tuyo. */
  confirmar(id: number): Observable<Sesion> {
    return this.http.post<Sesion>(`${this.url}/${id}/confirmar`, {});
  }
}
