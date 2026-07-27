import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE } from '../config/api';

export interface Hito {
  id: number;
  titulo: string;
  descripcion: string | null;
  fecha: string;
  medioUrl: string | null;
  medioTipo: 'imagen' | 'video' | null;
}

/** Una marca principal: lo que el motor puede cruzar entre dos personas. */
export interface Levantamiento {
  /** Clave del enum del backend, para marcar el desplegable. */
  ejercicio: string;
  nombre: string;
  peso: number | null;
  repeticiones: number | null;
}

export interface PerfilPublico {
  id: number;
  nombre: string;
  avatar: string | null;
  fotoUrl: string | null;
  biografia: string | null;
  nivel: string | null;
  objetivos: string | null;
  /** Ya legible ("Torso / Pierna"), no la clave del enum. */
  rutina: string | null;
  edad: number | null;
  gimnasioNombre: string | null;
  compatibilidad: number;
  etiquetaCompatibilidad: string;
  resumenCompatibilidad: string;
  franjasEnComun: any[];
  hitos: Hito[];
  levantamientos: Levantamiento[];
  entrenosUltimaSemana: number;
  yaConectado: boolean;
  solicitudPendiente: boolean;
}

/**
 * Fichas de otras personas y hitos propios.
 *
 * El servidor devuelve rutas relativas para los medios (/api/medios/…) porque
 * no sabe desde dónde se le llama; aquí se completan con el origen.
 */
@Injectable({ providedIn: 'root' })
export class PerfilesService {

  private http = inject(HttpClient);
  private readonly origen = API_BASE;

  verPerfil(usuarioId: number): Observable<PerfilPublico> {
    return this.http.get<PerfilPublico>(`${this.origen}/api/usuarios/${usuarioId}/perfil`);
  }

  misHitos(): Observable<Hito[]> {
    return this.http.get<Hito[]>(`${this.origen}/api/hitos`);
  }

  crearHito(datos: {
    titulo: string; descripcion?: string; fecha?: string;
    medioUrl?: string | null; medioTipo?: string | null;
  }): Observable<Hito> {
    return this.http.post<Hito>(`${this.origen}/api/hitos`, datos);
  }

  borrarHito(hitoId: number): Observable<void> {
    return this.http.delete<void>(`${this.origen}/api/hitos/${hitoId}`);
  }

  /** Sube una foto o un vídeo y devuelve su ruta. */
  subirMedio(archivo: File): Observable<{ url: string, tipo: string }> {
    const cuerpo = new FormData();
    cuerpo.append('archivo', archivo);
    return this.http.post<{ url: string, tipo: string }>(`${this.origen}/api/medios`, cuerpo);
  }

  /** Ruta absoluta de un medio, o null. Para usar en src de img y video. */
  urlDeMedio(ruta: string | null | undefined): string | null {
    return ruta ? `${this.origen}${ruta}` : null;
  }
}
