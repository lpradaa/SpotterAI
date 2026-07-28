import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { firstValueFrom, isObservable, Observable } from 'rxjs';

import { perfilGuard } from './perfil.guard';
import { PerfilEstadoService } from '../services/perfil-estado.service';
import { api } from '../config/api';

/**
 * Sin lo mínimo no se pasa.
 *
 * Qué es "lo mínimo" lo decide el backend y viaja en `perfilMinimo.queFalta`;
 * aquí solo se comprueba que el guardián lo respete. La decisión que conviene
 * fijar es la que no es obvia: ante un error de red **deja pasar**. Encerrar a
 * todo el mundo en la pantalla de bienvenida porque el backend ha parpadeado es
 * peor que colar a alguien que sí tenía el perfil completo.
 */
describe('perfilGuard', () => {

  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])]
    });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => http.verify());

  /** Ejecuta el guardián en su contexto de inyección y resuelve su respuesta. */
  function ejecutar(): Promise<boolean | UrlTree> {
    const salida = TestBed.runInInjectionContext(
      () => perfilGuard(null as any, null as any)) as any;
    return isObservable(salida)
      ? firstValueFrom(salida as Observable<boolean | UrlTree>)
      : Promise.resolve(salida);
  }

  it('con el perfil mínimo completo, deja pasar', async () => {
    const respuesta = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({ perfilMinimo: { queFalta: [] } });

    expect(await respuesta).toBe(true);
  });

  it('si falta algo del mínimo, manda a la bienvenida', async () => {
    const respuesta = ejecutar();
    http.expectOne(api('/api/usuarios/perfil'))
        .flush({ perfilMinimo: { queFalta: ['rutina', 'edad'] } });

    const salida = await respuesta;
    expect(salida instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(salida as UrlTree)).toBe('/bienvenida');
  });

  it('basta con que falte una cosa', async () => {
    const respuesta = ejecutar();
    http.expectOne(api('/api/usuarios/perfil'))
        .flush({ perfilMinimo: { queFalta: ['horarios'] } });

    expect(await respuesta instanceof UrlTree).toBe(true);
  });

  it('si el perfil no trae el mínimo, se deja pasar en vez de encerrar', async () => {
    const respuesta = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({});

    // Un backend viejo, o una respuesta a medias, no deberían bloquear a nadie:
    // el mismo criterio que con el error de red.
    expect(await respuesta).toBe(true);
  });

  it('ante un error de red deja pasar en vez de encerrar a nadie', async () => {
    const respuesta = ejecutar();
    http.expectOne(api('/api/usuarios/perfil'))
        .flush('caido', { status: 500, statusText: 'Error' });

    expect(await respuesta).toBe(true);
  });

  it('solo pregunta la primera vez: la respuesta se recuerda', async () => {
    const primera = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({ perfilMinimo: { queFalta: [] } });
    expect(await primera).toBe(true);

    // Sin memoria, cada navegación cargaría el perfil entero otra vez.
    expect(await ejecutar()).toBe(true);
    http.expectNone(api('/api/usuarios/perfil'));
  });

  it('tras guardar el perfil se vuelve a preguntar', async () => {
    const primera = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({ perfilMinimo: { queFalta: [] } });
    await primera;

    // Guardar puede haber borrado todas las franjas, así que lo que sabíamos ya
    // no vale.
    TestBed.inject(PerfilEstadoService).olvidar();

    const segunda = ejecutar();
    http.expectOne(api('/api/usuarios/perfil'))
        .flush({ perfilMinimo: { queFalta: ['horarios'] } });

    expect(await segunda instanceof UrlTree).toBe(true);
  });
});
