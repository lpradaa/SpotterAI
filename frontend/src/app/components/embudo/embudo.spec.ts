import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { EmbudoComponent, Embudo, FilaEmbudo } from './embudo';
import { api } from '../../config/api';
import { IdiomaService } from '../../services/idioma.service';

/**
 * La pantalla no saca conclusiones por su cuenta.
 *
 * Quién decide si hay muestra suficiente es el backend, y aquí solo se pinta lo
 * que mande. Es la parte que hay que proteger: en cuanto esta plantilla calcule
 * un porcentaje —o decida que 12 casos «ya casi»— habrá dos sitios decidiendo
 * lo mismo, y este proyecto ya se ha comido tres veces el mismo número saliendo
 * distinto por dos caminos.
 */
describe('EmbudoComponent', () => {

  let fixture: ComponentFixture<EmbudoComponent>;
  let http: HttpTestingController;

  function fila(parcial: Partial<FilaEmbudo>): FilaEmbudo {
    return {
      tramo: 'ALTA', enviadas: 0, aceptadas: 0, entrenadas: 0,
      hayBastante: false, tasaAceptacion: null, tasaEntrenamiento: null,
      ...parcial,
    };
  }

  async function pintar(embudo: Embudo): Promise<string> {
    fixture = TestBed.createComponent(EmbudoComponent);
    fixture.detectChanges();
    http.expectOne(api('/api/metricas/embudo')).flush(embudo);
    await fixture.whenStable();
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  /**
   * Solo el cuerpo de la tabla.
   *
   * <p>El texto de la pantalla lleva porcentajes suyos —los nombres de los
   * tramos son "Alta (70 % o más)"— asi que buscar "%" en toda la pagina no
   * distingue el rotulo de una tasa calculada, que es lo unico que se quiere
   * comprobar aqui.
   */
  function celdas(): string {
    return (fixture.nativeElement as HTMLElement)
      .querySelectorAll('tbody td').length === 0
      ? ''
      : [...(fixture.nativeElement as HTMLElement).querySelectorAll('tbody td')]
          .map(c => c.textContent ?? '').join(' | ');
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EmbudoComponent],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);

    // En español y dicho a propósito: lo que se afirma abajo son frases, y sin
    // fijarlo la pantalla sale en el idioma del entorno, que en jsdom es inglés.
    TestBed.inject(IdiomaService).cambiar('es');
  });

  afterEach(() => http.verify());

  it('sin muestra enseña el recuento pero ningún porcentaje', async () => {
    const texto = await pintar({
      filas: [fila({ tramo: 'ALTA', enviadas: 4, aceptadas: 2 })],
      sinPuntuacion: 0,
      sePuedeComparar: false,
    });

    expect(celdas()).toContain('4');
    // Un "50 %" sobre cuatro casos es la forma mas rapida de confirmar la
    // hipotesis que uno ya traia puesta al abrir la pantalla.
    expect(celdas()).not.toContain('%');
    expect(texto).toContain('Todavía no se puede concluir nada');
  });

  it('con muestra enseña las tasas que manda el backend, sin recalcularlas', async () => {
    const texto = await pintar({
      filas: [fila({
        tramo: 'ALTA', enviadas: 40, aceptadas: 30, entrenadas: 10,
        hayBastante: true, tasaAceptacion: 75, tasaEntrenamiento: 25,
      })],
      sinPuntuacion: 0,
      sePuedeComparar: true,
    });

    expect(celdas()).toContain('75 %');
    expect(celdas()).toContain('25 %');
    expect(texto).not.toContain('Todavía no se puede concluir nada');
  });

  it('dice cuántas solicitudes quedan fuera por no tener puntuación guardada', async () => {
    const texto = await pintar({
      filas: [fila({ tramo: 'ALTA' })],
      sinPuntuacion: 40,
      sePuedeComparar: false,
    });

    expect(texto).toContain('40');
    expect(texto).toContain('sería inventarse el dato');
  });

  it('los tres tramos se pintan aunque estén a cero', async () => {
    const texto = await pintar({
      filas: [fila({ tramo: 'ALTA' }), fila({ tramo: 'MEDIA' }), fila({ tramo: 'BAJA' })],
      sinPuntuacion: 0,
      sePuedeComparar: false,
    });

    expect(texto).toContain('Alta');
    expect(texto).toContain('Media');
    // Que nadie escriba nunca a alguien de compatibilidad baja es un dato: si la
    // fila desaparece, la tabla parece decir que se escribe a todo el mundo.
    expect(texto).toContain('Baja');
  });
});
