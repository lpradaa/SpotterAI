import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { FichaSugerenciaComponent } from './ficha-sugerencia';
import { api } from '../../config/api';
import { Match } from '../../services/usuario.service';
import { IdiomaService } from '../../services/idioma.service';

/**
 * Fichas de una en una, con su explicación pedida al servidor.
 *
 * Lo que más importa aquí no es la navegación sino la carrera: la explicación
 * llega tarde, y si para entonces ya se ha pasado a otra persona, pintarla
 * significa atribuirle a alguien un texto que habla de otro. Es un fallo que no
 * se ve probando a mano —en local la respuesta llega antes de que dé tiempo a
 * pulsar— y que en una conexión lenta pasa constantemente.
 */
describe('FichaSugerenciaComponent', () => {

  let fixture: ComponentFixture<FichaSugerenciaComponent>;
  let componente: FichaSugerenciaComponent;
  let http: HttpTestingController;
  let idioma: IdiomaService;

  function candidato(id: number, nombre: string): Match {
    return {
      id, nombre, compatibilidad: 80, etiquetaCompatibilidad: 'Buena compatibilidad',
      resumenCompatibilidad: 'Coincidís los lunes', avatar: 'ascua'
    } as unknown as Match;
  }

  const CANDIDATOS = [candidato(1, 'Marta'), candidato(2, 'Javi'), candidato(3, 'Noa')];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FichaSugerenciaComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    // En español y dicho a propósito: lo que se afirma más abajo son frases, y
    // sin fijarlo la ficha sale en el idioma del entorno, que en jsdom es inglés.
    idioma = TestBed.inject(IdiomaService);
    idioma.cambiar('es');

    fixture = TestBed.createComponent(FichaSugerenciaComponent);
    componente = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);

    fixture.componentRef.setInput('candidatos', CANDIDATOS);
    fixture.componentRef.setInput('misFranjas', []);
    await fixture.whenStable();
  });

  afterEach(() => {
    http.verify({ ignoreCancelled: true });
  });

  function explicacionDe(id: number) {
    return http.expectOne(api(`/api/usuarios/matches/${id}/explicacion`));
  }

  it('empieza por el primero y pide su explicación', () => {
    expect(componente.actual()?.nombre).toBe('Marta');
    explicacionDe(1).flush({ titular: 'Marta - 80%', motivo: 'Coincidís los lunes' });

    expect(componente.explicacion()?.titular).toBe('Marta - 80%');
    expect(componente.cargandoExplicacion()).toBe(false);
  });

  it('avanzar y retroceder cambian de persona y piden su explicación', async () => {
    explicacionDe(1).flush({ titular: 'Marta', motivo: '' });

    componente.siguiente();
    await fixture.whenStable();
    expect(componente.actual()?.nombre).toBe('Javi');
    explicacionDe(2).flush({ titular: 'Javi', motivo: '' });
    expect(componente.explicacion()?.titular).toBe('Javi');

    componente.anterior();
    await fixture.whenStable();
    expect(componente.actual()?.nombre).toBe('Marta');
    explicacionDe(1).flush({ titular: 'Marta otra vez', motivo: '' });
    expect(componente.explicacion()?.titular).toBe('Marta otra vez');
  });

  it('no se sale de la lista por ninguno de los dos extremos', async () => {
    explicacionDe(1).flush({ titular: 'Marta', motivo: '' });

    componente.anterior();
    await fixture.whenStable();
    expect(componente.indiceActual()).toBe(0);

    for (let i = 0; i < 5; i++) {
      componente.siguiente();
      await fixture.whenStable();
      // Cada salto real pide explicación; los que no se mueven, no.
      const pendientes = http.match(() => true);
      pendientes.forEach(p => p.flush({ titular: '', motivo: '' }));
    }

    expect(componente.indiceActual()).toBe(CANDIDATOS.length - 1);
  });

  it('una explicación que llega tarde no se pinta sobre otra persona', async () => {
    const deMarta = explicacionDe(1);

    componente.siguiente();
    await fixture.whenStable();
    const deJavi = explicacionDe(2);

    // Javi responde primero, Marta después: el orden que provoca el fallo.
    deJavi.flush({ titular: 'Javi - 80%', motivo: 'Lo suyo' });
    deMarta.flush({ titular: 'Marta - 80%', motivo: 'Lo de Marta' });

    expect(componente.actual()?.nombre).toBe('Javi');
    expect(componente.explicacion()?.titular).toBe('Javi - 80%');
  });

  it('si la explicación falla, deja de cargar y no se queda colgada', () => {
    explicacionDe(1).flush('boom', { status: 500, statusText: 'Error' });

    expect(componente.cargandoExplicacion()).toBe(false);
    // Sin explicación, la plantilla cae al resumen que ya trae el candidato.
    expect(componente.explicacion()).toBeNull();
  });

  it('conectar emite el id de quien se está mirando, no el primero', async () => {
    explicacionDe(1).flush({ titular: '', motivo: '' });

    componente.siguiente();
    await fixture.whenStable();
    explicacionDe(2).flush({ titular: '', motivo: '' });

    let emitido: number | null = null;
    componente.conectar.subscribe(id => emitido = id);
    componente.conectarConActual();

    expect(emitido).toBe(2);
  });

  it('los tramos de compatibilidad parten donde dice el diseño', () => {
    explicacionDe(1).flush({ titular: '', motivo: '' });

    expect(componente.tramo(70)).toBe('alta');
    expect(componente.tramo(69)).toBe('media');
    expect(componente.tramo(40)).toBe('media');
    expect(componente.tramo(39)).toBe('baja');
  });

  it('el solape se dice en horas y minutos, no en minutos sueltos', () => {
    explicacionDe(1).flush({ titular: '', motivo: '' });

    expect(componente.formatearSolape(0)).toBe('');
    expect(componente.formatearSolape(45)).toBe('45 min');
    expect(componente.formatearSolape(60)).toBe('1 hora');
    expect(componente.formatearSolape(120)).toBe('2 horas');
    expect(componente.formatearSolape(270)).toBe('4h 30min');
  });

  it('y también en el otro idioma, con su singular', () => {
    explicacionDe(1).flush({ titular: '', motivo: '' });
    idioma.cambiar('en');

    expect(componente.formatearSolape(45)).toBe('45 min');
    expect(componente.formatearSolape(60)).toBe('1 hour');
    expect(componente.formatearSolape(120)).toBe('2 hours');
    expect(componente.formatearSolape(270)).toBe('4h 30min');
  });

  /**
   * La ficha la abre un botón de explorar, que ya estaba traducida: hasta ahora
   * pulsarlo era pasar de una pantalla en inglés a un panel en español.
   */
  it('la ficha entera cambia de idioma', () => {
    explicacionDe(1).flush({ titular: '', motivo: 'Coincidís los lunes' });
    fixture.detectChanges();

    const texto = () => (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(texto()).toContain('Vuestra semana');
    expect(texto()).toContain('Conectar con Marta');

    idioma.cambiar('en');
    fixture.detectChanges();

    expect(texto()).toContain('Your week together');
    expect(texto()).toContain('Connect with Marta');
    expect(texto()).toContain('See where it comes from');
  });
});
