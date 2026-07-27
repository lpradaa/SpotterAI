import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { ProponerSesionComponent } from '../proponer-sesion/proponer-sesion';
import { CommonModule } from '@angular/common';
import { PerfilesService, PerfilPublico } from '../../services/perfiles.service';
import { UsuarioService } from '../../services/usuario.service';
import { RejillaSemana } from '../rejilla-semana/rejilla-semana';
import { Avatar } from '../avatar/avatar';
import { Carga } from '../carga/carga';

/**
 * La ficha de una persona.
 *
 * Es la pieza que más cambia el carácter de la aplicación. Hasta ahora no se
 * podía mirar a nadie: había filas con un porcentaje y un botón de "Conectar",
 * que es como se comporta un panel de administración, no un sitio donde hay
 * gente. Aquí la persona ocupa el espacio y el número queda de apoyo.
 *
 * Se abre encima de cualquier pantalla en vez de ser una ruta propia: se entra a
 * mirar y se vuelve a lo que estabas haciendo, sin perder el sitio en la lista.
 */
@Component({
  selector: 'app-perfil-publico',
  standalone: true,
  imports: [CommonModule, RejillaSemana, Avatar, Carga, ProponerSesionComponent],
  templateUrl: './perfil-publico.html',
  styleUrl: './perfil-publico.scss'
})
export class PerfilPublicoComponent {

  private perfiles = inject(PerfilesService);
  private usuarios = inject(UsuarioService);

  /** A quién se mira. Cambiarlo recarga la ficha. */
  usuarioId = input.required<number>();
  /** Mis franjas, para poder dibujar el solape sin volver a pedirlas. */
  misFranjas = input<any[]>([]);

  cerrar = output<void>();
  /** Se ha conectado o desconectado con esta persona: quien escucha refresca. */
  cambioDeRelacion = output<void>();
  escribir = output<number>();

  perfil = signal<PerfilPublico | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);
  enviando = signal(false);

  /** Índice del medio abierto a tamaño grande, o null. */
  medioAmpliado = signal<string | null>(null);

  hitosConMedio = computed(() => this.perfil()?.hitos.filter(h => h.medioUrl) ?? []);

  constructor() {
    effect(() => this.cargar(this.usuarioId()));
  }

  private cargar(id: number): void {
    this.cargando.set(true);
    this.error.set(null);

    this.perfiles.verPerfil(id).subscribe({
      next: p => {
        this.perfil.set(p);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set('No se ha podido cargar el perfil.');
        this.cargando.set(false);
      }
    });
  }

  url(ruta: string | null): string | null {
    return this.perfiles.urlDeMedio(ruta);
  }

  conectar(): void {
    const p = this.perfil();
    if (!p || this.enviando()) return;

    this.enviando.set(true);
    this.usuarios.enviarSolicitudConexion(p.id).subscribe({
      next: () => {
        this.perfil.set({ ...p, solicitudPendiente: true });
        this.enviando.set(false);
        this.cambioDeRelacion.emit();
      },
      error: () => {
        this.error.set('No se ha podido enviar la solicitud.');
        this.enviando.set(false);
      }
    });
  }

  retirar(): void {
    const p = this.perfil();
    if (!p || this.enviando()) return;

    this.enviando.set(true);
    this.usuarios.deshacerRelacion(p.id).subscribe({
      next: () => {
        this.perfil.set({ ...p, solicitudPendiente: false, yaConectado: false });
        this.enviando.set(false);
        this.cambioDeRelacion.emit();
      },
      error: () => {
        this.error.set('No se ha podido deshacer.');
        this.enviando.set(false);
      }
    });
  }

  /**
   * "hace 3 días" en vez de una fecha.
   *
   * En una lista de logros lo que importa es si es reciente, no el día exacto.
   */
  cuando(fecha: string): string {
    const dias = Math.floor((Date.now() - new Date(fecha).getTime()) / 86_400_000);
    if (dias <= 0) return 'hoy';
    if (dias === 1) return 'ayer';
    if (dias < 7) return `hace ${dias} días`;
    if (dias < 30) {
      const semanas = Math.floor(dias / 7);
      return `hace ${semanas} ${semanas === 1 ? 'semana' : 'semanas'}`;
    }
    const meses = Math.floor(dias / 30);
    return `hace ${meses} ${meses === 1 ? 'mes' : 'meses'}`;
  }

  /** Frase de actividad. Sin datos no se dice nada, que inventar es peor. */
  actividad = computed(() => {
    const n = this.perfil()?.entrenosUltimaSemana ?? 0;
    if (n === 0) return null;
    return `${n} ${n === 1 ? 'entrenamiento' : 'entrenamientos'} esta semana`;
  });

  /** Formulario de propuesta abierto dentro de la ficha. */
  proponiendo = signal(false);

  /**
   * Las veces que ya habéis quedado.
   *
   * Dice "habéis quedado" y no "habéis entrenado" a propósito: lo que consta es
   * que lo acordasteis y que el día llegó. Que además fuerais es cosa vuestra,
   * y afirmarlo sería inventar.
   */
  juntos = computed(() => {
    const n = this.perfil()?.sesionesJuntos ?? 0;
    if (n === 0) return null;
    return n === 1 ? 'Ya habéis quedado una vez' : `Ya habéis quedado ${n} veces`;
  });

  /** Propuesta hecha: se cierra el formulario y la ficha lo refleja. */
  alProponer(): void {
    this.proponiendo.set(false);
    this.cargar(this.usuarioId());
  }
}
