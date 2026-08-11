import { Component, computed, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FactorDelDesglose } from '../../services/usuario.service';
import { tramoDe } from '../../utils/compatibilidad';

/**
 * De dónde sale el número.
 *
 * <p>La apuesta de todo el proyecto es que un porcentaje sin explicación no vale
 * nada, y hasta ahora se servía exactamente eso: un porcentaje y una frase. El
 * desglose se calculaba entero —los ocho factores, cada uno con sus puntos, su
 * peso y su explicación— y se aplastaba a una cadena antes de salir del backend.
 *
 * <p>Enseñarlo hace visibles las dos decisiones más caras del motor, de las que
 * no había ni rastro en pantalla: que un factor sin datos <strong>no</strong>
 * puntúa como un cero, y que su peso se reparte entre los demás en vez de
 * perderse. Un usuario que ve "Lo que movéis — sin datos" entiende por qué su
 * número es el que es, y qué puede hacer al respecto.
 */
@Component({
  selector: 'app-desglose',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './desglose.html',
  styleUrl: './desglose.scss',
})
export class Desglose {
  factores = input.required<FactorDelDesglose[]>();

  /** Los que sí puntuaron, de mayor a menor peso: primero lo que más decide. */
  evaluados = computed(() =>
    this.factores().filter(f => f.aplicable).sort((a, b) => b.puntosMax - a.puntosMax));

  /** Los que no se han podido evaluar. Van al final, y en gris. */
  sinDatos = computed(() => this.factores().filter(f => !f.aplicable));

  /**
   * Cuánto del total se está repartiendo entre los demás por falta de datos.
   *
   * <p>Es la cifra que explica por qué los pesos no son los de la tabla del
   * README: si faltan los levantamientos, sus puntos no desaparecen, se
   * reparten. Sin decirlo, un "Nivel 13/13" donde el README dice 10 parece un
   * error.
   */
  hayReparto = computed(() => this.sinDatos().length > 0);

  /** Proporción obtenida, para el ancho de la barra. */
  proporcion(f: FactorDelDesglose): number {
    return f.puntosMax === 0 ? 0 : Math.round((f.puntos / f.puntosMax) * 100);
  }

  /**
   * El tramo de cada barra, con la misma escala que el número grande.
   *
   * <p>Sobre la proporción del factor, no sobre el total: una barra llena de un
   * factor pequeño está llena, y pintarla del color del total la haría mentir.
   */
  tramo(f: FactorDelDesglose): string {
    return tramoDe(this.proporcion(f));
  }
}
