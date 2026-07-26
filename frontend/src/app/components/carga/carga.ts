import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Cinco y no siete.
 *
 * Con siete y redondeo hacia arriba, un 98 % y un 88 % daban los siete discos
 * puestos: el indicador no distinguia nada justo en el tramo donde esta casi
 * todo el mundo. Cinco discos anchos y separados se leen como piezas; siete
 * finos y pegados se leian como una linea continua.
 */
const DISCOS = 5;

/**
 * Compatibilidad dibujada como una barra cargada de discos.
 *
 * Sustituye a la barra de progreso lisa. Es la misma información, pero una
 * barra de relleno es el widget que usa cualquier formulario para decir «vas
 * por la mitad»; unos discos puestos en una barra son el vocabulario del sitio
 * para el que se hace esto.
 *
 * Va marcado como decorativo a propósito: el porcentaje se enseña siempre al
 * lado, y un indicador que hay que interpretar no puede ser el único portador
 * del dato para quien usa un lector de pantalla.
 */
@Component({
  selector: 'app-carga',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="carga" aria-hidden="true">
      @for (i of discos(); track i) {
        <span class="carga__disco" [class.carga__disco--puesto]="i <= puestos()"></span>
      }
    </span>
  `,
  styleUrl: './carga.scss'
})
export class Carga {
  /** Puntuación de 0 a 100. */
  puntuacion = input<number>(0);

  readonly discos = computed(() => Array.from({ length: DISCOS }, (_, i) => i + 1));

  /**
   * Cuántos discos van puestos.
   *
   * Redondeo normal, con un mínimo de uno: una barra completamente vacía junto
   * a un 12 % se lee como que falta el dato, no como que hay poco.
   */
  puestos = computed(() => {
    const p = Math.max(0, Math.min(100, this.puntuacion()));
    return p === 0 ? 0 : Math.max(1, Math.round((p / 100) * DISCOS));
  });
}
