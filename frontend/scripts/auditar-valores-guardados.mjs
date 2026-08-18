/**
 * Valores guardados que se pintan crudos, sobre todas las plantillas.
 *
 * El nivel, el objetivo y el género se guardan en español —son el dato, viajan
 * al backend y se comparan— y la pantalla solo cambia su *etiqueta*. Cuando una
 * plantilla los interpola tal cual, en español no se nota nada: «Intermedio» es
 * exactamente lo que había que enseñar. En inglés se queda en «Intermedio» con
 * el resto de la pantalla traducida.
 *
 * Por eso hace falta esto y no bastaba con revisar: el fallo es invisible en el
 * idioma en el que se desarrolla. Apareció en el tablero después de dar por
 * traducidas las veinte pantallas, y no lo cazó ninguna prueba —las de los
 * componentes traducidos pasaban, porque el tablero no tenía ninguna—.
 *
 * QUÉ MIRA: solo el texto que alguien lee. Las interpolaciones y los tres
 * atributos que acaban en voz alta (aria-label, alt, title).
 *
 * QUÉ NO MIRA, a propósito: [value], [(ngModel)] y compañía. Ahí el valor crudo
 * es lo correcto —es lo que se guarda—, y avisar sería empujar justo al error
 * contrario: traducir el dato.
 *
 *   node scripts/auditar-valores-guardados.mjs
 */
import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const RAIZ = join(process.cwd(), 'src/app');

/** Los campos que llevan un valor guardado en español. */
const CAMPOS = ['nivel', 'objetivos', 'genero'];

/** Señales de que la expresión ya pasa por una etiqueta. */
const ETIQUETADO = /\b(nivel|objetivo|genero)\s*\(|etiquetaDe/;

function plantillas(desde = RAIZ) {
  return readdirSync(desde, { withFileTypes: true }).flatMap(entrada => {
    const ruta = join(desde, entrada.name);
    if (entrada.isDirectory()) return plantillas(ruta);
    return entrada.name.endsWith('.html') ? [ruta] : [];
  });
}

const nombre = ruta => ruta.split(/[\/]/).pop();

/**
 * Fuera las cadenas literales antes de mirar.
 *
 * Sin esto, i18n.t('explorar.nivel') —que es una clave de traducción, o sea
 * justo lo que hay que hacer— saldría marcado por contener «.nivel».
 */
const sinCadenas = expresion => expresion.replace(/'[^']*'|"[^"]*"/g, "''");

const problemas = [];

for (const ruta of plantillas()) {
  const html = readFileSync(ruta, 'utf-8');

  const lugares = [
    ...[...html.matchAll(/\{\{([\s\S]*?)\}\}/g)].map(m => ({ expresion: m[1], como: 'texto' })),
    ...[...html.matchAll(/\[(?:attr\.)?(?:aria-label|alt|title)\]\s*=\s*"([^"]*)"/g)]
      .map(m => ({ expresion: m[1], como: 'texto accesible' })),
  ];

  for (const { expresion, como } of lugares) {
    const limpia = sinCadenas(expresion);
    if (ETIQUETADO.test(limpia)) continue;

    const campo = CAMPOS.find(c => new RegExp(`\\.${c}\\b`).test(limpia));
    if (!campo) continue;

    problemas.push({
      campo,
      donde: `${nombre(ruta)} (${como}): ${expresion.trim().slice(0, 60)}`,
    });
  }
}

const cuantas = plantillas().length;

if (problemas.length === 0) {
  console.log(`Valores guardados: ${cuantas} plantillas, ninguno se lee sin etiquetar.`);
  process.exit(0);
}

console.error(`\nValores guardados sin etiquetar: ${problemas.length} en ${cuantas} plantillas\n`);
for (const p of problemas) {
  console.error(`  [${p.campo}] ${p.donde}`);
  console.error(`      se lee tal cual: en inglés sale el valor en español\n`);
}
console.error('  Arreglo: pasarlo por etiquetaDeNivel / etiquetaDeObjetivo / etiquetaDeGenero');
console.error('  (utils/valores-de-perfil.ts). El valor que se guarda no cambia.\n');
process.exit(1);
