import { HttpErrorResponse } from '@angular/common/http';

/**
 * Saca el motivo del error tal y como lo mandó el backend.
 *
 * <p>El backend no manda los errores todos con la misma forma: los controladores
 * más antiguos devuelven el mensaje como texto plano en el cuerpo
 * (`.badRequest().body(e.getMessage())`) y los más recientes lo devuelven en
 * JSON (`Map.of("error", e.getMessage())`). Un formulario que solo sepa leer una
 * de las dos formas acierta la mitad de las veces y falla la otra mitad sin que
 * se note por qué: el síntoma no es "no hay mensaje", es "a veces sí y a veces
 * no", que es mucho más difícil de detectar.
 *
 * <p>De ahí este ayudante: se usa en todos los formularios en vez de leer
 * `err.error` a mano en cada uno, así que arreglar la ambigüedad se hace una
 * vez y no en cada sitio donde se muestra un error.
 *
 * <p>Nunca deja el formulario sin explicación. Si el cuerpo no es reconocible
 * —una caída de red, un 500 sin cuerpo, un timeout— se devuelve el mensaje por
 * defecto que da quien llama, nunca "undefined" ni un JSON crudo.
 */
export function mensajeDeError(err: unknown, porDefecto: string): string {
  if (!(err instanceof HttpErrorResponse)) return porDefecto;

  const cuerpo = err.error;

  // Forma antigua: el cuerpo entero es el texto del mensaje.
  if (typeof cuerpo === 'string' && cuerpo.trim()) return cuerpo;

  // Forma nueva: { "error": "..." }.
  if (cuerpo && typeof cuerpo === 'object' && typeof cuerpo.error === 'string' && cuerpo.error.trim()) {
    return cuerpo.error;
  }

  return porDefecto;
}
