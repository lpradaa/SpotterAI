import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { api } from '../../config/api';

/** Una fila del embudo, tal y como la manda el backend. */
export interface FilaEmbudo {
  tramo: 'ALTA' | 'MEDIA' | 'BAJA';
  enviadas: number;
  aceptadas: number;
  entrenadas: number;
  hayBastante: boolean;
  /** null cuando la muestra no da para decirlo. */
  tasaAceptacion: number | null;
  tasaEntrenamiento: number | null;
}

export interface Embudo {
  filas: FilaEmbudo[];
  sinPuntuacion: number;
  sePuedeComparar: boolean;
}

/**
 * Si el motor acierta.
 *
 * <p>Toda la calculadora está construida sobre una apuesta —que dos personas
 * con un 90 % acaban entrenando juntas más que dos con un 50 %— que hasta ahora
 * no se podía comprobar. Cada peso sale de razonar sobre cómo funciona un
 * gimnasio, no de haber mirado qué pasa.
 *
 * <p>Los umbrales y las tasas los decide el backend y aquí solo se pintan. Es a
 * propósito: si esta pantalla calculara el porcentaje por su cuenta, habría dos
 * sitios decidiendo cuándo hay muestra suficiente, y este proyecto ya se ha
 * comido tres veces el mismo número saliendo distinto por dos caminos.
 */
@Component({
  selector: 'app-embudo',
  standalone: true,
  templateUrl: './embudo.html',
  styleUrl: './embudo.scss',
})
export class EmbudoComponent {
  private http = inject(HttpClient);

  protected error = signal(false);

  protected embudo = toSignal(
    this.http.get<Embudo>(api('/api/metricas/embudo')).pipe(
      catchError(() => {
        this.error.set(true);
        return of(null);
      }),
    ),
    { initialValue: null },
  );

  protected totalMedibles = computed(() =>
    (this.embudo()?.filas ?? []).reduce((suma, f) => suma + f.enviadas, 0),
  );

  protected nombre(tramo: FilaEmbudo['tramo']): string {
    return { ALTA: 'Alta (70 % o más)', MEDIA: 'Media (40–69 %)', BAJA: 'Baja (menos de 40 %)' }[tramo];
  }
}
