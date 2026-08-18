import { Component, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { api } from '../../config/api';
import { IdiomaService } from '../../services/idioma.service';

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

  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);

  /**
   * El listón de muestra por tramo, que la plantilla dice en voz alta.
   *
   * <p>Escrito una vez: el número aparece en la frase que explica por qué
   * todavía no se puede concluir nada, y ahí es donde se comprueba.
   */
  protected readonly MINIMO_POR_TRAMO = 20;

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

  /** El nombre de cada tramo, con su umbral, en el idioma de la pantalla. */
  protected nombre(tramo: FilaEmbudo['tramo']): string {
    const claves = {
      ALTA: 'embudo.tramoAlta',
      MEDIA: 'embudo.tramoMedia',
      BAJA: 'embudo.tramoBaja',
    } as const;

    return this.i18n.t(claves[tramo]);
  }
}
