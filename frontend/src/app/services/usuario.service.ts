import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  biografia: string | null;
  gimnasioId: number | null;
  gimnasioNombre: string | null;

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
  private readonly usuarios = 'http://localhost:8080/api/usuarios';
  private readonly solicitudes = 'http://localhost:8080/api/solicitudes';
  private readonly entrenamientos = 'http://localhost:8080/api/entrenamientos';
  private readonly gimnasios = 'http://localhost:8080/api/gimnasios';

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

  obtenerMisConexiones(): Observable<any[]> {
    return this.http.get<any[]>(`${this.solicitudes}/aceptadas`);
  }

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

  getExplorarUsuarios(): Observable<any[]> {
    return this.http.get<any[]>(`${this.usuarios}/explorar`);
  }

  getGimnasios(): Observable<any[]> {
    return this.http.get<any[]>(this.gimnasios);
  }
}
