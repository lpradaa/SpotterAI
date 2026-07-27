import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { firstValueFrom, isObservable, Observable } from 'rxjs';

import { horarioGuard } from './horario.guard';
import { PerfilEstadoService } from '../services/perfil-estado.service';
import { api } from '../config/api';

/**
 * Sin horario no se pasa.
 *
 * El horario vale el 40 % de la compatibilidad, así que un perfil sin él no se
 * puede emparejar con nadie: solo ocupa sitio en la lista. Pero el guardián
 * tiene una decisión que no es obvia y conviene fijar: ante un error de red deja
 * pasar. Encerrar a todo el mundo en la pantalla de bienvenida porque el backend
 * ha parpadeado es peor que colar a alguien que sí tenía horario.
 */
describe('horarioGuard', () => {

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
      () => horarioGuard(null as any, null as any)) as any;
    return isObservable(salida)
      ? firstValueFrom(salida as Observable<boolean | UrlTree>)
      : Promise.resolve(salida);
  }

  it('con horario, deja pasar', async () => {
    const respuesta = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({
      horarios: [{ diaSemana: 'Lunes', horaInicio: '18:00', horaFin: '20:00' }]
    });

    expect(await respuesta).toBe(true);
  });

  it('sin horario, manda a la bienvenida', async () => {
    const respuesta = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({ horarios: [] });

    const salida = await respuesta;
    expect(salida instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(salida as UrlTree)).toBe('/bienvenida');
  });

  it('si el perfil no trae horarios, cuenta como que no los tiene', async () => {
    const respuesta = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({});

    expect(await respuesta instanceof UrlTree).toBe(true);
  });

  it('ante un error de red deja pasar en vez de encerrar a nadie', async () => {
    const respuesta = ejecutar();
    http.expectOne(api('/api/usuarios/perfil'))
        .flush('caido', { status: 500, statusText: 'Error' });

    expect(await respuesta).toBe(true);
  });

  it('solo pregunta la primera vez: la respuesta se recuerda', async () => {
    const primera = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({ horarios: [{ diaSemana: 'Lunes' }] });
    expect(await primera).toBe(true);

    // Sin memoria, cada navegación cargaría el perfil entero otra vez.
    expect(await ejecutar()).toBe(true);
    http.expectNone(api('/api/usuarios/perfil'));
  });

  it('tras guardar el perfil se vuelve a preguntar', async () => {
    const primera = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({ horarios: [{ diaSemana: 'Lunes' }] });
    await primera;

    // Guardar puede haber borrado todas las franjas, así que lo que sabíamos ya
    // no vale.
    TestBed.inject(PerfilEstadoService).olvidar();

    const segunda = ejecutar();
    http.expectOne(api('/api/usuarios/perfil')).flush({ horarios: [] });

    expect(await segunda instanceof UrlTree).toBe(true);
  });
});
