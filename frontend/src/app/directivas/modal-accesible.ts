import { Directive, ElementRef, OnDestroy, OnInit, inject, output } from '@angular/core';

/**
 * Lo que un diálogo tiene que hacer y ninguno de los nuestros hacía.
 *
 * <p>Tres cosas, y las tres se notan solo si no usas el ratón:
 *
 * <ul>
 *   <li><b>Cerrar con Escape.</b> Es el gesto que espera todo el mundo. Sin él,
 *       quien navega con teclado tiene que buscar la equis a base de tabulador.
 *   <li><b>Atrapar el foco.</b> El diálogo tapa la página, pero el tabulador
 *       seguía paseando por los botones de detrás del velo: se podía enviar una
 *       solicitud sin ver dónde estabas pulsando.
 *   <li><b>Devolver el foco al salir.</b> Al cerrar, el foco volvía al principio
 *       del documento y había que recorrer la página entera para llegar al botón
 *       que acababas de usar.
 * </ul>
 *
 * <p>Como directiva y no copiado en cada modal: hay cuatro, y de cuatro copias
 * una se queda sin arreglar.
 */
@Directive({
  selector: '[appModalAccesible]',
  standalone: true,
  host: {
    'tabindex': '-1',
    '(keydown)': 'alPulsarTecla($event)'
  }
})
export class ModalAccesible implements OnInit, OnDestroy {

  private elemento: ElementRef<HTMLElement> = inject(ElementRef);

  /** Se ha pedido cerrar con Escape. */
  cerrarPedido = output<void>();

  /** Quién tenía el foco antes de abrir, para devolvérselo. */
  private anterior: HTMLElement | null = null;

  /**
   * Lo que se puede enfocar dentro del diálogo.
   *
   * <p>El {@code :not([hidden])} no es adorno: el formulario de perfil mete sus
   * selectores de archivo dentro de una etiqueta y los oculta con ese atributo,
   * así que sin excluirlos el tabulador se pararía en controles invisibles.
   *
   * <p>Se filtra por atributo y no por {@code offsetParent}, que es lo primero
   * que uno escribe. {@code offsetParent} depende de que haya maquetación
   * calculada: en el navegador funciona, y en el entorno de pruebas devuelve
   * null para todo, de modo que la directiva parecía rota justo donde se
   * comprueba si lo está.
   */
  private static readonly ENFOCABLES = [
    'a[href]', 'button:not([disabled])', 'input:not([disabled])',
    'select:not([disabled])', 'textarea:not([disabled])', '[tabindex]:not([tabindex="-1"])'
  ].map(selector => `${selector}:not([hidden])`).join(',');

  ngOnInit(): void {
    this.anterior = document.activeElement as HTMLElement | null;

    // Al primer control, no al contenedor: así se empieza donde se actúa. Si no
    // hay ninguno, al propio diálogo, que por eso lleva tabindex="-1".
    const primero = this.enfocables()[0] ?? this.elemento.nativeElement;
    // En el siguiente ciclo: al arrancar la directiva, el contenido del diálogo
    // puede no estar todavía en el DOM.
    setTimeout(() => primero.focus());
  }

  ngOnDestroy(): void {
    this.anterior?.focus?.();
  }

  alPulsarTecla(evento: KeyboardEvent): void {
    if (evento.key === 'Escape') {
      evento.stopPropagation();
      this.cerrarPedido.emit();
      return;
    }

    if (evento.key !== 'Tab') return;

    const enfocables = this.enfocables();
    if (enfocables.length === 0) return;

    const primero = enfocables[0];
    const ultimo = enfocables[enfocables.length - 1];
    const activo = document.activeElement;

    // El ciclo se cierra a mano: sin esto, el tabulador se escapa al fondo de la
    // página en cuanto pasa del último control.
    if (evento.shiftKey && (activo === primero || activo === this.elemento.nativeElement)) {
      evento.preventDefault();
      ultimo.focus();
    } else if (!evento.shiftKey && activo === ultimo) {
      evento.preventDefault();
      primero.focus();
    }
  }

  private enfocables(): HTMLElement[] {
    return Array.from(
      this.elemento.nativeElement.querySelectorAll<HTMLElement>(ModalAccesible.ENFOCABLES)
    );
  }
}
