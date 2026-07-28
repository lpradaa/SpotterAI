import { Component, computed, effect, inject, output, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { ProponerSesionComponent } from '../proponer-sesion/proponer-sesion';
import { ModalPerfilComponent } from '../modal-perfil/modal-perfil';
import { CommonModule } from '@angular/common';
import { PerfilesService, PerfilPublico } from '../../services/perfiles.service';
import { UsuarioService } from '../../services/usuario.service';
import { RejillaSemana } from '../rejilla-semana/rejilla-semana';
import { Avatar } from '../avatar/avatar';
import { Carga } from '../carga/carga';

/**
 * La página de una persona.
 *
 * Es la pieza que más cambia el carácter de la aplicación. Al principio no se
 * podía mirar a nadie: había filas con un porcentaje y un botón de "Conectar",
 * que es como se comporta un panel de administración, no un sitio donde hay
 * gente. Aquí la persona ocupa el espacio y el número queda de apoyo.
 *
 * <p>Y ahora es una ruta, que era lo que faltaba para que se pareciera a una red
 * social. Siendo un panel flotante, una persona no tenía sitio propio: no había
 * URL a la que enlazar, el botón "atrás" del navegador no hacía lo esperable, y
 * el mismo componente vivía incrustado a la vez en el tablero y en Explorar. En
 * una red social la gente son lugares.
 *
 * <p>`/yo` lleva aquí también, con tu propio identificador. Verte como te ven es
 * distinto de editarte, y hasta ahora solo existía lo segundo.
 */
@Component({
  selector: 'app-perfil-publico',
  standalone: true,
  imports: [CommonModule, RejillaSemana, Avatar, Carga, ProponerSesionComponent, ModalPerfilComponent],
  templateUrl: './perfil-publico.html',
  styleUrl: './perfil-publico.scss'
})
export class PerfilPublicoComponent {

  private perfiles = inject(PerfilesService);
  private usuarios = inject(UsuarioService);
  private ruta = inject(ActivatedRoute);
  private router = inject(Router);

  /**
   * A quién se mira, sacado de la URL.
   *
   * `/yo` no lleva identificador: se resuelve con el que guardó el login, que
   * es el mismo que usa el resto de la aplicación.
   */
  usuarioId = computed(() => {
    const parametros = this.parametros();
    const enLaUrl = Number(parametros?.get('id'));
    return Number.isFinite(enLaUrl) && enLaUrl > 0 ? enLaUrl : this.miId();
  });

  private parametros = toSignal(this.ruta.paramMap);

  /**
   * Edición del perfil propio, alojada aquí.
   *
   * El formulario vivía en el tablero, así que había dos sitios desde los que
   * tocar tu perfil y ninguno era tu perfil. Tu página es donde te ves y donde
   * te cambias; el tablero solo enlaza aquí.
   */
  editando = signal(false);
  perfilPropio = signal<any>(null);
  gimnasios = signal<any[]>([]);
  avisoDelFormulario = signal<string | null>(null);

  /** Mis franjas, para dibujar el solape. Se piden aquí porque ya no las hereda. */
  misFranjas = signal<any[]>([]);

  perfil = signal<PerfilPublico | null>(null);
  cargando = signal(true);
  error = signal<string | null>(null);
  enviando = signal(false);

  /** Índice del medio abierto a tamaño grande, o null. */
  medioAmpliado = signal<string | null>(null);

  hitosConMedio = computed(() => this.perfil()?.hitos.filter(h => h.medioUrl) ?? []);

  constructor() {
    effect(() => {
      const id = this.usuarioId();
      if (id > 0) this.cargar(id);
    });

    // Para dibujar el solape hace falta tu semana. Antes se la pasaba la
    // pantalla que lo incrustaba; siendo una página, se pide aquí.
    this.usuarios.getMiPerfil().subscribe({
      next: perfil => this.misFranjas.set(perfil?.horarios ?? []),
      error: () => {}
    });
  }

  private miId(): number {
    return Number(localStorage.getItem('userId') || localStorage.getItem('id')) || 0;
  }

  /**
   * Vuelve a donde estabas.
   *
   * El historial del navegador y no una ruta fija: se llega aquí desde Explorar,
   * desde el chat y desde la actividad del tablero, y mandar siempre al mismo
   * sitio sería perder el hilo en dos de los tres casos.
   */
  volver(): void {
    if (history.length > 1) history.back();
    else this.router.navigate(['/dashboard']);
  }

  abrirEdicion(): void {
    // El formulario necesita el perfil crudo y el catálogo de gimnasios, que
    // son datos distintos de los de la ficha pública.
    this.usuarios.getMiPerfil().subscribe({
      next: perfil => { this.perfilPropio.set(perfil); this.editando.set(true); },
      error: () => this.error.set('No se ha podido abrir el formulario.')
    });
    this.usuarios.getGimnasios().subscribe({
      next: lista => this.gimnasios.set(lista),
      error: () => {}
    });
  }

  /** Guardado: se cierra y la ficha se recarga para reflejarlo. */
  alGuardar(): void {
    this.editando.set(false);
    this.cargar(this.usuarioId());
    this.avisoDelFormulario.set('Perfil actualizado.');
    setTimeout(() => this.avisoDelFormulario.set(null), 3000);
  }

  /** Al chat con esta persona. */
  escribirA(id: number): void {
    this.router.navigate(['/conexiones'], { queryParams: { con: id } });
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
