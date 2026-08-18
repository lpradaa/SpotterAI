import { Component, inject, signal, input, output, ChangeDetectorRef, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SesionesService, Sesion, SugerenciaSesion } from '../../services/sesiones.service';
import { IdiomaService } from '../../services/idioma.service';

/**
 * Proponer un día y una hora concretos a un compañero.
 *
 * <p>El formulario no sale vacío: pide al servidor el próximo hueco que
 * compartís —el mismo solape con el que se calcula la compatibilidad— y lo trae
 * ya puesto. Eso es lo que separa proponer de ponerse a cuadrar horarios en el
 * chat, que es justo lo que la aplicación existe para evitar.
 *
 * <p>Vivía dentro del chat. Sacarlo aquí es lo que permite proponer también
 * desde la ficha de alguien, que es donde de verdad se decide: miras su semana,
 * ves que coincidís los lunes y los dos vais siempre, y quedas. Antes había que
 * salir al chat para eso.
 */
@Component({
  selector: 'app-proponer-sesion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './proponer-sesion.html',
  styleUrl: './proponer-sesion.scss'
})
export class ProponerSesionComponent implements OnInit {

  private sesiones = inject(SesionesService);
  private cdr = inject(ChangeDetectorRef);

  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);

  /** Con quién se quiere quedar. */
  paraId = input.required<number>();

  cerrar = output<void>();
  /** La sesión ya creada, para que quien lo abrió la pinte. */
  propuesta = output<Sesion>();

  sugerencia = signal<SugerenciaSesion | null>(null);
  error = signal<string | null>(null);
  enviando = signal(false);

  formulario = { fecha: '', horaInicio: '', horaFin: '', nota: '' };

  /**
   * Dónde quedar, cuando cada uno entrena en un sitio.
   *
   * <p>Es la pregunta que el formulario no hacía. La sesión ya tenía sitio en la
   * base de datos, pero solo se rellenaba cuando los dos compartían gimnasio; si
   * no, quedaba vacío y dos personas quedaban a una hora y en ninguna parte.
   *
   * <p>Empieza sin elegir a propósito: uno de los dos tiene que desplazarse, y
   * dar por hecho cuál sería decidir por ellos.
   */
  gimnasioElegido = signal<number | null>(null);

  ngOnInit(): void {
    this.sesiones.sugerencia(this.paraId()).subscribe({
      next: s => {
        this.sugerencia.set(s);
        if (s.hayFranjas) {
          this.formulario = {
            fecha: s.fecha!,
            horaInicio: (s.horaInicio ?? '').slice(0, 5),
            horaFin: (s.horaFin ?? '').slice(0, 5),
            nota: ''
          };
        }
        // Sin franjas en común no se inventa una hora: el formulario se queda
        // vacío y la pista de arriba dice por qué.
        this.cdr.detectChanges();
      },
      error: () => {
        this.sugerencia.set(null);
        this.cdr.detectChanges();
      }
    });
  }

  get completo(): boolean {
    return !!(this.formulario.fecha && this.formulario.horaInicio && this.formulario.horaFin);
  }

  enviar(): void {
    if (!this.completo || this.enviando()) return;

    this.enviando.set(true);
    this.error.set(null);

    this.sesiones.proponer(this.paraId(), {
      fecha: this.formulario.fecha,
      horaInicio: this.formulario.horaInicio,
      horaFin: this.formulario.horaFin,
      nota: this.formulario.nota || null,
      gimnasioId: this.gimnasioElegido()
    }).subscribe({
      next: sesion => this.propuesta.emit(sesion),
      error: err => {
        this.enviando.set(false);
        // El servidor explica por qué no vale —el pasado, una duración
        // imposible, una propuesta ya en marcha—; repetir esas reglas aquí sería
        // mantener dos versiones de lo mismo. El respaldo sí es nuestro, para
        // cuando lo que falla es la conexión y no hay mensaje que enseñar.
        this.error.set(err?.error?.error ?? this.i18n.t('propuesta.error'));
        this.cdr.detectChanges();
      }
    });
  }
}
