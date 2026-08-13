import { Injectable, computed, signal } from '@angular/core';
import { ClaveDeMensaje, Mensaje, es } from '../i18n/es';
import { en } from '../i18n/en';

export type Idioma = 'es' | 'en';

const CLAVE = 'spotterai_idioma';

/** Los valores que se le pasan a un texto para rellenar sus huecos. */
export type Valores = Record<string, string | number>;

const CATALOGOS: Record<Idioma, Record<ClaveDeMensaje, Mensaje>> = { es, en };

/**
 * El idioma de la aplicación.
 *
 * Mismo patrón que TemaService, y a propósito: una señal, la preferencia
 * guardada y un atributo en el elemento raíz. Aquí el atributo es `lang`, que
 * no es decoración — es lo que hace que un lector de pantalla pronuncie el
 * texto en el idioma correcto y que el navegador parta las palabras bien.
 *
 * <p>No se usa el i18n nativo de Angular. El nativo trabaja en tiempo de
 * compilación: genera un paquete por idioma y cambiar de idioma significa
 * recargar en otra URL. Lo que se ha pedido es un selector que cambie la
 * aplicación en caliente, y para eso hace falta que el texto salga de una señal
 * que las plantillas puedan leer.
 *
 * <p>Al leer `idioma()` dentro de `t()`, cualquier plantilla que llame a `t()`
 * queda suscrita al cambio sin que nadie tenga que acordarse de nada.
 */
@Injectable({ providedIn: 'root' })
export class IdiomaService {

  readonly idioma = signal<Idioma>(this.leerPreferencia());

  /**
   * El identificador de región, para fechas y números.
   *
   * Va aquí y no en LOCALE_ID porque LOCALE_ID se fija al arrancar y no se
   * puede cambiar en caliente, que es justo lo que se quiere. En esta
   * aplicación sale barato: solo hay un `| date` en las veinte plantillas y el
   * resto de fechas se formatean a mano, así que pasan todas por aquí.
   */
  readonly locale = computed(() => (this.idioma() === 'es' ? 'es-ES' : 'en-GB'));

  constructor() {
    this.aplicar(this.idioma());
  }

  cambiar(idioma: Idioma): void {
    if (idioma === this.idioma()) return;
    this.idioma.set(idioma);
    this.aplicar(idioma);
    localStorage.setItem(CLAVE, idioma);
  }

  alternar(): void {
    this.cambiar(this.idioma() === 'es' ? 'en' : 'es');
  }

  /**
   * El texto de una clave, en el idioma actual.
   *
   * <p>Si el mensaje depende de la cantidad se pasa `cuenta` y se elige la
   * forma. Español e inglés parten igual —uno / otros— así que no hace falta la
   * maquinaria de CLDR para seis formas que aquí no se usan; el día que entre
   * un idioma que las necesite, este es el único sitio que cambia.
   */
  t(clave: ClaveDeMensaje, valores?: Valores): string {
    const mensaje = CATALOGOS[this.idioma()][clave];

    const plantilla = typeof mensaje === 'string'
      ? mensaje
      : Number(valores?.['cuenta']) === 1 ? mensaje.uno : mensaje.otros;

    return valores ? this.rellenar(plantilla, valores) : plantilla;
  }

  /** Una fecha en el idioma actual. */
  fecha(valor: string | Date, opciones: Intl.DateTimeFormatOptions): string {
    const fecha = typeof valor === 'string' ? new Date(valor) : valor;
    return fecha.toLocaleString(this.locale(), opciones);
  }

  private rellenar(plantilla: string, valores: Valores): string {
    // Sin hueco no se toca nada, y un hueco sin valor se queda tal cual en vez
    // de convertirse en "undefined": si algún día falta un dato, que se vea qué
    // falta y no una palabra inglesa en medio de una frase.
    return plantilla.replace(/\{(\w+)\}/g, (entero, nombre) =>
      nombre in valores ? String(valores[nombre]) : entero,
    );
  }

  private aplicar(idioma: Idioma): void {
    document.documentElement.setAttribute('lang', idioma);
  }

  /**
   * Qué idioma enseñar la primera vez.
   *
   * <p>Lo guardado manda. Si no hay nada, se mira el navegador: alguien que
   * tiene el sistema en inglés y entra por primera vez no debería encontrarse
   * una pantalla en español que no sabe cambiar. Cualquier cosa que no sea
   * inglés cae en español, que es el idioma en el que está escrita la
   * aplicación.
   */
  private leerPreferencia(): Idioma {
    const guardado = localStorage.getItem(CLAVE);
    if (guardado === 'es' || guardado === 'en') return guardado;

    return navigator.language?.toLowerCase().startsWith('en') ? 'en' : 'es';
  }
}
