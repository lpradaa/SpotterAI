import { Component, computed, effect, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Franja } from '../rejilla-semana/rejilla-semana';

const DIAS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
const ABREVIADOS = ['L', 'M', 'X', 'J', 'V', 'S', 'D'];

/** Ventana fija, al contrario que en la rejilla de solo lectura. */
const HORA_DESDE = 6;
const HORA_HASTA = 23;

function clave(dia: string | undefined): string {
  if (!dia) return '';
  return dia.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();
}

const INDICE_DIA = new Map(DIAS.map((d, i) => [clave(d), i]));

/**
 * Rejilla para pintar tu disponibilidad a golpe de clic.
 *
 * Es hermana de {@link RejillaSemana} pero no una variante suya: aquella calcula
 * la ventana horaria a partir de los datos, y aquí puede no haber ninguno
 * todavía, que es justo el caso para el que existe. Por eso la ventana es fija.
 *
 * Se trabaja por celdas de una hora y al guardar se funden las horas seguidas
 * del mismo día en una sola franja, de modo que pintar de 19 a 22 produce un
 * tramo y no tres.
 */
@Component({
  selector: 'app-rejilla-editable',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rejilla-editable.html',
  styleUrl: './rejilla-editable.scss'
})
export class RejillaEditable {

  /** Franjas de partida, para poder editar un horario ya guardado. */
  franjasIniciales = input<Franja[]>([]);

  /** Se emite en cada cambio, ya fundido en franjas. */
  cambio = output<Franja[]>();

  readonly dias = DIAS;
  readonly abreviados = ABREVIADOS;
  readonly horas = Array.from({ length: HORA_HASTA - HORA_DESDE }, (_, i) => HORA_DESDE + i);

  /** Celdas pintadas, como "indiceDia:hora". */
  private celdas = signal<Set<string>>(new Set());

  /** Se rellena una sola vez, cuando llegan las franjas de partida. */
  private sembrado = false;

  horasPintadas = computed(() => this.celdas().size);

  constructor() {
    // Las franjas iniciales llegan de una peticion, o sea despues de que exista
    // el componente: sembrar solo en el constructor las encontraria vacias y la
    // rejilla saldria en blanco con un horario ya guardado. El effect espera a
    // que lleguen, y `sembrado` impide que una llegada posterior machaque lo que
    // la persona haya pintado entretanto.
    effect(() => this.sembrar(this.franjasIniciales()));
  }

  private sembrar(iniciales: Franja[]): void {
    if (this.sembrado || iniciales.length === 0) return;

    const set = new Set<string>();
    for (const f of iniciales) {
      const dia = INDICE_DIA.get(clave(f.diaSemana ?? f.dia));
      if (dia === undefined) continue;

      const desde = parseInt((f.horaInicio ?? f.inicio ?? '').split(':')[0], 10);
      const hasta = parseInt((f.horaFin ?? f.fin ?? '').split(':')[0], 10);
      if (Number.isNaN(desde) || Number.isNaN(hasta)) continue;

      for (let h = Math.max(desde, HORA_DESDE); h < Math.min(hasta, HORA_HASTA); h++) {
        set.add(`${dia}:${h}`);
      }
    }
    this.celdas.set(set);
    this.sembrado = true;
  }

  estaPintada(dia: number, hora: number): boolean {
    return this.celdas().has(`${dia}:${hora}`);
  }

  alternar(dia: number, hora: number): void {
    const set = new Set(this.celdas());
    const id = `${dia}:${hora}`;
    if (set.has(id)) {
      set.delete(id);
    } else {
      set.add(id);
    }
    this.celdas.set(set);
    this.sembrado = true; // ya hay intervención humana: no volver a sembrar
    this.cambio.emit(this.aFranjas());
  }

  /** Funde las horas contiguas de cada día en una sola franja. */
  private aFranjas(): Franja[] {
    const franjas: Franja[] = [];

    for (let dia = 0; dia < DIAS.length; dia++) {
      const horas = this.horas.filter(h => this.estaPintada(dia, h)).sort((a, b) => a - b);
      if (horas.length === 0) continue;

      let inicio = horas[0];
      let anterior = horas[0];

      for (const h of horas.slice(1)) {
        if (h !== anterior + 1) {
          franjas.push(this.franja(dia, inicio, anterior + 1));
          inicio = h;
        }
        anterior = h;
      }
      franjas.push(this.franja(dia, inicio, anterior + 1));
    }
    return franjas;
  }

  private franja(dia: number, desde: number, hasta: number): Franja {
    const dosCifras = (n: number) => String(n).padStart(2, '0');
    return {
      diaSemana: DIAS[dia],
      horaInicio: `${dosCifras(desde)}:00`,
      horaFin: `${dosCifras(hasta)}:00`,
      // Siempre entra como disponibilidad, nunca como compromiso. Marcar "voy
      // siempre" es una accion aparte y deliberada, y eso es lo que evita que un
      // horario pintado con desgana pese como uno real.
      habitual: false
    };
  }
}
