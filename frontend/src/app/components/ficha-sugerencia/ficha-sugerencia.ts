import {
  Component, ChangeDetectorRef, inject, signal, computed, input, output, effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { UsuarioService, Match, ExplicacionMatch } from '../../services/usuario.service';
import { RejillaSemana } from '../rejilla-semana/rejilla-semana';
import { Avatar } from '../avatar/avatar';

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
  imports: [CommonModule, RejillaSemana, Avatar],
  templateUrl: './ficha-sugerencia.html',
  styleUrl: './ficha-sugerencia.scss'
})
export class FichaSugerenciaComponent {

  private usuarioService = inject(UsuarioService);
  private cdr = inject(ChangeDetectorRef);

  candidatos = input<Match[]>([]);
  misFranjas = input<any[]>([]);

  cerrar = output<void>();
  /** Id de la persona con la que se quiere conectar. */
  conectar = output<number>();

  indiceActual = signal(0);
  explicacion = signal<ExplicacionMatch | null>(null);
  cargandoExplicacion = signal(false);

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

  /** Convierte los minutos de solape en algo legible: "4h 30min". */
  formatearSolape(minutos: number): string {
    if (!minutos) return '';
    const horas = Math.floor(minutos / 60);
    const resto = minutos % 60;
    if (horas === 0) return `${resto} min`;
    if (resto === 0) return horas === 1 ? '1 hora' : `${horas} horas`;
    return `${horas}h ${resto}min`;
  }

  /** Tramo de compatibilidad, para no repetir umbrales por la plantilla. */
  tramo(puntuacion: number): 'alta' | 'media' | 'baja' {
    if (puntuacion >= 70) return 'alta';
    if (puntuacion >= 40) return 'media';
    return 'baja';
  }

  private cargarExplicacion(candidatoId: number): void {
    this.explicacion.set(null);
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
