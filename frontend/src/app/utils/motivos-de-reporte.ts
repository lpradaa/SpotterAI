/**
 * Los motivos por los que se puede reportar a alguien, con su etiqueta.
 *
 * Vivían dentro de perfil-publico.ts, que es donde se rellena el formulario, y
 * la pantalla de moderación no los tenía: enseñaba el valor crudo que viaja al
 * backend. Quien modera leía «COMPORTAMIENTO_INAPROPIADO» y «PERFIL_FALSO»,
 * que es la clave de un enum de Java asomando en una pantalla de producto.
 *
 * Copiar la lista en la pantalla de moderación habría sido tener dos, y dos
 * listas de lo mismo divergen a la primera corrección que solo se aplica en
 * una. Los valores tienen que coincidir además con MotivoReporte del backend,
 * así que cuantas menos copias, mejor.
 */
export interface MotivoDeReporte {
  /** Lo que viaja al backend. Coincide con el enum MotivoReporte. */
  valor: string;
  /** Lo que lee una persona. */
  etiqueta: string;
}

export const MOTIVOS_DE_REPORTE: readonly MotivoDeReporte[] = [
  { valor: 'COMPORTAMIENTO_INAPROPIADO', etiqueta: 'Comportamiento inapropiado' },
  { valor: 'PERFIL_FALSO', etiqueta: 'Perfil falso o suplantación' },
  { valor: 'ACOSO', etiqueta: 'Acoso o mensajes no deseados' },
  { valor: 'SPAM', etiqueta: 'Spam o publicidad' },
  { valor: 'OTRO', etiqueta: 'Otro motivo' },
];

/**
 * La etiqueta de un motivo, o el valor tal cual si no lo conocemos.
 *
 * Devolver el valor crudo y no una cadena vacía es a propósito: si algún día el
 * backend añade un motivo y aquí no está, quien modera verá algo raro pero
 * seguirá viendo *qué* es. Un hueco en blanco esconde el reporte entero.
 */
export function etiquetaDeMotivo(valor: string): string {
  return MOTIVOS_DE_REPORTE.find(m => m.valor === valor)?.etiqueta ?? valor;
}
