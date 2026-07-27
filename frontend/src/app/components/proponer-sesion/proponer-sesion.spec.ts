import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { ProponerSesionComponent } from './proponer-sesion';
import { api } from '../../config/api';

/**
 * El formulario que trae el día y la hora ya puestos.
 *
 * Lo que se protege aquí es que la sugerencia del servidor llegue intacta al
 * formulario y de ahí a la propuesta. Si por el camino se pierde o se recorta
 * mal, lo que pasa no es un error visible: es que se propone una hora distinta
 * de la que pone en pantalla.
 */
describe('ProponerSesionComponent', () => {

  let fixture: ComponentFixture<ProponerSesionComponent>;
  let componente: ProponerSesionComponent;
  let http: HttpTestingController;

  const CON_HUECO = {
    hayFranjas: true,
    fecha: '2026-08-03',
    // El servidor manda las horas con segundos; un plan no los necesita.
    horaInicio: '18:00:00',
    horaFin: '20:00:00',
    ambosFijos: true,
    gimnasioNombre: 'McFit'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ProponerSesionComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();

    fixture = TestBed.createComponent(ProponerSesionComponent);
    componente = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);

    fixture.componentRef.setInput('paraId', 7);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function sugerencia() {
    return http.expectOne(api('/api/sesiones/sugerencia/7'));
  }

  it('se rellena con el hueco que manda el servidor, sin los segundos', () => {
    sugerencia().flush(CON_HUECO);

    expect(componente.formulario.fecha).toBe('2026-08-03');
    expect(componente.formulario.horaInicio).toBe('18:00');
    expect(componente.formulario.horaFin).toBe('20:00');
    expect(componente.completo).toBe(true);
  });

  it('sin franjas en común no se inventa una hora', () => {
    sugerencia().flush({
      hayFranjas: false, fecha: null, horaInicio: null, horaFin: null,
      ambosFijos: false, gimnasioNombre: null
    });

    // Se deja vacío y la pista de arriba explica por qué. Poner una hora al azar
    // sería peor que no poner ninguna: parecería un dato calculado.
    expect(componente.formulario.fecha).toBe('');
    expect(componente.completo).toBe(false);
  });

  it('manda exactamente lo que hay en el formulario', () => {
    sugerencia().flush(CON_HUECO);
    componente.formulario.nota = 'Empiezo por pierna';

    componente.enviar();

    const peticion = http.expectOne(api('/api/sesiones/proponer/7'));
    expect(peticion.request.method).toBe('POST');
    expect(peticion.request.body).toEqual({
      fecha: '2026-08-03', horaInicio: '18:00', horaFin: '20:00', nota: 'Empiezo por pierna'
    });
    peticion.flush({ id: 1, estado: 'PROPUESTA' });
  });

  it('una nota vacía viaja como null, no como cadena vacía', () => {
    sugerencia().flush(CON_HUECO);

    componente.enviar();

    const peticion = http.expectOne(api('/api/sesiones/proponer/7'));
    expect(peticion.request.body.nota).toBeNull();
    peticion.flush({ id: 1, estado: 'PROPUESTA' });
  });

  it('emite la sesión creada para que quien lo abrió la pinte', () => {
    sugerencia().flush(CON_HUECO);

    let emitida: any = null;
    componente.propuesta.subscribe(s => emitida = s);

    componente.enviar();
    http.expectOne(api('/api/sesiones/proponer/7')).flush({ id: 42, estado: 'PROPUESTA' });

    expect(emitida.id).toBe(42);
  });

  it('enseña el motivo que da el servidor, no uno genérico', () => {
    sugerencia().flush(CON_HUECO);

    componente.enviar();
    http.expectOne(api('/api/sesiones/proponer/7')).flush(
      { error: 'Ya hay una propuesta en marcha con esta persona.' },
      { status: 400, statusText: 'Bad Request' });

    // Las reglas viven en el servidor; repetirlas aquí sería mantener dos
    // versiones de lo mismo y que una acabe mintiendo.
    expect(componente.error()).toBe('Ya hay una propuesta en marcha con esta persona.');
    expect(componente.enviando()).toBe(false);
  });

  it('sin los tres campos no se manda nada', () => {
    sugerencia().flush(CON_HUECO);
    componente.formulario.horaFin = '';

    expect(componente.completo).toBe(false);
    componente.enviar();

    http.expectNone(api('/api/sesiones/proponer/7'));
  });

  it('no se manda dos veces por pulsar dos veces', () => {
    sugerencia().flush(CON_HUECO);

    componente.enviar();
    componente.enviar();

    // Una sola: la segunda pulsación mientras la primera está en vuelo crearía
    // dos propuestas, y la segunda se estrellaría contra la regla de "solo una
    // viva" con un error que la persona no entendería.
    const peticiones = http.match(api('/api/sesiones/proponer/7'));
    expect(peticiones.length).toBe(1);
    peticiones[0].flush({ id: 1, estado: 'PROPUESTA' });
  });

  it('si la sugerencia falla, el formulario sigue siendo usable a mano', () => {
    sugerencia().flush('boom', { status: 500, statusText: 'Error' });

    expect(componente.sugerencia()).toBeNull();
    componente.formulario = {
      fecha: '2026-08-05', horaInicio: '19:00', horaFin: '20:30', nota: ''
    };
    expect(componente.completo).toBe(true);
  });
});
