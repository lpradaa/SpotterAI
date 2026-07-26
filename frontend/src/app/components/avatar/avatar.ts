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
@Component({
  selector: 'app-avatar',
  standalone: true,
  template: `
    @if (foto()) {
      <img class="avatar avatar--foto" [src]="foto()" [alt]="nombre() || 'Foto de perfil'"
           [style.--medida.px]="tamano()" loading="lazy">
    } @else {
      <span class="avatar" [attr.data-color]="color()" [style.--medida.px]="tamano()"
            [attr.title]="nombre()" aria-hidden="true">{{ iniciales() }}</span>
    }
  `,
  styleUrl: './avatar.scss'
})
export class Avatar {
  nombre = input<string | null | undefined>('');
  /** Color elegido por la persona. Puede venir vacío o con datos antiguos. */
  valor = input<string | null | undefined>('');
  /** Ruta absoluta de la foto, si la tiene. */
  foto = input<string | null | undefined>(null);
  tamano = input<number>(36);

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
