import { ComponentFixture, TestBed } from '@angular/core/testing';

import { RejillaEditable } from './rejilla-editable';
import { Franja } from '../rejilla-semana/rejilla-semana';
import { IdiomaService } from '../../services/idioma.service';

/**
 * La rejilla de pintar es el único sitio donde el día **se escribe**.
 *
 * <p>Todo lo demás lee lo que hay guardado; esto lo produce. Por eso lo que se
 * vigila aquí no es solo que las cabeceras se traduzcan, sino que la franja que
 * sale al guardar siga en español pase lo que pase: es lo que cruza tu semana
 * con la de otra persona, y un «Monday» no cruzaría con nada.
 */
describe('RejillaEditable', () => {
  let fixture: ComponentFixture<RejillaEditable>;
  let component: RejillaEditable;
  let idioma: IdiomaService;

  /** Las celdas de un día, en el orden en que se pintan. */
  function celdas(): HTMLButtonElement[] {
    fixture.detectChanges();
    return Array.from(fixture.nativeElement.querySelectorAll('.celda'));
  }

  /** La primera celda del lunes: la rejilla empieza a las 6. */
  function lunesALasSeis(): HTMLButtonElement {
    return celdas()[0];
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [RejillaEditable] }).compileComponents();

    idioma = TestBed.inject(IdiomaService);
    idioma.cambiar('es');

    fixture = TestBed.createComponent(RejillaEditable);
    component = fixture.componentInstance;
  });

  it('cada celda dice qué se está marcando', () => {
    expect(lunesALasSeis().getAttribute('aria-label')).toBe('Lunes de 6 a 7');

    idioma.cambiar('en');
    expect(lunesALasSeis().getAttribute('aria-label')).toBe('Monday from 6 to 7');
  });

  it('las cabeceras cambian de idioma', () => {
    const titulos = () => Array.from(
      fixture.nativeElement.querySelectorAll('.editable__dia') as NodeListOf<HTMLElement>)
      .map(e => e.title);

    fixture.detectChanges();
    expect(titulos()[2]).toBe('Miércoles');

    idioma.cambiar('en');
    fixture.detectChanges();
    expect(titulos()[2]).toBe('Wednesday');
  });

  /**
   * Lo que se guarda no es lo que se lee. Este es el test que impide que
   * alguien «termine de traducir» la rejilla traduciendo la lista de días y
   * escriba inglés en la base.
   */
  it('la franja que guarda lleva el día en español, esté la pantalla como esté', () => {
    idioma.cambiar('en');

    let guardado: Franja[] = [];
    component.cambio.subscribe(f => guardado = f);

    lunesALasSeis().click();

    expect(guardado).toEqual([
      { diaSemana: 'Lunes', horaInicio: '06:00', horaFin: '07:00', habitual: false },
    ]);
  });

  it('funde las horas seguidas del mismo día en una sola franja', () => {
    let guardado: Franja[] = [];
    component.cambio.subscribe(f => guardado = f);

    const todas = celdas();
    todas[0].click();  // lunes 6
    todas[7].click();  // lunes 7: siete columnas más allá
    fixture.detectChanges();

    expect(guardado).toHaveLength(1);
    expect(guardado[0].horaInicio).toBe('06:00');
    expect(guardado[0].horaFin).toBe('08:00');
  });

  /**
   * Las franjas de partida llegan de una petición, o sea después de que exista
   * el componente. Y llegan con el día en español, que es como se guardaron.
   */
  it('siembra un horario ya guardado en su columna', () => {
    fixture.componentRef.setInput('franjasIniciales', [
      { diaSemana: 'Miércoles', horaInicio: '18:00', horaFin: '20:00' },
    ]);
    fixture.detectChanges();

    expect(component.horasPintadas()).toBe(2);
    expect(component.estaPintada(2, 18)).toBe(true);
    expect(component.estaPintada(2, 19)).toBe(true);
  });
});
