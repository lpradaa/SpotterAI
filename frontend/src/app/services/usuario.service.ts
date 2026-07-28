import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { api } from '../config/api';

/** Un tramo en el que dos usuarios coinciden. */
export interface FranjaComun {
  dia: string;
  inicio: string;
  fin: string;
  /** Los dos declaran ir siempre a esa franja. */
  ambosFijos: boolean;
}

/** Un candidato a compañero de entrenamiento, tal y como lo devuelve el backend. */
export interface Match {
  id: number;
  nombre: string;
  email: string;
  edad: number | null;
  genero: string | null;
  peso: number | null;
  nivel: string | null;
  objetivos: string | null;
  avatar: string | null;
  /** Foto de perfil. Si no la hay, el avatar cae a iniciales sobre color. */
  fotoUrl: string | null;
  biografia: string | null;
  gimnasioId: number | null;
  gimnasioNombre: string | null;
  /** Nombre legible de su rutina ("Torso / Pierna"), o null si no la ha dicho. */
  rutina: string | null;
  /**
   * Si podéis cubriros con la barra cargada.
   *
   * `null` es "no se sabe" —no hay ningún ejercicio en común— y no "no podéis":
   * son cosas distintas y la interfaz tiene que poder decirlas distinto.
   */
  fuerzaCompatible: boolean | null;

  // Compatibilidad: solo llega en los endpoints de match
  compatibilidad: number;
  etiquetaCompatibilidad: string;
  resumenCompatibilidad: string;
  diasEnComun: string[];
  minutosEnComun: number;
  /** Días a los que ambos declaran ir siempre. La señal más fiable que hay. */
  diasFijosEnComun: number;
  /** La puntuación se calculó sin algún factor por falta de datos. */
  compatibilidadIncompleta: boolean;
  /** Tramos concretos en común, para poder dibujar la semana. */
  franjasEnComun: FranjaComun[];

  yaConectado: boolean;
  solicitudPendiente: boolean;
}

/** Explicación redactada de un match concreto. */
export interface ExplicacionMatch {
  titular: string;
  motivo: string;
}

@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private http = inject(HttpClient);

  // jwtInterceptor inyecta la cabecera Authorization en cada petición, así que
  // no hace falta construirla a mano método a método como se hacía antes.
  private readonly usuarios = api('/api/usuarios');
  private readonly solicitudes = api('/api/solicitudes');
  private readonly entrenamientos = api('/api/entrenamientos');
  private readonly gimnasios = api('/api/gimnasios');

  /** Candidatos puntuados y ordenados por compatibilidad. */
  getMatches(): Observable<Match[]> {
    return this.http.get<Match[]>(`${this.usuarios}/matches`);
  }

  /**
   * Explicación redactada de un match concreto.
   * Va aparte de la lista porque cuesta una llamada al modelo: se pide solo
   * cuando el usuario abre una ficha.
   */
  getExplicacionMatch(usuarioId: number): Observable<ExplicacionMatch> {
    return this.http.get<ExplicacionMatch>(`${this.usuarios}/matches/${usuarioId}/explicacion`);
  }

  enviarSolicitudConexion(receptorId: number): Observable<unknown> {
    return this.http.post(`${this.solicitudes}/enviar/${receptorId}`, {});
  }

  obtenerSolicitudesPendientes(): Observable<any[]> {
    return this.http.get<any[]>(`${this.solicitudes}/pendientes`);
  }

  responderSolicitud(solicitudId: number, estado: 'ACEPTADA' | 'RECHAZADA'): Observable<unknown> {
    return this.http.put(`${this.solicitudes}/responder/${solicitudId}?estado=${estado}`, {});
  }

  /**
   * Retira una solicitud enviada o deja de ser compañero de alguien.
   *
   * Va por usuario y no por identificador de solicitud porque quien lo usa sabe
   * con quién quiere deshacer, no el número de la fila.
   */
  deshacerRelacion(otroUsuarioId: number): Observable<void> {
    return this.http.delete<void>(`${this.solicitudes}/con/${otroUsuarioId}`);
  }

  // obtenerMisConexiones se ha ido: la lista de compañeros la sirve ahora
  // MensajesService.conversaciones(), que ademas trae el ultimo mensaje y lo
  // pendiente. Era el unico consumidor de /solicitudes/aceptadas desde el front.

  actualizarPerfil(perfilData: unknown): Observable<any> {
    return this.http.put<any>(`${this.usuarios}/perfil`, perfilData);
  }

  getMiPerfil(): Observable<any> {
    return this.http.get<any>(`${this.usuarios}/perfil`);
  }

  registrarEntrenamiento(data: unknown): Observable<unknown> {
    return this.http.post(this.entrenamientos, data);
  }

  getMisEntrenamientos(): Observable<any[]> {
    return this.http.get<any[]>(this.entrenamientos);
  }

  /** Lo que han hecho últimamente tus compañeros. Solo los tuyos. */
  getActividad(): Observable<any[]> {
    return this.http.get<any[]>(`${this.usuarios}/actividad`);
  }

  getGimnasios(): Observable<any[]> {
    return this.http.get<any[]>(this.gimnasios);
  }
}
