import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';

import { jwtInterceptor } from './jwt.interceptor';

/**
 * Lo que va en cada petición desde que la sesión es una galleta.
 *
 * <p>Ya no hay ninguna cabecera `Authorization`: el token vive en una galleta
 * `HttpOnly` y el navegador la manda solo. Lo que sí tiene que poner este
 * interceptor es `withCredentials` —sin eso el navegador no manda galletas a
 * otro origen, que es el caso en desarrollo— y la cabecera del CSRF.
 *
 * <p>Esa cabecera es la parte que se rompe callando: sin ella, todo lo que lee
 * sigue funcionando y todo lo que escribe da 403. Es exactamente lo que pasó al
 * hacer este cambio, y el síntoma no fue «falta una cabecera», fue que se
 * entraba y la aplicación te echaba sola al login.
 */
describe('jwtInterceptor', () => {

  let http: HttpClient;
  let control: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([jwtInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    control = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    control.verify();
    // La galleta se comparte entre pruebas: sin borrarla, la primera que la
    // ponga hace pasar a las que comprueban que NO va.
    document.cookie = 'XSRF-TOKEN=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/';
  });

  it('nunca manda una cabecera Authorization', () => {
    http.get('/api/lo-que-sea').subscribe();

    const peticion = control.expectOne('/api/lo-que-sea');
    // Si esto vuelve, es que alguien ha guardado el token en JavaScript otra
    // vez, y entonces todo el cambio no ha servido de nada.
    expect(peticion.request.headers.has('Authorization')).toBe(false);
    peticion.flush({});
  });

  it('manda las galletas, también a otro origen', () => {
    http.get('/api/lo-que-sea').subscribe();

    const peticion = control.expectOne('/api/lo-que-sea');
    // En desarrollo la aplicación (:4200) y la API (:8080) son orígenes
    // distintos: sin esto el navegador no manda la galleta de sesión.
    expect(peticion.request.withCredentials).toBe(true);
    peticion.flush({});
  });

  it('copia el token del CSRF a la cabecera en lo que escribe', () => {
    document.cookie = 'XSRF-TOKEN=el-token-csrf;path=/';

    http.post('/api/algo', {}).subscribe();

    const peticion = control.expectOne('/api/algo');
    expect(peticion.request.headers.get('X-XSRF-TOKEN')).toBe('el-token-csrf');
    peticion.flush({});
  });

  it('en lo que solo lee no hace falta, y no se manda', () => {
    document.cookie = 'XSRF-TOKEN=el-token-csrf;path=/';

    http.get('/api/algo').subscribe();

    const peticion = control.expectOne('/api/algo');
    expect(peticion.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    peticion.flush({});
  });

  it('sin galleta de CSRF no inventa la cabecera', () => {
    http.post('/api/algo', {}).subscribe();

    const peticion = control.expectOne('/api/algo');
    expect(peticion.request.headers.has('X-XSRF-TOKEN')).toBe(false);
    peticion.flush({});
  });
});
