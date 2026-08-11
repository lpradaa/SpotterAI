/** Los tres tramos de la escala de compatibilidad. */
export type Tramo = 'alta' | 'media' | 'baja';

/**
 * En qué tramo cae una puntuación.
 *
 * <p>Los umbrales son los mismos que usa el backend para agrupar el embudo
 * (alta 70+, media 40-69, baja por debajo), así que la pantalla que dice "muy
 * compatibles" y la tabla que mide si eso se cumple hablan del mismo tramo.
 *
 * <p>Vivía copiado en explore.ts y en ficha-sugerencia.ts, y perfil-publico no
 * lo tenía —por eso su número salía siempre del color del acento en vez de la
 * escala—. Tres pantallas pintando la misma magnitud de tres maneras.
 */
export function tramoDe(puntuacion: number): Tramo {
  if (puntuacion >= 70) return 'alta';
  if (puntuacion >= 40) return 'media';
  return 'baja';
}
