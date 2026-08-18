import { Component, computed, inject, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IdiomaService } from '../../services/idioma.service';
import { etiquetaDeDia, indiceDeDia, VALORES_DE_DIA } from '../../utils/valores-de-perfil';

/** Una franja horaria, tal y como la manejan el perfil y el solape. */
export interface Franja {
  diaSemana?: string;
  dia?: string;
  horaInicio?: string;
  horaFin?: string;
  inicio?: string;
  fin?: string;
  habitual?: boolean;
  ambosFijos?: boolean;
}

interface Bloque {
  columna: number;   // 1..7
  desde: number;     // minutos desde el inicio de la ventana visible
  hasta: number;
  fijo: boolean;
}

function aMinutos(hora: string | undefined): number | null {
  if (!hora) return null;
  const [h, m] = hora.split(':').map(Number);
  if (Number.isNaN(h) || Number.isNaN(m)) return null;
  return h * 60 + m;
}

/**
 * Dibuja la semana y marca en ella cuándo coincidís.
 *
 * Es la pieza que hace visible la tesis del producto. El backend cruza los
 * horarios al minuto y pondera por compromiso; hasta ahora eso se mostraba como
 * dos chips de texto que decían "Lunes, Martes". Aquí se ve: tu semana de fondo,
 * el solape encendido encima, y los tramos a los que ambos vais siempre con un
 * tratamiento distinto porque son otra cosa.
 *
 * Dos modos:
 * - `completa`: rejilla con horas, para la ficha de un candidato o tu perfil.
 * - `compacta`: siete columnas sin etiquetas, para caber en una fila de lista.
 */
@Component({
  selector: 'app-rejilla-semana',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './rejilla-semana.html',
  styleUrl: './rejilla-semana.scss'
})
export class RejillaSemana {
  /** Mi disponibilidad, que se dibuja de fondo. */
  misFranjas = input<Franja[]>([]);

  /** Los tramos en común, que se dibujan encima. */
  solape = input<Franja[]>([]);

