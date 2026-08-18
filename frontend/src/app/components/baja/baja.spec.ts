import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

import { BajaComponent } from './baja';
import { api } from '../../config/api';
import { IdiomaService } from '../../services/idioma.service';

/**
 * La salida de los avisos por correo.
 *
 * <p>Lo que se protege aquí es que abrir la pantalla no dé de baja a nadie. Los
 * antivirus de correo y los previsualizadores de las bandejas siguen los
 * enlaces por su cuenta: si bastara con abrirlo, media lista se daría de baja
 * sola y en el registro se vería gente dándose de baja con toda normalidad.
 */
describe('BajaComponent', () => {

  let fixture: ComponentFixture<BajaComponent>;
  let http: HttpTestingController;

  function montar(token: string | null): void {
    TestBed.configureTestingModule({
      imports: [BajaComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(token ? { t: token } : {}) } },
        },
      ],
    });
    http = TestBed.inject(HttpTestingController);

    // En español y dicho a propósito: lo que se afirma abajo son frases, y sin
    // fijarlo la pantalla sale en el idioma del entorno, que en jsdom es inglés.
    TestBed.inject(IdiomaService).cambiar('es');

    fixture = TestBed.createComponent(BajaComponent);
    fixture.detectChanges();
  }

  function texto(): string {
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  function pulsar(etiqueta: string): void {
    // Refrescar primero: tras responder una petición el DOM aún no se ha
    // repintado, y el botón que se busca puede ser justo el que acaba de salir.
    fixture.detectChanges();
    const boton = [...(fixture.nativeElement as HTMLElement).querySelectorAll('button')]
      .find(b => (b.textContent ?? '').includes(etiqueta));
    if (!boton) throw new Error(`No está el botón «${etiqueta}»: ${texto()}`);
    boton.click();
  }

  afterEach(() => http.verify());

  it('abrirla no da de baja a nadie: solo pregunta', () => {
    montar('LLAVE');

    // Si esto llamara al backend al cargar, un escáner de correo daría de baja
    // a quien nunca pulsó nada. http.verify() en afterEach lo confirma.
    expect(texto()).toContain('¿Dejar de recibir avisos por correo?');
  });

  it('al pulsar se manda la llave del enlace', () => {
    montar('LLAVE-DEL-CORREO');
    pulsar('dejar de recibirlos');

    const peticion = http.expectOne(api('/api/avisos/baja'));
    expect(peticion.request.body).toEqual({ token: 'LLAVE-DEL-CORREO' });
    peticion.flush({ avisosPorCorreo: false });

    expect(texto()).toContain('no te escribimos más');
  });

  it('desde la baja se puede volver sin buscar dónde', () => {
    montar('LLAVE');
    pulsar('dejar de recibirlos');
    http.expectOne(api('/api/avisos/baja')).flush({ avisosPorCorreo: false });

    // Una baja de la que hay que buscar cómo salir convierte un clic mal dado
    // en un problema.
    pulsar('Me he equivocado');
    http.expectOne(api('/api/avisos/alta')).flush({ avisosPorCorreo: true });

    expect(texto()).toContain('Vuelves a recibirlos');
  });

  it('una llave que ya no vale se dice, no se finge que ha ido bien', () => {
    montar('VIEJA');
    pulsar('dejar de recibirlos');
    http.expectOne(api('/api/avisos/baja')).flush({}, { status: 404, statusText: 'Not Found' });

    expect(texto()).toContain('Ese enlace ya no vale');
    expect(texto()).not.toContain('no te escribimos más');
  });

  it('sin token en la url se explica, en vez de fallar al pulsar', () => {
    montar(null);

    expect(texto()).toContain('no está completo');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('button').length).toBe(0);
  });
});
