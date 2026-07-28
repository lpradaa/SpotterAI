import { Component, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ModalAccesible } from './modal-accesible';

/**
 * Anfitrión mínimo: un botón fuera del diálogo y tres controles dentro.
 *
 * `abierto` es una señal y no un booleano suelto porque las pruebas lo abren y
 * lo cierran varias veces: con un campo normal, el doble chequeo de Angular en
 * desarrollo salta con NG0100 y el fallo parece de la directiva cuando es del
 * andamiaje.
 */
@Component({
  standalone: true,
  imports: [ModalAccesible],
  template: `
    <button id="fuera">Detrás del velo</button>

    @if (abierto()) {
      <div id="dialogo" appModalAccesible (cerrarPedido)="cerrado = cerrado + 1">
        <button id="primero">Uno</button>
        <input id="medio">
        <button id="ultimo">Tres</button>
      </div>
    }
  `
})
class Anfitrion {
  abierto = signal(true);
  cerrado = 0;
}

/**
 * Lo que un diálogo tiene que hacer y ninguno de los nuestros hacía.
 *
 * Todo esto solo se nota si no usas el ratón, que es exactamente por qué llevaba
 * tanto tiempo sin arreglarse: probándolo a mano nunca falla.
 */
describe('ModalAccesible', () => {

  let fixture: ComponentFixture<Anfitrion>;
  let anfitrion: Anfitrion;

  const porId = (id: string) => document.getElementById(id) as HTMLElement;

  function pulsar(tecla: string, conShift = false): void {
    porId('dialogo').dispatchEvent(new KeyboardEvent('keydown', {
      key: tecla, shiftKey: conShift, bubbles: true
    }));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [Anfitrion] }).compileComponents();
    fixture = TestBed.createComponent(Anfitrion);
    anfitrion = fixture.componentInstance;
    // Al DOM de verdad: el foco no se mueve a un elemento que no está en la
    // página, así que sin esto ninguna de estas pruebas mediría nada.
    document.body.appendChild(fixture.nativeElement);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => fixture.nativeElement.remove());

  it('al abrirse enfoca el primer control, no el contenedor', async () => {
    // Donde se actúa, no donde empieza la caja: si no, hay que tabular una vez
    // de más antes de poder hacer nada.
    await new Promise(r => setTimeout(r));
    expect(document.activeElement?.id).toBe('primero');
  });

  it('Escape pide cerrar', () => {
    pulsar('Escape');
    expect(anfitrion.cerrado).toBe(1);
  });

  it('otras teclas no cierran nada', () => {
    pulsar('a');
    pulsar('Enter');
    expect(anfitrion.cerrado).toBe(0);
  });

  it('desde el último control, Tab vuelve al primero', () => {
    porId('ultimo').focus();
    pulsar('Tab');

    // Sin esto el tabulador se escapaba al fondo de la página: se podía pulsar
    // un botón de detrás del velo sin ver dónde estabas.
    expect(document.activeElement?.id).toBe('primero');
  });

  it('desde el primero, Shift+Tab va al último', () => {
    porId('primero').focus();
    pulsar('Tab', true);

    expect(document.activeElement?.id).toBe('ultimo');
  });

  it('por el medio no se toca nada: el navegador ya sabe tabular', () => {
    porId('medio').focus();
    pulsar('Tab');

    expect(document.activeElement?.id).toBe('medio');
  });

  it('al cerrarse devuelve el foco a donde estaba', () => {
    // Se parte de cerrado: el fixture nace abierto y su ciclo de destrucción
    // movería el foco por su cuenta.
    anfitrion.abierto.set(false);
    fixture.detectChanges();

    // Esto es lo que habría hecho quien abre el diálogo desde un botón.
    porId('fuera').focus();

    anfitrion.abierto.set(true);
    fixture.detectChanges();

    anfitrion.abierto.set(false);
    fixture.detectChanges();

    // Volvía al principio del documento y había que recorrer la página entera
    // para llegar al botón que acababas de usar.
    expect(document.activeElement?.id).toBe('fuera');
  });
});
