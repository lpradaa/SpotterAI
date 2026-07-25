import { Injectable, signal } from '@angular/core';

export type Tema = 'oscuro' | 'claro';

const CLAVE = 'spotterai_tema';

/**
 * Tema visual de la aplicación.
 *
 * SpotterAI entra en oscuro por defecto: encaja con el contexto y hace que el
 * naranja ascua funcione como luz en vez de como mancha. El claro está
 * disponible y la elección se recuerda entre sesiones.
 *
 * El tema se aplica poniendo data-tema en el elemento raíz; los tokens de
 * _tokens.scss hacen el resto sin que ningún componente tenga que enterarse.
 */
@Injectable({ providedIn: 'root' })
export class TemaService {
  readonly tema = signal<Tema>(this.leerPreferencia());

  constructor() {
    this.aplicar(this.tema());
  }

  alternar(): void {
    const siguiente: Tema = this.tema() === 'oscuro' ? 'claro' : 'oscuro';
    this.tema.set(siguiente);
    this.aplicar(siguiente);
    localStorage.setItem(CLAVE, siguiente);
  }

  private aplicar(tema: Tema): void {
    // Solo se marca el claro: el oscuro es el estado por defecto de :root, así
    // que no hace falta ensuciar el DOM para lo que ya es el comportamiento base.
    const raiz = document.documentElement;
    if (tema === 'claro') {
      raiz.setAttribute('data-tema', 'claro');
    } else {
      raiz.removeAttribute('data-tema');
    }
  }

  private leerPreferencia(): Tema {
    const guardado = localStorage.getItem(CLAVE);
    return guardado === 'claro' ? 'claro' : 'oscuro';
  }
}
