import { Injectable, inject, signal } from '@angular/core';
import { Observable, of, tap, map, catchError } from 'rxjs';
import { UsuarioService } from './usuario.service';

/** Lo que falta para poder emparejarse, tal y como lo dice el servidor. */
export interface PerfilMinimo {
  queFalta: string[];
  estaCompleto: boolean;
}

/**
 * Recuerda si el perfil tiene lo mínimo para poder emparejarse.
 *
 * Existe para que el guardián no pregunte al servidor en cada navegación: la
 * respuesta solo cambia cuando se guarda el perfil, y entonces la invalidamos
 * desde ahí.
 *
 * Antes solo miraba el horario. Ahora mira todo lo que el motor necesita
 * declarado —gimnasio, nivel, objetivo, rutina y edad, además del horario—
 * porque quién decide qué es imprescindible es el motor, y la lista vive en el
 * backend, en PerfilMinimo. Duplicarla aquí sería garantizar que un día digan
 * cosas distintas.
 */
@Injectable({ providedIn: 'root' })
export class PerfilEstadoService {

  private usuarioService = inject(UsuarioService);

  /** null mientras no se sepa. */
  private minimo = signal<PerfilMinimo | null>(null);

  /** Para pintar avisos sin volver a preguntar. */
  readonly perfilMinimo = this.minimo.asReadonly();

  /**
   * Resuelve si se puede emparejar a esta persona, consultando la primera vez.
   *
   * Ante un error de red devuelve true a propósito: dejar pasar a alguien con el
   * perfil completo es menos malo que encerrar a todo el mundo en la pantalla de
   * bienvenida porque el backend parpadeó.
   */
  comprobar(): Observable<boolean> {
    const conocido = this.minimo();
    if (conocido !== null) return of(conocido.estaCompleto);

    return this.usuarioService.getMiPerfil().pipe(
      map(perfil => this.leer(perfil)),
      tap(m => this.minimo.set(m)),
      map(m => m.estaCompleto),
      catchError(() => of(true))
    );
  }

  /** Qué falta exactamente, para poder pedirlo en la bienvenida. */
  queFalta(): Observable<string[]> {
    const conocido = this.minimo();
    if (conocido !== null) return of(conocido.queFalta);

    return this.usuarioService.getMiPerfil().pipe(
      map(perfil => this.leer(perfil)),
      tap(m => this.minimo.set(m)),
      map(m => m.queFalta),
      catchError(() => of([]))
    );
  }

  private leer(perfil: any): PerfilMinimo {
    const queFalta: string[] = perfil?.perfilMinimo?.queFalta ?? [];
    return { queFalta, estaCompleto: queFalta.length === 0 };
  }

  /** Tras guardar el perfil, porque puede haber cambiado cualquier campo. */
  olvidar(): void {
    this.minimo.set(null);
  }
}
