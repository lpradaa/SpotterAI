import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Franja, RejillaSemana } from './rejilla-semana';
import { IdiomaService } from '../../services/idioma.service';

/**
 * La rejilla dice cuándo podéis entrenar juntos usando color y posición, así
 * que lleva al lado el mismo contenido en palabras: sin él, la pieza más
 * importante de la aplicación es muda para quien no ve.
 *
 * <p>Y ese texto no se ve en pantalla, que es justo el problema: se puede
 * quedar en español, o vacío, o describiendo el día equivocado, sin que nadie
 * lo note mirando. De ahí que se pruebe aquí.
 */
describe('RejillaSemana', () => {
  let fixture: ComponentFixture<RejillaSemana>;
  let idioma: IdiomaService;

  function franja(dia: string, desde: string, hasta: string, fijo = false): Franja {
    return { diaSemana: dia, horaInicio: desde, horaFin: hasta, habitual: fijo, ambosFijos: fijo };
  }

  /** El equivalente en texto, que es lo que oye un lector de pantalla. */
  function dicho(): string {
    fixture.detectChanges();
    return fixture.nativeElement.querySelector('.visually-hidden')?.textContent ?? '';
  }

  function cabeceras(): string[] {
    fixture.detectChanges();
    return Array.from(fixture.nativeElement.querySelectorAll('.rejilla__dia'))
      .map(e => (e as HTMLElement).textContent!.trim());
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [RejillaSemana] }).compileComponents();

    // En español y dicho a propósito: jsdom dice que el navegador está en
    // inglés, y lo que se afirma aquí abajo son frases.
    idioma = TestBed.inject(IdiomaService);
    idioma.cambiar('es');

    fixture = TestBed.createComponent(RejillaSemana);
  });

  it('sin nada que cruzar lo dice y no dibuja', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Sin horarios que cruzar todavía');
    expect(fixture.nativeElement.querySelector('.rejilla')).toBeNull();
  });

  it('cuenta las franjas en las que coincidís', () => {
    fixture.componentRef.setInput('misFranjas', [franja('Lunes', '18:00', '20:00')]);
    fixture.componentRef.setInput('solape', [franja('Lunes', '18:00', '20:00')]);

    expect(dicho()).toContain('Coincidís en una franja de la semana.');

    fixture.componentRef.setInput('solape', [
      franja('Lunes', '18:00', '20:00'), franja('Miércoles', '18:00', '20:00')]);

    expect(dicho()).toContain('Coincidís en 2 franjas de la semana.');
  });

  /**
   * Tu semana entera sin un solo cruce no es lo mismo que no tener horarios: la
   * primera es una respuesta —esta persona no te viene bien— y la segunda es
   * que todavía no hay nada que comparar.
   */
  it('tener semana y no coincidir no es lo mismo que no tener nada', () => {
    fixture.componentRef.setInput('misFranjas', [franja('Lunes', '18:00', '20:00')]);

    expect(dicho()).toContain('No coincidís en ninguna franja');
    expect(dicho()).not.toContain('Sin horarios que cruzar');
  });

  it('cada tramo dice su día, su hora y qué significa', () => {
    fixture.componentRef.setInput('solape', [
      franja('Lunes', '18:00', '20:00', true),
      franja('Miércoles', '19:00', '21:00'),
    ]);

    expect(dicho()).toContain('Lunes de 18:00 a 20:00: coincidís y los dos vais siempre');
    expect(dicho()).toContain('Miércoles de 19:00 a 21:00: coincidís');
  });

  it('el tramo entero cambia de idioma, día incluido', () => {
    fixture.componentRef.setInput('solape', [franja('Miércoles', '19:00', '21:00')]);

    idioma.cambiar('en');

    // El dia guardado sigue siendo "Miércoles"; lo que cambia es como se lee.
    expect(dicho()).toContain('Wednesday from 19:00 to 21:00: you overlap');
    expect(dicho()).not.toContain('Miércoles');
  });

  /**
   * En compacta la fila entera tiene que caber en una tarjeta de lista. Una
   * letra vale en español; en inglés dejaría martes y jueves —y sábado y
   * domingo— escritos igual, así que van dos.
   */
  it('abrevia las cabeceras según el ancho y el idioma', () => {
    fixture.componentRef.setInput('misFranjas', [franja('Lunes', '18:00', '20:00')]);
    fixture.componentRef.setInput('modo', 'compacta');

    expect(cabeceras()).toEqual(['L', 'M', 'X', 'J', 'V', 'S', 'D']);

    idioma.cambiar('en');
    expect(cabeceras()).toEqual(['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su']);

    fixture.componentRef.setInput('modo', 'completa');
    expect(cabeceras()).toEqual(['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']);
  });

  /**
   * Un día que no reconocemos no se dibuja, pero tampoco se cuela en la columna
   * equivocada: la rejilla enseñaría un solape que no existe, y decir cuándo
   * coincidís es lo único que hace.
   */
  it('un día desconocido no acaba en la columna del domingo', () => {
    fixture.componentRef.setInput('misFranjas', [franja('Lunnes', '18:00', '20:00')]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('.bloque').length).toBe(0);
  });
});
