import { Component, computed, input } from '@angular/core';

/** Colores que un usuario puede elegir para su identidad visual. */
export const COLORES_AVATAR = ['ascua', 'ambar', 'oliva', 'acero', 'ciruela', 'pizarra'] as const;
export type ColorAvatar = typeof COLORES_AVATAR[number];

/**
 * Identidad visual de una persona: su foto, o sus iniciales sobre un color.
 *
 * Sustituye a los avatares de emoticono del TFG. Un emoji de gorila como foto de
 * perfil es la señal más rápida de que algo es un proyecto de clase, y además no
 * identifica a nadie: dos personas con el mismo emoji son indistinguibles en una
 * lista. Las iniciales sí identifican, y el color deja margen para personalizar.
 *
 * La foto es opcional a propósito: obligar a subir una para empezar a usar la
 * aplicación es una barrera, y las iniciales funcionan perfectamente mientras
 * tanto.
 *
 * Si el valor guardado no es un color conocido —por ejemplo los emoticonos que
 * quedaron de antes— se deriva uno del nombre, de forma que cada persona tiene
 * siempre el mismo y la migración no necesita tocar la base de datos.
 */
/** Los cuatro sitios donde aparece una persona. */
export type Medida = 'fila' | 'ficha' | 'cabecera' | 'protagonista';

const MEDIDAS: Record<Medida, number> = {
  fila: 36,
  ficha: 48,
  cabecera: 64,
  protagonista: 96,
};

@Component({
  selector: 'app-avatar',
  standalone: true,
  template: `
    @if (foto()) {
      <img class="avatar avatar--foto" [src]="foto()" [alt]="nombre() || 'Foto de perfil'"
           [style.--medida.px]="px()" loading="lazy">
    } @else {
      <span class="avatar" [attr.data-color]="color()" [style.--medida.px]="px()"
            [attr.title]="nombre()" aria-hidden="true">{{ iniciales() }}</span>
    }
  `,
  styleUrl: './avatar.scss'
})
export class Avatar {
  nombre = input<string | null | undefined>('');
  /** Color elegido por la persona. Puede venir vacío o con datos antiguos. */
  valor = input<string | null | undefined>('');
  /**
   * Ruta absoluta de la foto, si la tiene.
   *
   * <p>Es opcional, y ahí está la trampa: al no serlo, cada pantalla nueva
   * pinta iniciales de color sin que nada falle, y el fallo solo se ve mirando
   * a una persona que sí tiene foto. Ha pasado seis veces —tres por olvidar el
   * campo en un DTO y tres por no pasarlo aquí— en `mis-conexiones` (lista y
   * cabecera del chat), `ficha-sugerencia`, `solicitudes` y el tablero.
   *
   * <p>Antes de añadir un `<app-avatar>` nuevo: comprueba que el DTO del que
   * sale esa persona trae su foto, no solo su color.
   */
  foto = input<string | null | undefined>(null);

  /**
   * Cómo de grande, por nombre.
   *
   * <p>Había diez tamaños distintos escritos a mano en las plantillas —34, 36,
   * 40, 44, 48, 56, 64, 68, 84 y 96— para cuatro situaciones reales. Nadie
   * eligió diez: cada pantalla nueva copiaba el de al lado y lo ajustaba un
   * poco, que es como una escala deja de existir sin que nadie lo decida.
   *
   * <p>Cuatro peldaños, uno por sitio donde aparece una persona:
   * <ul>
   *   <li><b>fila</b> (36): una línea de una lista, junto a texto</li>
   *   <li><b>ficha</b> (48): un elemento de lista o tarjeta</li>
   *   <li><b>cabecera</b> (64): quién eres, arriba de una pantalla</li>
   *   <li><b>protagonista</b> (96): la persona ES la pantalla</li>
   * </ul>
   */
  medida = input<Medida | null>(null);

  /**
   * En píxeles, para lo que de verdad no encaje en la escala.
   *
   * <p>Sigue existiendo a propósito, pero si aparece un número nuevo conviene
   * preguntarse si no es uno de los cuatro de arriba con otro nombre.
   */
  tamano = input<number>(36);

  /** Lo que acaba valiendo: manda la medida con nombre si la hay. */
  protected px = computed(() => this.medida() ? MEDIDAS[this.medida()!] : this.tamano());

  iniciales = computed(() => {
    const partes = (this.nombre() ?? '').trim().split(/\s+/).filter(Boolean);
    if (partes.length === 0) return '?';
    if (partes.length === 1) return partes[0].charAt(0).toUpperCase();
    return (partes[0].charAt(0) + partes[partes.length - 1].charAt(0)).toUpperCase();
  });

  color = computed<ColorAvatar>(() => {
    const guardado = (this.valor() ?? '').trim();
    if ((COLORES_AVATAR as readonly string[]).includes(guardado)) {
      return guardado as ColorAvatar;
    }
    return this.derivarDelNombre(this.nombre() ?? '');
  });

  /** Reparto estable: el mismo nombre da siempre el mismo color. */
  private derivarDelNombre(nombre: string): ColorAvatar {
    let suma = 0;
    for (let i = 0; i < nombre.length; i++) suma = (suma + nombre.charCodeAt(i)) % 997;
    return COLORES_AVATAR[suma % COLORES_AVATAR.length];
  }
}
