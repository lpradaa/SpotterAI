/**
 * Lo mecánico de la accesibilidad, sobre todas las plantillas.
 *
 * Un lector de pantalla no lee botones: lee *nombres*. Un botón cuyo contenido
 * es un icono SVG no dice nada —se oye «botón», sin más— y eso pasa sin que se
 * note mirando, porque en pantalla el icono se entiende.
 *
 * Hoy las tres comprobaciones salen a cero. Esto no arregla nada: mantiene lo
 * que ya está bien, que es justo cuando conviene escribirlo. Después habría que
 * revisar veinte plantillas para volver aquí.
 *
 * QUÉ NO ES: no sustituye a pasar un lector de pantalla de verdad. Comprueba
 * que cada control tenga un nombre; no que ese nombre se entienda, ni que el
 * orden de tabulación sea razonable, ni que la pantalla se pueda usar sin
 * ratón. Eso necesita a una persona con NVDA o VoiceOver delante.
 *
 * Va como script y no como .spec: es un análisis de ficheros, no una prueba de
 * componente, y meterlo en el runner de Angular obligaba a añadir los tipos de
 * Node a un tsconfig que no los necesita para nada más.
 *
 *   node scripts/auditar-accesibilidad.mjs
 */
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const RAIZ = join(process.cwd(), 'src/app');

function plantillas(desde = RAIZ) {
  return readdirSync(desde, { withFileTypes: true }).flatMap(entrada => {
    const ruta = join(desde, entrada.name);
    if (entrada.isDirectory()) return plantillas(ruta);
    return entrada.name.endsWith('.html') ? [ruta] : [];
  });
}

const nombre = ruta => ruta.split(/[\/]/).pop();

/** Lo que queda de un bloque al quitarle etiquetas y control de flujo. */
function textoVisible(bloque) {
  return bloque
    .slice(bloque.indexOf('>') + 1)
    .replace(/<[^>]*>/g, ' ')
    .replace(/@(if|else|for|empty)[^{]*\{?/g, ' ')
    .replace(/[{}]/g, ' ')
    .trim();
}

const problemas = [];

for (const ruta of plantillas()) {
  const html = readFileSync(ruta, 'utf-8');

  // 1. Botones sin nombre que leer. El caso que de verdad pasa: el de icono.
  for (const bloque of html.match(/<button\b[\s\S]*?<\/button>/g) ?? []) {
    const tieneNombre = bloque.includes('aria-label') || /\btitle\s*=|\[title\]/.test(bloque);
    if (!tieneNombre && !textoVisible(bloque)) {
      problemas.push({
        regla: 'botón sin nombre',
        porque: 'sin aria-label se oye «botón» y nada más',
        donde: `${nombre(ruta)}: ${bloque.slice(0, 70).replace(/\s+/g, ' ')}`,
      });
    }
  }

  // 2. Imágenes sin alt. Con alt="" se declara decorativa y se salta, que
  //    también es una respuesta; lo que no vale es no decir nada.
  for (const etiqueta of html.match(/<img\b[^>]*>/g) ?? []) {
    if (!etiqueta.includes('alt')) {
      problemas.push({
        regla: 'imagen sin alt',
        porque: 'un lector acaba leyendo la URL del fichero',
        donde: `${nombre(ruta)}: ${etiqueta.slice(0, 70)}`,
      });
    }
  }

  // 3. Campos sin etiqueta. El patrón de la aplicación es envolverlos en un
  //    <label>; aria-label vale igual cuando no hay sitio para texto visible.
  for (const m of html.matchAll(/<input\b[^>]*>/g)) {
    const etiqueta = m[0];
    if (etiqueta.includes('hidden') || etiqueta.includes('aria-label') || etiqueta.includes('id=')) {
      continue;
    }
    const antes = html.slice(0, m.index);
    if (antes.lastIndexOf('<label') > antes.lastIndexOf('</label>')) continue;

    problemas.push({
      regla: 'campo sin etiqueta',
      porque: 'se oye como «edición, en blanco»',
      donde: `${nombre(ruta)}: ${etiqueta.slice(0, 70)}`,
    });
  }
}

const cuantas = plantillas().length;

if (problemas.length === 0) {
  console.log(`Accesibilidad: ${cuantas} plantillas, ningún control sin nombre.`);
  process.exit(0);
}

console.error(`\nAccesibilidad: ${problemas.length} problema(s) en ${cuantas} plantillas\n`);
for (const p of problemas) {
  console.error(`  [${p.regla}] ${p.donde}`);
  console.error(`      ${p.porque}\n`);
}
process.exit(1);
