import { describe, expect, it } from 'vitest';
import { tramoDe } from './compatibilidad';

/**
 * Los umbrales tienen que ser los mismos que agrupan el embudo en el backend:
 * si aqui se movieran, la pantalla diria "muy compatibles" de gente que la
 * tabla que mide el acierto cuenta en otro tramo.
 */
describe('tramoDe', () => {
  it('70 ya es alta, 69 todavia no', () => {
    expect(tramoDe(70)).toBe('alta');
    expect(tramoDe(69)).toBe('media');
  });

  it('40 ya es media, 39 todavia no', () => {
    expect(tramoDe(40)).toBe('media');
    expect(tramoDe(39)).toBe('baja');
  });

  it('los extremos caen donde deben', () => {
    expect(tramoDe(100)).toBe('alta');
    expect(tramoDe(0)).toBe('baja');
  });
});
