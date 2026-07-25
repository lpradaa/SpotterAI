import { Injectable, inject, signal } from '@angular/core';
import { Observable, of, tap, map, catchError } from 'rxjs';
import { UsuarioService } from './usuario.service';

/**
 * Recuerda si el usuario ya ha indicado su horario.
 *
 * Existe para que el guardián no pregunte al servidor en cada navegación: la
 * respuesta solo cambia cuando se guarda el perfil, y entonces la invalidamos
 * desde ahí.
 */
@Injectable({ providedIn: 'root' })
export class PerfilEstadoService {

  private usuarioService = inject(UsuarioService);

  /** null mientras no se sepa; luego true o false. */
  private tieneHorario = signal<boolean | null>(null);

  /** Para pintar avisos sin volver a preguntar. */
  readonly horarioIndicado = this.tieneHorario.asReadonly();

  /**
   * Resuelve si hay horario, consultando solo la primera vez.
   *
   * Ante un error de red devuelve true a propósito: dejar pasar a alguien que sí
   * tenía horario es menos malo que encerrar a todo el mundo en la pantalla de
   * bienvenida porque el backend parpadeó.
   */
  comprobar(): Observable<boolean> {
    const conocido = this.tieneHorario();
    if (conocido !== null) return of(conocido);

    return this.usuarioService.getMiPerfil().pipe(
      map(perfil => (perfil?.horarios?.length ?? 0) > 0),
      tap(tiene => this.tieneHorario.set(tiene)),
      catchError(() => of(true))
    );
  }

  /** Tras guardar el perfil, porque los horarios pueden haber cambiado. */
  olvidar(): void {
    this.tieneHorario.set(null);
  }

  marcarIndicado(): void {
    this.tieneHorario.set(true);
  }
}
