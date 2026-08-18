import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Explore } from './explore';
import { Match } from '../../services/usuario.service';
import { IdiomaService } from '../../services/idioma.service';

/**
 * El arranque en frío.
 *
 * Lo que se protege aquí son tres situaciones que la pantalla enseñaba como si
 * fueran una: que no haya nadie todavía, que tus filtros no dejen pasar a
 * nadie, y que haya gente pero ninguna de tu gimnasio. La tercera es la que
 * más engaña, porque la lista se llena de puntuaciones bajas y no dice por qué:
 * el solape horario en otro edificio vale una cuarta parte.
 */
describe('Explore', () => {
  let component: Explore;
  let fixture: ComponentFixture<Explore>;

  function persona(id: number, mismoGimnasio: boolean): Match {
    return {
      id, nombre: `Persona ${id}`, email: `p${id}@test.com`,
      edad: 28, genero: null, peso: null, nivel: 'Intermedio',
      objetivos: 'Hipertrofia', avatar: null, fotoUrl: null, biografia: null,
      gimnasioId: mismoGimnasio ? 1 : 2,
      gimnasioNombre: mismoGimnasio ? 'McFit' : 'Basic-Fit',
      rutina: 'Torso / Pierna', fuerzaCompatible: null, mismoGimnasio,
      compatibilidad: 40, etiquetaCompatibilidad: '', resumenCompatibilidad: '',
      diasEnComun: [], minutosEnComun: 0, diasFijosEnComun: 0,
      compatibilidadIncompleta: false, franjasEnComun: [],
      yaConectado: false, solicitudPendiente: false,
    };
  }

  function texto(): string {
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  let idioma: IdiomaService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Explore],
    }).compileComponents();

    /* En español y dicho a proposito. Lo que se afirma aqui abajo son frases, y
       sin fijar el idioma la pantalla sale en el del entorno: jsdom dice ingles,
       asi que estos tests pasaban a rojo en cuanto la pantalla se tradujo. La
       preferencia se lee del navegador y no la elige el test. */
    idioma = TestBed.inject(IdiomaService);
    idioma.cambiar('es');

    fixture = TestBed.createComponent(Explore);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('sin nadie todavía no dice que fallen los filtros', () => {
    component.usuarios.set([]);

    // Eran dos situaciones distintas diciendo lo mismo. A quien acaba de entrar
    // y no hay nadie, "no hay nadie que encaje con esa combinación de filtros"
    // le dice que ha hecho algo mal.
    expect(texto()).toContain('Todavía no hay nadie');
    expect(texto()).not.toContain('combinación de filtros');
  });

  it('con gente pero ninguna de tu gimnasio, lo dice y explica los números', () => {
    component.usuarios.set([persona(1, false), persona(2, false)]);

    expect(component.soyElPrimeroDeMiGimnasio()).toBe(true);
    expect(texto()).toContain('Eres el primero de tu gimnasio');
    // Lo que hace que la banda valga algo: sin el motivo, la lista es una pared
    // de puntuaciones bajas y el usuario concluye que la aplicación no sirve.
    expect(texto()).toContain('no es coincidir');
  });

  it('en cuanto hay alguien de tu gimnasio, la banda desaparece', () => {
    component.usuarios.set([persona(1, true), persona(2, false)]);

    expect(component.enMiGimnasio()).toBe(1);
    expect(component.soyElPrimeroDeMiGimnasio()).toBe(false);
    expect(texto()).not.toContain('Eres el primero de tu gimnasio');
  });

  it('con gente pero filtrada a cero sigue hablando de filtros', () => {
    component.usuarios.set([persona(1, true)]);
    component.actualizarFiltros('busqueda', 'nadie-se-llama-asi');

    expect(texto()).toContain('combinación de filtros');
    expect(texto()).not.toContain('Todavía no hay nadie');
  });

  /**
   * Que la pantalla cambie entera, y en caliente.
   *
   * <p>Un texto fijo que se cuele en la plantilla no rompe nada visible: se ve
   * bien en español, que es como se escribe y como se mira. Aparece en inglés,
   * rodeado de lo que sí cambió, y eso no se lee como aplicación bilingüe sino
   * como aplicación rota.
   */
  it('cambia de idioma sin recargar', () => {
    component.usuarios.set([]);
    expect(texto()).toContain('Todavía no hay nadie');

    idioma.cambiar('en');
    expect(texto()).toContain('Nobody here yet');
    expect(texto()).not.toContain('Todavía no hay nadie');
  });

  /**
   * El nivel y el objetivo se guardan en español y viajan asi, que es lo que
   * compara el motor. Lo que cambia es la etiqueta.
   */
  it('traduce el nivel y el objetivo guardados sin tocar el valor', () => {
    component.usuarios.set([persona(1, true)]);
    expect(texto()).toContain('Intermedio');

    idioma.cambiar('en');
    expect(texto()).toContain('Intermediate');
    expect(texto()).toContain('Hypertrophy');

    // El desplegable filtra contra lo guardado, asi que su valor sigue en español
    const opciones = (fixture.nativeElement as HTMLElement)
      .querySelectorAll<HTMLOptionElement>('option');
    const valores = Array.from(opciones).map(o => o.value);
    expect(valores).toContain('Intermedio');
    expect(valores).toContain('Pérdida de peso');
  });
});
