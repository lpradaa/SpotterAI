import {
  Component, ChangeDetectorRef, inject, signal, computed, input, output, effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { UsuarioService, Match, ExplicacionMatch } from '../../services/usuario.service';
import { RejillaSemana } from '../rejilla-semana/rejilla-semana';
import { Avatar } from '../avatar/avatar';
import { ModalAccesible } from '../../directivas/modal-accesible';
import { tramoDe } from '../../utils/compatibilidad';
import { PerfilesService } from '../../services/perfiles.service';
import { IdiomaService } from '../../services/idioma.service';
import { etiquetaDeNivel, etiquetaDeObjetivo } from '../../utils/valores-de-perfil';
import { Desglose } from '../desglose/desglose';

/**
 * Los candidatos de uno en uno, en ficha grande.
 *
 * <p>La lista ya llega puntuada y ordenada por el backend, así que abrir esto es
 * instantáneo: no hay ninguna espera artificial. Lo que sí cuesta una llamada es
 * la explicación redactada, y por eso se pide de una en una, solo para la ficha
 * que se está viendo.
 *
 * <p>Estaba dentro del tablero. Es una pantalla entera metida dentro de otra, y
 * de fuera solo necesita dos cosas: a quién enseñar y cuál es tu semana.
 */
@Component({
  selector: 'app-ficha-sugerencia',
  standalone: true,
  imports: [CommonModule, RejillaSemana, Avatar, ModalAccesible, Desglose],
  templateUrl: './ficha-sugerencia.html',
  styleUrl: './ficha-sugerencia.scss'
})
export class FichaSugerenciaComponent {

  private usuarioService = inject(UsuarioService);
  private perfiles = inject(PerfilesService);
  private cdr = inject(ChangeDetectorRef);

  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);

  /** El nivel y el objetivo guardados, tal y como se leen. */
  protected nivel(valor: string): string {
    return etiquetaDeNivel(valor, c => this.i18n.t(c));
  }

  protected objetivo(valor: string): string {
    return etiquetaDeObjetivo(valor, c => this.i18n.t(c));
  }

  /** La foto de alguien, resuelta a URL servible. */
  foto(ruta: string | null | undefined): string | null {
    return this.perfiles.urlDeMedio(ruta ?? null);
  }

  candidatos = input<Match[]>([]);
  misFranjas = input<any[]>([]);

  cerrar = output<void>();
  /** Id de la persona con la que se quiere conectar. */
  conectar = output<number>();

  indiceActual = signal(0);
  explicacion = signal<ExplicacionMatch | null>(null);
  cargandoExplicacion = signal(false);

  /**
   * Si el desglose está desplegado.
   *
   * <p>Plegado por defecto: quien pasa fichas quiere decidir, y ocho barras
   * delante del botón de conectar convierten una ojeada en un informe. Quien
   * quiera saber de dónde sale el número lo pide.
   */
  verDesglose = signal(false);

  /** El candidato que se está mostrando ahora mismo. */
  actual = computed(() => this.candidatos()[this.indiceActual()] ?? null);

  constructor() {
    // Cada vez que cambia a quién se mira, se pide su explicación. Con un
    // effect y no llamándolo desde cada botón: así no hay forma de añadir una
    // tercera manera de moverse y olvidarse de pedirla.
    effect(() => {
      const candidato = this.actual();
      if (candidato) this.cargarExplicacion(candidato.id);
    });
  }

  siguiente(): void {
    if (this.indiceActual() < this.candidatos().length - 1) {
      this.indiceActual.update(i => i + 1);
    }
  }

  anterior(): void {
    if (this.indiceActual() > 0) {
      this.indiceActual.update(i => i - 1);
    }
  }

  conectarConActual(): void {
    const candidato = this.actual();
    if (candidato) this.conectar.emit(candidato.id);
  }

  /**
   * Convierte los minutos de solape en algo legible: «4h 30min».
   *
   * <p>Tres formas y no una: «90 min» se lee peor que «1h 30min», y «2h 0min»
   * no lo dice nadie. El plural de las horas lo resuelve el catálogo.
   */
  formatearSolape(minutos: number): string {
    if (!minutos) return '';
    const horas = Math.floor(minutos / 60);
    const resto = minutos % 60;

    if (horas === 0) return this.i18n.t('duracion.minutos', { cuenta: resto });
    if (resto === 0) return this.i18n.t('duracion.horas', { cuenta: horas });
    return this.i18n.t('duracion.horasYMinutos', { horas, minutos: resto });
  }

  /** Tramo de compatibilidad, para no repetir umbrales por la plantilla. */
  tramo = tramoDe;

  private cargarExplicacion(candidatoId: number): void {
    this.explicacion.set(null);
    // Al cambiar de persona el desglose se pliega: dejarlo abierto enseñaría el
    // de la anterior durante el instante que tarda la nueva respuesta.
    this.verDesglose.set(false);
    this.cargandoExplicacion.set(true);

    this.usuarioService.getExplicacionMatch(candidatoId).subscribe({
      next: texto => {
        // La respuesta puede llegar tarde: si ya se ha pasado a otra ficha, se
        // descarta en vez de pintar la explicación de otra persona.
        if (this.actual()?.id !== candidatoId) return;
        this.explicacion.set(texto);
        this.cargandoExplicacion.set(false);
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('No se pudo cargar la explicación del match:', err);
        if (this.actual()?.id !== candidatoId) return;
        this.cargandoExplicacion.set(false);
        this.cdr.detectChanges();
      }
    });
  }
}
