import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { Header } from './header';
import { IdiomaService } from '../../services/idioma.service';
import { api } from '../../config/api';

describe('Header', () => {
  let component: Header;
  let fixture: ComponentFixture<Header>;
  let http: HttpTestingController;
  let idioma: IdiomaService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
      // La cabecera lleva enlaces de router y pregunta por los avisos: sin
      // estos proveedores ni siquiera se puede construir.
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    })
    .compileComponents();

    http = TestBed.inject(HttpTestingController);
    idioma = TestBed.inject(IdiomaService);
    idioma.cambiar('es');

    fixture = TestBed.createComponent(Header);
    component = fixture.componentInstance;
    await fixture.whenStable();

    // Lo que la cabecera pide al construirse (avisos, sin leer…) no es lo que
    // se prueba aquí: se descarta para que no ensucie las afirmaciones.
    http.match(() => true).forEach(p => p.flush({}));
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // ===================== El selector de idioma =====================

  /**
   * El botón hace dos cosas y las dos importan: cambia la pantalla —eso se ve—
   * y deja apuntado en qué idioma escribirle por correo, que no se ve en ningún
   * sitio hasta que llega un correo en el idioma equivocado.
   *
   * <p>Los correos se mandan desde un barrido que corre solo, sin ninguna
   * petición de la que sacar `Accept-Language`, así que ese dato tiene que
   * quedar guardado. Ver `docs/i18n.md`.
   */
  describe('el selector de idioma', () => {

    function peticionDeIdioma() {
      return http.expectOne(r => r.url === api('/api/usuarios/idioma') && r.method === 'PUT');
    }

    it('cambia la pantalla y avisa al servidor', () => {
      component.alternarIdioma();

      expect(idioma.idioma()).toBe('en');
      expect(peticionDeIdioma().request.body).toEqual({ idioma: 'en' });
    });

    it('y de vuelta', () => {
      component.alternarIdioma();
      peticionDeIdioma().flush(null);

      component.alternarIdioma();

      expect(idioma.idioma()).toBe('es');
      expect(peticionDeIdioma().request.body).toEqual({ idioma: 'es' });
    });

    /**
     * Si el servidor falla, la pantalla ya ha cambiado y no se dice nada: la
     * persona pulsó para cambiar el idioma y el idioma ha cambiado. Un aviso de
     * error aquí interrumpiría por algo que no puede arreglar.
     */
    it('un fallo al guardar no rompe nada ni se le cuenta a nadie', () => {
      component.alternarIdioma();
      peticionDeIdioma().flush('boom', { status: 500, statusText: 'Error' });

      expect(idioma.idioma()).toBe('en');
    });
  });
});