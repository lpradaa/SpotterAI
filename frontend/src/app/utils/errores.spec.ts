import { HttpErrorResponse } from '@angular/common/http';
import { mensajeDeError } from './errores';

/**
 * El backend manda los errores de dos formas distintas —texto plano en los
 * controladores más antiguos, JSON {error} en los más recientes—, y un
 * formulario que solo entienda una de las dos acierta la mitad de las veces
 * sin que se note un patrón. Esto es lo que se protege aquí: que las dos
 * formas se lean igual y que nunca se le enseñe a nadie "undefined".
 */
describe('mensajeDeError', () => {

  it('lee el cuerpo cuando es texto plano', () => {
    const err = new HttpErrorResponse({ error: 'El email ya está registrado.', status: 400 });
    expect(mensajeDeError(err, 'por defecto')).toBe('El email ya está registrado.');
  });

  it('lee el cuerpo cuando es JSON con la clave error', () => {
    const err = new HttpErrorResponse({ error: { error: 'Ese enlace ya no vale.' }, status: 410 });
    expect(mensajeDeError(err, 'por defecto')).toBe('Ese enlace ya no vale.');
  });

  it('cae al mensaje por defecto si el cuerpo está vacío', () => {
    const err = new HttpErrorResponse({ error: '', status: 500 });
    expect(mensajeDeError(err, 'por defecto')).toBe('por defecto');
  });

  it('cae al mensaje por defecto si el cuerpo es JSON sin la clave error', () => {
    // Una caída de red o un proxy intermedio pueden devolver cualquier JSON.
    const err = new HttpErrorResponse({ error: { motivo: 'otra cosa' }, status: 502 });
    expect(mensajeDeError(err, 'por defecto')).toBe('por defecto');
  });

  it('cae al mensaje por defecto si no hay cuerpo en absoluto', () => {
    const err = new HttpErrorResponse({ status: 0 });
    expect(mensajeDeError(err, 'por defecto')).toBe('por defecto');
  });

  it('nunca revienta ni muestra "undefined" con algo que no es un error HTTP', () => {
    expect(mensajeDeError(new Error('fallo de JavaScript'), 'por defecto')).toBe('por defecto');
    expect(mensajeDeError(null, 'por defecto')).toBe('por defecto');
    expect(mensajeDeError(undefined, 'por defecto')).toBe('por defecto');
  });
});
