import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { PerfilPublicoComponent } from './perfil-publico';
import { Levantamiento, PerfilPublico } from '../../services/perfiles.service';

/**
 * La página donde se decide.
 *
 * Lo que se fija aquí son reglas de producto, no pintado. La del 10 % sobre
 * todo: por debajo de esa diferencia no hay un «más fuerte», hay dos personas
 * que levantan lo mismo — que es justo el caso para el que existe un spotter.
 * Si alguien la simplifica a un `>` normal no se rompe nada visible: la tabla
 * empieza a llamar más fuerte a uno de dos que levantan igual, y contradice en
 * silencio para qué está la aplicación.
 */
describe('PerfilPublicoComponent', () => {

  let componente: PerfilPublicoComponent;

  function marca(ejercicio: string, peso: number, reps: number, maximo: number): Levantamiento {
    return { ejercicio, nombre: ejercicio, peso, repeticiones: reps, maximoEstimado: maximo };
  }

  function perfil(datos: Partial<PerfilPublico>): PerfilPublico {
    return {
      id: 2, nombre: 'Javi Ortega', esMio: false,
      levantamientos: [], misLevantamientos: [], hitos: [], franjasEnComun: [],
      ...datos,
    } as unknown as PerfilPublico;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PerfilPublicoComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    componente = TestBed.createComponent(PerfilPublicoComponent).componentInstance;
  });

  // ===================== Quién levanta más =====================

  it('una diferencia grande sí señala a alguien', () => {
    const fila = { mio: marca('banca', 90, 5, 105), suyo: marca('banca', 30, 8, 38) };
    expect(componente.quienMas(fila)).toBe('mio');
  });

  it('y en el otro sentido también', () => {
    const fila = { mio: marca('banca', 30, 8, 38), suyo: marca('banca', 90, 5, 105) };
    expect(componente.quienMas(fila)).toBe('suyo');
  });

  it('por debajo del 10 % no hay un mas fuerte: levantais lo mismo', () => {
    // 105 y 110 es un 95 %: son la pareja que sí puede cubrirse, y destacar a
    // uno diría lo contrario de lo que la tabla existe para decir.
    const fila = { mio: marca('banca', 90, 5, 105), suyo: marca('banca', 100, 3, 110) };
    expect(componente.quienMas(fila)).toBeNull();
  });

  it('el umbral está exactamente en el 10 %', () => {
    // 90 sobre 100 es justo el limite y todavia cuenta como "lo mismo"
    expect(componente.quienMas({ mio: marca('x', 90, 1, 90), suyo: marca('x', 100, 1, 100) })).toBeNull();
    // 89 ya no
    expect(componente.quienMas({ mio: marca('x', 89, 1, 89), suyo: marca('x', 100, 1, 100) })).toBe('suyo');
  });

  it('sin marca tuya no se compara nada', () => {
    expect(componente.quienMas({ mio: null, suyo: marca('banca', 100, 3, 110) })).toBeNull();
  });

  it('un maximo a cero no se convierte en una division entre cero', () => {
    expect(componente.quienMas({ mio: marca('x', 0, 1, 0), suyo: marca('x', 0, 1, 0) })).toBeNull();
  });

  // ===================== La comparativa =====================

  it('empareja las marcas por ejercicio', () => {
    componente.perfil.set(perfil({
      levantamientos: [marca('banca', 100, 3, 110)],
      misLevantamientos: [marca('banca', 90, 5, 105)],
    }));

    const filas = componente.comparativa();
    expect(filas.length).toBe(1);
    expect(filas[0].mio?.maximoEstimado).toBe(105);
    expect(filas[0].suyo.maximoEstimado).toBe(110);
  });

  it('delante lo que haceis los dos, que es lo unico comparable', () => {
    componente.perfil.set(perfil({
      levantamientos: [marca('remo', 70, 8, 89), marca('banca', 100, 3, 110)],
      misLevantamientos: [marca('banca', 90, 5, 105)],
    }));

    expect(componente.comparativa().map(f => f.suyo.ejercicio)).toEqual(['banca', 'remo']);
    expect(componente.ejerciciosEnComun()).toBe(1);
  });

  it('un ejercicio suyo que tu no haces sale, pero sin marca tuya', () => {
    componente.perfil.set(perfil({
      levantamientos: [marca('remo', 70, 8, 89)],
      misLevantamientos: [],
    }));

    const filas = componente.comparativa();
    expect(filas.length).toBe(1);
    // Sale con una raya en tu columna: decirlo es mas honesto que dejarla vacia.
    expect(filas[0].mio).toBeNull();
    expect(componente.ejerciciosEnComun()).toBe(0);
  });

  it('los tuyos que el no hace no salen: esta es su pagina', () => {
    componente.perfil.set(perfil({
      levantamientos: [marca('banca', 100, 3, 110)],
      misLevantamientos: [marca('banca', 90, 5, 105), marca('sentadilla', 130, 3, 143)],
    }));

    expect(componente.comparativa().map(f => f.suyo.ejercicio)).toEqual(['banca']);
  });

  it('en tu propio perfil no hay nada que comparar', () => {
    componente.perfil.set(perfil({
      esMio: true,
      levantamientos: [marca('banca', 90, 5, 105)],
      misLevantamientos: [marca('banca', 90, 5, 105)],
    }));

    // Sin esto, la tabla se compararia consigo misma y saldrian dos columnas
    // identicas diciendo que levantas lo mismo que tu.
    expect(componente.comparativa()).toEqual([]);
  });

  // ===================== La escala =====================

  it('el tramo usa los mismos umbrales que el resto de la aplicacion', () => {
    expect(componente.tramo(70)).toBe('alta');
    expect(componente.tramo(69)).toBe('media');
    expect(componente.tramo(39)).toBe('baja');
  });
});
