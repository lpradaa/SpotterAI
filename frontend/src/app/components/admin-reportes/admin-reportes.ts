import { Component, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { api } from '../../config/api';

/** Un reporte tal y como lo manda GET /api/reportes. */
export interface Reporte {
  id: number;
  reportadorNombre: string;
  reportadorEmail: string;
  reportadoId: number;
  reportadoNombre: string;
  reportadoEmail: string;
  motivo: string;
  detalle: string;
  creadoEn: string;
  /** Si alguien con acceso ya lo dio por visto. */
  revisado: boolean;
}

/**
 * Cerrando el bucle de moderación: capturar un reporte no sirve de nada si
 * verlo requiere entrar a la base de datos a mano. Esta pantalla es esa
 * segunda mitad, deliberadamente mínima —una tabla, sin filtros ni acciones—
 * porque hoy solo hace falta poder mirar, no todavía gestionar.
 *
 * <p>No está en la navegación ni tiene guardia propia en las rutas: quien no
 * está en {@code AdminEmails} recibe un 404 del backend y aquí se enseña como
 * "no tienes acceso", sin distinguir ese caso de "no existe esta ruta".
 */
@Component({
  selector: 'app-admin-reportes',
  standalone: true,
  templateUrl: './admin-reportes.html',
  styleUrl: './admin-reportes.scss',
})
export class AdminReportesComponent {
  private http = inject(HttpClient);

  protected sinAcceso = signal(false);
  protected error = signal(false);

  protected reportes = toSignal(
    this.http.get<Reporte[]>(api('/api/reportes')).pipe(
      catchError((err: unknown) => {
        if (err instanceof HttpErrorResponse && err.status === 404) {
          this.sinAcceso.set(true);
        } else {
          this.error.set(true);
        }
        return of(null);
      }),
    ),
    { initialValue: null },
  );

  /** Marcados por mí en esta sesión, para que la fila cambie sin recargar. */
  private vistosAhora = signal<Set<number>>(new Set());

  protected estaRevisado(r: Reporte): boolean {
    return r.revisado || this.vistosAhora().has(r.id);
  }

  /**
   * Dar por visto.
   *
   * <p>No resuelve nada ni sanciona a nadie: lo que se haga después pasa fuera
   * de la aplicación. Sirve para que una lista que crece no se vuelva ilegible
   * — que es lo que la convertía en teatro, porque quien reporta sigue creyendo
   * que alguien lo lee.
   */
  protected marcarVisto(r: Reporte): void {
    if (this.estaRevisado(r)) return;

    this.http.post(api(`/api/reportes/${r.id}/revisado`), {}).subscribe({
      next: () => this.vistosAhora.update(s => new Set(s).add(r.id)),
      error: () => {},
    });
  }

  protected cuando(fecha: string): string {
    return new Date(fecha).toLocaleString('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
