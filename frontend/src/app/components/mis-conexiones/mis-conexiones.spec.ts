import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { MisConexionesComponent } from './mis-conexiones';
import { Mensaje } from '../../services/mensajes.service';
import { IdiomaService } from '../../services/idioma.service';

/**
 * El chat tiene que decir qué día se dijo cada cosa.
 *
 * Cada burbuja llevaba solo la hora. En una aplicación cuyo asunto entero es
 * acordar cuándo entrenar, eso convierte "quedamos mañana a las 7" leído tres
 * días después en información falsa sin forma de detectarlo.
 *
 * Se prueba sobre el modelo que arma las líneas y no sobre el HTML pintado
 * porque lo que puede romperse es el agrupado: que dos días distintos caigan
 * bajo la misma marca no se ve mirando una captura.
 */
describe('MisConexionesComponent · las marcas de día', () => {

  let component: MisConexionesComponent;
  let fixture: ComponentFixture<MisConexionesComponent>;
  let idioma: IdiomaService;

  function mensaje(id: number, fecha: Date): Mensaje {
    return {
      id, emisorId: 2, emisorNombre: 'Ana', receptorId: 1,
      contenido: `Mensaje ${id}`, fechaEnvio: fecha.toISOString(), leido: true,
    };
  }

  function haceDias(dias: number, hora = 18): Date {
    const d = new Date();
    d.setDate(d.getDate() - dias);
    d.setHours(hora, 30, 0, 0);
    return d;
  }

  function etiquetas(): string[] {
    return component.lineas()
      .filter(l => l.tipo === 'dia')
      .map(l => (l as { etiqueta: string }).etiqueta);
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MisConexionesComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    // En español y dicho a propósito: «Hoy» y «Ayer» son frases, y sin fijarlo
    // salen en el idioma del entorno, que en jsdom es inglés.
    idioma = TestBed.inject(IdiomaService);
    idioma.cambiar('es');

    fixture = TestBed.createComponent(MisConexionesComponent);
    component = fixture.componentInstance;
  });

  it('sin mensajes no hay ninguna marca', () => {
    component.historial.set([]);
    expect(component.lineas()).toEqual([]);
  });

  it('varios mensajes del mismo día llevan una sola marca', () => {
    component.historial.set([
      mensaje(1, haceDias(0, 9)),
      mensaje(2, haceDias(0, 14)),
      mensaje(3, haceDias(0, 20)),
    ]);

    expect(etiquetas()).toEqual(['Hoy']);
    expect(component.lineas().length).toBe(4);
  });

  it('cada cambio de día abre una marca nueva', () => {
    component.historial.set([
      mensaje(1, haceDias(2)),
      mensaje(2, haceDias(1)),
      mensaje(3, haceDias(0)),
    ]);

    const marcas = etiquetas();
    expect(marcas.length).toBe(3);
    // Los dos que se usan al hablar, y justo los días en que un plan aún sirve.
    expect(marcas[1]).toBe('Ayer');
    expect(marcas[2]).toBe('Hoy');
  });

  it('volver al mismo día después de otro abre marca otra vez', () => {
    // No se agrupa por "día visto alguna vez" sino por tramos seguidos. Si se
    // dedujera con un Set, dos mensajes de hoy separados por uno de ayer
    // dejarían el segundo sin marca y pareceria de ayer.
    component.historial.set([
      mensaje(1, haceDias(0, 8)),
      mensaje(2, haceDias(1, 8)),
      mensaje(3, haceDias(0, 21)),
    ]);

    expect(etiquetas()).toEqual(['Hoy', 'Ayer', 'Hoy']);
  });

  it('más atrás de ayer se escribe la fecha, no solo el día de la semana', () => {
    component.historial.set([mensaje(1, haceDias(10))]);

    const marca = etiquetas()[0];
    expect(marca).not.toBe('Hoy');
    expect(marca).not.toBe('Ayer');
    // "jue" a secas vuelve a ser ambiguo en cuanto pasa una semana.
    expect(marca).toMatch(/\d/);
  });

  it('la marca va antes de su mensaje, no después', () => {
    component.historial.set([mensaje(7, haceDias(0))]);

    const [primera, segunda] = component.lineas();
    expect(primera.tipo).toBe('dia');
    expect(segunda.tipo).toBe('mensaje');
  });

  describe('el «visto»', () => {

    function mio(id: number, leido: boolean): Mensaje {
      return {
        id, emisorId: 1, emisorNombre: 'Yo', receptorId: 2,
        contenido: 'lo mío', fechaEnvio: new Date().toISOString(), leido,
      };
    }

    beforeEach(() => {
      component.chatActivo.set({ usuarioId: 2 } as any);
    });

    it('la marca va solo en el último tuyo', () => {
      component.historial.set([mio(1, true), mensaje(2, new Date()), mio(3, false)]);

      // En todos sería una columna de "Visto" repetida que no aporta nada.
      expect(component.ultimoMio()).toBe(3);
    });

    it('los suyos nunca son el último tuyo', () => {
      component.historial.set([mio(1, true), mensaje(9, new Date())]);

      expect(component.ultimoMio()).toBe(1);
    });

    it('sin mensajes tuyos no hay marca', () => {
      component.historial.set([mensaje(9, new Date())]);

      expect(component.ultimoMio()).toBeNull();
    });
  });

  /**
   * Las fechas del chat iban con `toLocaleDateString('es-ES', …)` escrito a
   * mano y la hora de cada burbuja con `| date`, que usa el LOCALE_ID fijado al
   * arrancar. Las dos cosas se quedaban en español con la pantalla en inglés, y
   * son de las que no se ven leyendo el código.
   */
  describe('las fechas siguen al idioma', () => {

    it('las marcas de día', () => {
      component.historial.set([mensaje(1, haceDias(0)), mensaje(2, haceDias(1))]);
      expect(etiquetas()).toEqual(['Hoy', 'Ayer']);

      idioma.cambiar('en');
      expect(etiquetas()).toEqual(['Today', 'Yesterday']);
    });

    it('y el día largo de un plan', () => {
      // Un lunes, para poder afirmar sobre el nombre del día.
      expect(component.diaLargo('2026-08-17')).toContain('lunes');

      idioma.cambiar('en');
      expect(component.diaLargo('2026-08-17')).toContain('Monday');
    });
  });
});