  modo = input<'completa' | 'compacta'>('completa');

  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);

  /**
   * Las siete cabeceras, ya leídas y al ancho que toca.
   *
   * <p>Sale de los valores guardados y no de una lista propia: el orden de las
   * columnas y el vocabulario que se cruza son el mismo dato, y tenerlo dos
   * veces es tenerlo mal una de ellas.
   */
  protected cabeceras = computed(() => VALORES_DE_DIA.map(
    dia => etiquetaDeDia(dia, c => this.i18n.t(c),
      this.modo() === 'compacta' ? 'estrecho' : 'abrev')));

  /**
   * Ventana horaria visible.
   *
   * No se dibujan las 24 horas: casi nadie entrena de madrugada y mostrarlas
   * dejaría la rejilla vacía en dos tercios de su altura. Se calcula a partir de
   * los datos reales, con un margen, para que lo que se ve sea lo que importa.
   */
  private ventana = computed(() => {
    const todas = [...this.misFranjas(), ...this.solape()];
    const inicios = todas.map(f => aMinutos(f.horaInicio ?? f.inicio)).filter((n): n is number => n !== null);
    const fines = todas.map(f => aMinutos(f.horaFin ?? f.fin)).filter((n): n is number => n !== null);

    if (inicios.length === 0) return { desde: 6 * 60, hasta: 23 * 60 };

    const desde = Math.max(0, Math.min(...inicios) - 60);
    const hasta = Math.min(24 * 60, Math.max(...fines) + 60);

    // Una ventana muy estrecha se ve rara; se garantiza un mínimo de 6 horas
    if (hasta - desde < 360) {
      const centro = (desde + hasta) / 2;
      return { desde: Math.max(0, centro - 180), hasta: Math.min(24 * 60, centro + 180) };
    }
    return { desde, hasta };
  });

  /** Horas que se etiquetan a la izquierda. */
  horasVisibles = computed(() => {
    const { desde, hasta } = this.ventana();
    const horas: number[] = [];
    const paso = hasta - desde > 600 ? 3 : 2; // menos etiquetas si la ventana es larga
    for (let h = Math.ceil(desde / 60); h * 60 <= hasta; h += paso) horas.push(h);
    return horas;
  });

  bloquesMios = computed(() => this.aBloques(this.misFranjas(), false));
  bloquesSolape = computed(() => this.aBloques(this.solape(), true));

  hayDatos = computed(() => this.misFranjas().length > 0 || this.solape().length > 0);
  haySolape = computed(() => this.solape().length > 0);

  /**
   * La misma información, dicha con palabras.
   *
   * <p>La rejilla es el elemento más importante de la aplicación —es donde se ve
   * cuándo podéis entrenar juntos— y transmitía todo su contenido en color y
   * posición: las cabeceras iban con `aria-hidden` y los tramos eran `span`
   * vacíos con estilos en línea. Para quien no ve, ahí no había nada.
   *
   * <p>No es un resumen sino el equivalente: cada tramo, con su día, su hora y
   * lo que significa. Un `aria-label` con todo junto se leería como un párrafo
   * de corrido; una lista se puede recorrer elemento a elemento.
   */
  descripcion = computed<string[]>(() => {
    const solape = this.solape().map(f => this.describir(f, true));
    // Si coincidís, lo que importa es dónde. Enumerar además tu semana entera
    // sería enterrar la respuesta debajo del contexto.
    if (solape.length > 0) return solape;
    return this.misFranjas().map(f => this.describir(f, false));
  });

  /** Titular de la rejilla, para saber qué se está oyendo antes de la lista. */
  titular = computed(() => {
    const cuantos = this.solape().length;
    if (cuantos === 0) {
      return this.i18n.t(this.misFranjas().length > 0 ? 'rejilla.tuSemana' : 'rejilla.vacia');
    }
    return this.i18n.t('rejilla.coincidisEn', { cuenta: cuantos });
  });

  /**
   * Un tramo, dicho entero.
   *
   * <p>La frase va completa en el catálogo, con el día y las horas dentro, en
   * vez de armar «Lunes de 18:00 a 20:00» y meterlo luego en «…: coincidís».
   * Componer obliga a que las dos piezas vayan en el mismo orden en los dos
   * idiomas, y es la forma típica de acabar con media frase traducida.
   */
  private describir(f: Franja, esSolape: boolean): string {
    const guardado = f.diaSemana ?? f.dia ?? '';
    const valores = {
      dia: etiquetaDeDia(guardado, c => this.i18n.t(c)),
      desde: (f.horaInicio ?? f.inicio ?? '').slice(0, 5),
      hasta: (f.horaFin ?? f.fin ?? '').slice(0, 5),
    };

    if (esSolape) {
      return this.i18n.t(
        f.ambosFijos ? 'rejilla.tramoCoincidisFijo' : 'rejilla.tramoCoincidis', valores);
    }
    return this.i18n.t(f.habitual ? 'rejilla.tramoVasSiempre' : 'rejilla.tramo', valores);
  }

  /** Posición vertical de una hora dentro de la rejilla, en porcentaje. */
  posicionHora(hora: number): number {
    const { desde, hasta } = this.ventana();
    return ((hora * 60 - desde) / (hasta - desde)) * 100;
  }

  private aBloques(franjas: Franja[], esSolape: boolean): Bloque[] {
    const { desde, hasta } = this.ventana();
    const rango = hasta - desde;
    if (rango <= 0) return [];

    const bloques: Bloque[] = [];
    for (const f of franjas) {
      const indice = indiceDeDia(f.diaSemana ?? f.dia);
      if (indice === null) continue;

      const inicio = aMinutos(f.horaInicio ?? f.inicio);
      const fin = aMinutos(f.horaFin ?? f.fin);
      if (inicio === null || fin === null || fin <= inicio) continue;

      bloques.push({
        columna: indice + 1,
        desde: ((Math.max(inicio, desde) - desde) / rango) * 100,
        hasta: ((Math.min(fin, hasta) - desde) / rango) * 100,
        fijo: esSolape ? !!f.ambosFijos : !!f.habitual
      });
    }
    return bloques;
  }

  /**
   * Estilo inline de un bloque: es geometría calculada, no decoración.
   *
   * Los bloques van posicionados en absoluto, así que la columna se traduce a
   * coordenadas; no puede resolverse con grid-column porque el posicionamiento
   * absoluto los saca del flujo de la rejilla.
   */
  estiloBloque(b: Bloque): Record<string, string> {
    const ancho = 100 / 7;
    // 3 px en completa y no 2: con el lienzo a 240 los bloques son mas anchos de
    // lo que eran, y dos franjas contiguas del mismo dia se tocaban hasta
    // parecer una sola. El aire entre columnas es lo que deja contar los dias.
    const separacion = this.modo() === 'compacta' ? 1 : 3;

    return {
      left: `calc(${(b.columna - 1) * ancho}% + ${separacion}px)`,
      width: `calc(${ancho}% - ${separacion * 2}px)`,
      top: `${b.desde}%`,
      height: `${Math.max(b.hasta - b.desde, 2)}%`
    };
  }
}
