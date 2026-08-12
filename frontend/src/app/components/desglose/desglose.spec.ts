import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Desglose } from './desglose';
import { FactorDelDesglose } from '../../services/usuario.service';

/**
 * De dónde sale el número.
 *
 * Lo que se fija aquí no es que pinte barras, es el orden y la separación: el
 * desglose existe para hacer visibles las dos decisiones más caras del motor
 * —que un factor sin datos no puntúa como un cero, y que su peso se reparte
 * entre los demás—. Si los sin datos se mezclaran con los evaluados, o si el
 * orden dejara de seguir el peso, seguiría "funcionando" y dejaría de explicar
 * nada, que es lo único para lo que está.
 */
describe('Desglose', () => {

  let fixture: ComponentFixture<Desglose>;
  let componente: Desglose;

  function evaluado(nombre: string, puntos: number, puntosMax: number): FactorDelDesglose {
    return { nombre, etiqueta: nombre, puntos, puntosMax, aplicable: true, detalle: 'da igual' };
  }

  function sinDatos(nombre: string): FactorDelDesglose {
    return { nombre, etiqueta: nombre, puntos: 0, puntosMax: 0, aplicable: false,
             detalle: 'Falta el dato de alguno de los dos perfiles' };
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Desglose] }).compileComponents();
    fixture = TestBed.createComponent(Desglose);
    componente = fixture.componentInstance;
  });

  function conFactores(factores: FactorDelDesglose[]) {
    fixture.componentRef.setInput('factores', factores);
    fixture.detectChanges();
  }

  it('los evaluados van de mayor a menor peso: primero lo que más decide', () => {
    conFactores([
      evaluado('edad', 5, 5),
      evaluado('horario', 20, 40),
      evaluado('nivel', 10, 10),
    ]);

    expect(componente.evaluados().map(f => f.nombre)).toEqual(['horario', 'nivel', 'edad']);
  });

  it('los que no se han podido evaluar van aparte, no mezclados', () => {
    conFactores([evaluado('horario', 20, 40), sinDatos('fuerza'), evaluado('nivel', 10, 10)]);

    // Si "fuerza" se colara entre los evaluados saldría con una barra vacía, y
    // una barra vacía se lee como "puntuó cero", que es lo contrario de lo que pasa.
    expect(componente.evaluados().map(f => f.nombre)).toEqual(['horario', 'nivel']);
    expect(componente.sinDatos().map(f => f.nombre)).toEqual(['fuerza']);
  });

  it('sin ningún hueco no se enseña el aviso del reparto', () => {
    conFactores([evaluado('horario', 20, 40)]);
    expect(componente.hayReparto()).toBe(false);
  });

  it('con algún hueco sí, porque es lo que explica los pesos raros', () => {
    // Con factores fuera, el peso de los demás sube: "Edad 13/13" donde la tabla
    // del README dice 5. Sin decirlo, ese 13 parece un error.
    conFactores([evaluado('edad', 13, 13), sinDatos('fuerza')]);
    expect(componente.hayReparto()).toBe(true);
  });

  it('la proporción es sobre el peso del factor, no sobre el total', () => {
    // Una barra llena de un factor pequeño está llena. Medirla contra el total
    // haría que "Edad 5/5" pareciera un 5 % en vez de un lleno.
    conFactores([evaluado('edad', 5, 5), evaluado('horario', 10, 40)]);

    expect(componente.proporcion(evaluado('edad', 5, 5))).toBe(100);
    expect(componente.proporcion(evaluado('horario', 10, 40))).toBe(25);
  });

  it('un factor sin máximo no divide entre cero', () => {
    expect(componente.proporcion(sinDatos('fuerza'))).toBe(0);
  });

  it('el color de la barra sale de la proporción, no de la puntuación total', () => {
    // 5 de 5 es un lleno y se pinta como tal aunque sean pocos puntos.
    expect(componente.tramo(evaluado('edad', 5, 5))).toBe('alta');
    expect(componente.tramo(evaluado('horario', 10, 40))).toBe('baja');
  });
});
