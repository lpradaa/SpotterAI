import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { api } from '../../config/api';
import { etiquetaDeMotivo } from '../../utils/motivos-de-reporte';
import { agruparEnCasos, Reporte } from '../../utils/casos-de-moderacion';

export type { Reporte, CasoDeModeracion } from '../../utils/casos-de-moderacion';

/**
 * Cerrando el bucle de moderación: capturar un reporte no sirve de nada si
 * verlo requiere entrar a la base de datos a mano. Esta pantalla es esa
 * segunda mitad.
 *
 * <p>Era una tabla plana ordenada por fecha, y una tabla plana no responde la
 * pregunta que se hace quien modera. Esa pregunta no es "qué ha pasado y en qué
 * orden", es "de quién hay un problema". Con siete reportes ya se veía: tres
 * personas distintas habían reportado a la misma, en tres filas separadas por
 * otras dos, y la pantalla no lo decía en ningún sitio — había que leer la
 * columna y contar de memoria.
 *
 * <p>Ahora se agrupa por persona reportada. La distinción que manda es entre
 * "una persona se ha quejado tres veces" y "tres personas se han quejado una
 * vez": la primera puede ser un conflicto entre dos, la segunda es un patrón.
 * Son cosas distintas y ninguna tabla ordenada por fecha las separa.
 *
 * <p>Sigue sin filtros y sin sanciones: lo que se haga después pasa fuera de la
 * aplicación. Esto solo ordena lo que hay para poder mirarlo.
 *
 * <p>No está en la navegación ni tiene guardia propia en las rutas: quien no
 * está en {@code AdminEmails} recibe un 404 del backend y aquí se enseña como
 * "no tienes acceso", sin distinguir ese caso de "no existe esta ruta".
 */
@Component({
  selector: 'app-admin-reportes',
  standalone: true,
  imports: [RouterLink],
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
   * Los reportes agrupados por la persona reportada.
   *
   * <p>La regla vive en utils/casos-de-moderacion, que es donde se prueba: qué
   * cuenta como patrón y qué se mira antes es una decisión de producto, no una
   * cuestión de plantilla.
   */
  protected casos = computed(() =>
    agruparEnCasos(this.reportes() ?? [], r => this.estaRevisado(r)),
  );

  /** Cuántos reportes esperan que alguien los mire, en total. */
  protected sinRevisar = computed(() =>
    this.casos().reduce((total, c) => total + c.sinRevisar, 0),
  );

  /** El motivo como lo lee una persona, no como viaja al backend. */
  protected etiqueta(motivo: string): string {
    return etiquetaDeMotivo(motivo);
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
