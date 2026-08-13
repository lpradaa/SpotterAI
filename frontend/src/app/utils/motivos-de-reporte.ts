import type { ClaveDeMensaje } from '../i18n/es';

/**
 * Los motivos por los que se puede reportar a alguien.
 *
 * Aquí viven solo los **valores**, que son los que viajan al backend y tienen
 * que coincidir con el enum MotivoReporte de Java. Cómo se leen es otra cosa y
 * vive en los catálogos de idioma, bajo `motivo.<VALOR>`: un valor de enum no
 * se traduce, una etiqueta sí.
 *
 * <p>La lista estaba dentro de perfil-publico.ts, que es donde se rellena el
 * formulario, y la pantalla de moderación no la tenía: enseñaba el valor crudo.
 * Quien modera leía «COMPORTAMIENTO_INAPROPIADO», que es la clave de un enum de
 * Java asomando en una pantalla de producto.
 */
export const VALORES_DE_MOTIVO = [
  'COMPORTAMIENTO_INAPROPIADO',
  'PERFIL_FALSO',
  'ACOSO',
  'SPAM',
  'OTRO',
] as const;

export type ValorDeMotivo = typeof VALORES_DE_MOTIVO[number];

/**
 * La clave de catálogo de un motivo.
 *
 * El tipo de vuelta es literal a propósito: si algún día se añade un valor aquí
 * y no su texto en los catálogos, esto deja de compilar. Es la misma red que
 * tiene el resto de las claves, aplicada a las que se construyen.
 */
export function claveDeMotivo(valor: ValorDeMotivo): ClaveDeMensaje {
  return `motivo.${valor}`;
}

/**
 * Cómo se lee un motivo, con el traductor que se le pase.
 *
 * <p>Un valor que no conocemos se devuelve tal cual y no como cadena vacía: si
 * el backend añade un motivo y aquí no está, quien modera verá algo raro pero
 * seguirá viendo *qué* es. Un hueco en blanco esconde el reporte entero.
 */
export function etiquetaDeMotivo(
  valor: string,
  t: (clave: ClaveDeMensaje) => string,
): string {
  const conocido = (VALORES_DE_MOTIVO as readonly string[]).includes(valor);
  return conocido ? t(claveDeMotivo(valor as ValorDeMotivo)) : valor;
}
