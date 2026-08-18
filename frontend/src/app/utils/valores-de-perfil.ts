import type { ClaveDeMensaje } from '../i18n/es';

/**
 * Cómo se leen el nivel, el objetivo, el género y el día que hay guardados.
 *
 * <p>Los cuatro se guardan en la base como texto en español —«Intermedio»,
 * «Pérdida de peso», «Masculino», «Lunes»—, de cuando no había más que un
 * idioma. El valor no se toca: viaja al backend, se compara para puntuar y
 * migrarlo obligaría a tocar también todo lo que lo compara. Lo que se traduce
 * es cómo se lee, y para eso hay que volver del valor a la clave del catálogo.
 *
 * <p>Es la misma decisión que ya tomó el backend en CalculadoraCompatibilidad,
 * y a propósito la misma normalización: quitar acentos y bajar a minúsculas. Si
 * los dos lados normalizaran distinto, la misma persona saldría traducida en la
 * frase del motor y sin traducir en el chip de su tarjeta.
 *
 * <p>Lo que no reconozcamos se devuelve tal cual, como en {@link
 * ./motivos-de-reporte}: si alguien tiene guardado algo de una versión anterior,
 * mejor que vea lo que puso que un hueco en blanco.
 */

/** El traductor que se le pasa, para no acoplar esto al servicio de idioma. */
export type Traductor = (clave: ClaveDeMensaje) => string;

/**
 * Quita acentos y baja a minúsculas, igual que hace el backend con lo guardado.
 *
 * <p>Por propiedad y no por rango de códigos: el rango de marcas combinantes
 * son signos que se pintan encima del carácter anterior, así que escrito tal
 * cual dentro de los corchetes la clase parece vacía y el acento aparece
 * colgado del corchete. `\p{Diacritic}` dice lo mismo y se puede leer.
 */
function normalizar(valor: string): string {
  return valor.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase().trim();
}

const NIVELES: Record<string, ClaveDeMensaje> = {
  'principiante': 'nivel.principiante',
  'intermedio': 'nivel.intermedio',
  'avanzado': 'nivel.avanzado',
};

const OBJETIVOS: Record<string, ClaveDeMensaje> = {
  'hipertrofia': 'objetivo.hipertrofia',
  'fuerza': 'objetivo.fuerza',
  'perdida de peso': 'objetivo.perdidaDePeso',
  'resistencia': 'objetivo.resistencia',
};

const GENEROS: Record<string, ClaveDeMensaje> = {
  'masculino': 'genero.masculino',
  'femenino': 'genero.femenino',
  'otro': 'genero.otro',
};

/**
 * Los colores del avatar.
 *
 * Se guardan igual que el resto —la clave viaja al backend— y el nombre es lo
 * unico que cambia. Sin esto, el boton se lee «ascua colour» con la pantalla en
 * ingles, y ese texto solo existe para quien usa un lector de pantalla: mirando
 * no se nota, porque lo que se ve es el circulo de color.
 */
const COLORES: Record<string, ClaveDeMensaje> = {
  'ascua': 'color.ascua',
  'ambar': 'color.ambar',
  'oliva': 'color.oliva',
  'acero': 'color.acero',
  'ciruela': 'color.ciruela',
  'pizarra': 'color.pizarra',
};

function etiquetaDe(valor: string, catalogo: Record<string, ClaveDeMensaje>, t: Traductor): string {
  const clave = catalogo[normalizar(valor)];
  return clave ? t(clave) : valor;
}

export const etiquetaDeNivel = (valor: string, t: Traductor): string =>
  etiquetaDe(valor, NIVELES, t);

export const etiquetaDeObjetivo = (valor: string, t: Traductor): string =>
  etiquetaDe(valor, OBJETIVOS, t);

export const etiquetaDeGenero = (valor: string, t: Traductor): string =>
  etiquetaDe(valor, GENEROS, t);

export const etiquetaDeColor = (valor: string, t: Traductor): string =>
  etiquetaDe(valor, COLORES, t);

// ===================== Los días de la semana =====================

/**
 * Los siete días, en orden y con el texto que se guarda.
 *
 * <p>Este es el vocabulario de datos, no la cabecera de la rejilla: la rejilla
 * editable escribe {@code diaSemana: VALORES_DE_DIA[columna]} en cada franja que
 * se pinta, y eso es lo que viaja al backend y lo que cruza los horarios. Se
 * queda en español pase lo que pase.
 *
 * <p>Estaba copiado en los dos componentes de rejilla, con su normalización y su
 * índice al lado, también copiados. Dos listas del vocabulario que decide si dos
 * personas coinciden es una de más.
 */
export const VALORES_DE_DIA = [
  'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo',
] as const;

/**
 * A qué ancho se pide el día.
 *
 * <p>`completo` es el que se oye, `abrev` la cabecera de la rejilla grande y
 * `estrecho` la fila de siete columnas que cabe en una tarjeta.
 */
export type FormaDeDia = 'completo' | 'abrev' | 'estrecho';

const DIAS: Record<string, Record<FormaDeDia, ClaveDeMensaje>> = {
  'lunes':      { completo: 'dia.lunes',      abrev: 'dia.lunesAbrev',      estrecho: 'dia.lunesEstrecho' },
  'martes':     { completo: 'dia.martes',     abrev: 'dia.martesAbrev',     estrecho: 'dia.martesEstrecho' },
  'miercoles':  { completo: 'dia.miercoles',  abrev: 'dia.miercolesAbrev',  estrecho: 'dia.miercolesEstrecho' },
  'jueves':     { completo: 'dia.jueves',     abrev: 'dia.juevesAbrev',     estrecho: 'dia.juevesEstrecho' },
  'viernes':    { completo: 'dia.viernes',    abrev: 'dia.viernesAbrev',    estrecho: 'dia.viernesEstrecho' },
  'sabado':     { completo: 'dia.sabado',     abrev: 'dia.sabadoAbrev',     estrecho: 'dia.sabadoEstrecho' },
  'domingo':    { completo: 'dia.domingo',    abrev: 'dia.domingoAbrev',    estrecho: 'dia.domingoEstrecho' },
};

/**
 * En qué columna va un día guardado, o `null` si no lo conocemos.
 *
 * <p>Null y no −1: un −1 usado como índice sin querer devuelve el último día de
 * la semana, que es un lunes dibujado en la columna del domingo.
 */
export function indiceDeDia(valor: string | undefined): number | null {
  if (!valor) return null;
  const i = VALORES_DE_DIA.findIndex(d => normalizar(d) === normalizar(valor));
  return i === -1 ? null : i;
}

/** Cómo se lee un día guardado, al ancho que se pida. */
export function etiquetaDeDia(valor: string, t: Traductor, forma: FormaDeDia = 'completo'): string {
  const claves = DIAS[normalizar(valor)];
  return claves ? t(claves[forma]) : valor;
}
