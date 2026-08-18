import {
  etiquetaDeDia,
  etiquetaDeGenero,
  etiquetaDeNivel,
  etiquetaDeObjetivo,
  indiceDeDia,
  VALORES_DE_DIA,
} from './valores-de-perfil';
import type { ClaveDeMensaje } from '../i18n/es';

/**
 * El traductor de mentira devuelve la clave, que es justo lo que hay que
 * comprobar: lo que se prueba aquí no es la traducción sino que del valor
 * guardado se llegue a la clave correcta.
 */
const t = (clave: ClaveDeMensaje) => clave;

describe('valores-de-perfil', () => {

  it('lleva el nivel guardado a su clave', () => {
    expect(etiquetaDeNivel('Principiante', t)).toBe('nivel.principiante');
    expect(etiquetaDeNivel('Intermedio', t)).toBe('nivel.intermedio');
    expect(etiquetaDeNivel('Avanzado', t)).toBe('nivel.avanzado');
  });

  it('lleva el objetivo guardado a su clave', () => {
    expect(etiquetaDeObjetivo('Hipertrofia', t)).toBe('objetivo.hipertrofia');
    expect(etiquetaDeObjetivo('Fuerza', t)).toBe('objetivo.fuerza');
    expect(etiquetaDeObjetivo('Resistencia', t)).toBe('objetivo.resistencia');
  });

  it('lleva el género guardado a su clave', () => {
    expect(etiquetaDeGenero('Masculino', t)).toBe('genero.masculino');
    expect(etiquetaDeGenero('Femenino', t)).toBe('genero.femenino');
    expect(etiquetaDeGenero('Otro', t)).toBe('genero.otro');
  });

  // El único valor con tilde y con espacios, que es donde se rompería.
  it('reconoce «Pérdida de peso» con su tilde', () => {
    expect(etiquetaDeObjetivo('Pérdida de peso', t)).toBe('objetivo.perdidaDePeso');
  });

  // Lo mismo que ya guardó alguien, pero escrito de otra manera. La base tiene
  // texto libre de cuando esto era un campo sin desplegable.
  it('no depende de las mayúsculas, la tilde ni los espacios de los lados', () => {
    expect(etiquetaDeObjetivo('PERDIDA DE PESO', t)).toBe('objetivo.perdidaDePeso');
    expect(etiquetaDeNivel('  intermedio ', t)).toBe('nivel.intermedio');
  });

  /**
   * Lo que no conocemos se enseña tal cual y no como hueco: si alguien tiene
   * guardado algo de una versión anterior, mejor que vea lo que puso.
   */
  it('devuelve tal cual lo que no reconoce', () => {
    expect(etiquetaDeNivel('Semiprofesional', t)).toBe('Semiprofesional');
    expect(etiquetaDeObjetivo('', t)).toBe('');
  });

  describe('los días', () => {

    it('lleva cada día a su clave, al ancho que se pida', () => {
      expect(etiquetaDeDia('Lunes', t)).toBe('dia.lunes');
      expect(etiquetaDeDia('Lunes', t, 'abrev')).toBe('dia.lunesAbrev');
      expect(etiquetaDeDia('Lunes', t, 'estrecho')).toBe('dia.lunesEstrecho');
    });

    // «Miércoles» y «Miercoles» tienen que dar la misma clave: la base lleva las
    // dos, de cuando el día se escribía a mano.
    it('reconoce el día con y sin tilde', () => {
      expect(etiquetaDeDia('Miércoles', t)).toBe('dia.miercoles');
      expect(etiquetaDeDia('miercoles', t)).toBe('dia.miercoles');
      expect(etiquetaDeDia('SÁBADO', t)).toBe('dia.sabado');
    });

    it('sitúa cada día en su columna', () => {
      expect(indiceDeDia('Lunes')).toBe(0);
      expect(indiceDeDia('Miercoles')).toBe(2);
      expect(indiceDeDia('Domingo')).toBe(6);
    });

    /**
     * Null y no −1. Un −1 usado como índice sin querer devuelve el último día,
     * o sea un lunes dibujado en la columna del domingo: la rejilla enseñaría
     * un solape que no existe, que es lo único que la rejilla tiene que decir.
     */
    it('no da columna para lo que no reconoce', () => {
      expect(indiceDeDia('Lunnes')).toBeNull();
      expect(indiceDeDia(undefined)).toBeNull();
      expect(indiceDeDia('')).toBeNull();
    });

    it('el vocabulario que se guarda sigue en español y en orden', () => {
      expect(VALORES_DE_DIA[0]).toBe('Lunes');
      expect(VALORES_DE_DIA).toHaveLength(7);
      // Cada valor guardado se reconoce a sí mismo: si alguien tradujera esta
      // lista, la rejilla dejaría de encontrar sus propias franjas.
      VALORES_DE_DIA.forEach((dia, i) => expect(indiceDeDia(dia)).toBe(i));
    });
  });
});
