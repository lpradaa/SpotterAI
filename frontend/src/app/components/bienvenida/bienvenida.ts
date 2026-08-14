import { IdiomaService } from '../../services/idioma.service';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { PerfilEstadoService } from '../../services/perfil-estado.service';
import { RejillaEditable } from '../rejilla-editable/rejilla-editable';
import { Franja } from '../rejilla-semana/rejilla-semana';

/**
 * Puerta de entrada: sin horario no se pasa al tablero.
 *
 * Se pide aquí y no en el formulario de registro por dos razones. La primera es
 * que el registro es donde más gente abandona, y ahí la cuenta ni siquiera
 * existe todavía. La segunda es que así cubre también a quien ya estaba
 * registrado: se encuentra esta pantalla la próxima vez que entra.
 */
@Component({
  selector: 'app-bienvenida',
  standalone: true,
  imports: [CommonModule, RejillaEditable, FormsModule],
  templateUrl: './bienvenida.html',
  styleUrl: './bienvenida.scss'
})
export class BienvenidaComponent implements OnInit {

  private usuarioService = inject(UsuarioService);
  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);
  private perfilEstado = inject(PerfilEstadoService);
  private router = inject(Router);

  nombre = signal(localStorage.getItem('usuario_nombre') || '');
  franjas = signal<Franja[]>([]);
  franjasIniciales = signal<Franja[]>([]);
  guardando = signal(false);
  error = signal<string | null>(null);

  /**
   * En cuál de los dos pasos estamos.
   *
   * Se empieza por el horario aunque ya lo tengas: es lo que más pesa y lo único
   * que se pinta, así que abrir con cinco desplegables sería empezar por lo
   * aburrido. Quien ya lo tenga solo tiene que pulsar Continuar.
   */
  paso = signal<'horario' | 'datos'>('horario');

  /** Los catálogos los manda el backend, como en el resto de la aplicación. */
  gimnasios = signal<any[]>([]);
  rutinas = signal<{ clave: string, nombre: string }[]>([]);

  datos: {
    gimnasioId: number | null; nivel: string; objetivos: string; rutina: string;
    edad: number | null;
  } = { gimnasioId: null, nivel: '', objetivos: '', rutina: '', edad: null };

  hayHorario = computed(() => this.franjas().length > 0);

  datosCompletos(): boolean {
    return !!(this.datos.gimnasioId && this.datos.nivel && this.datos.objetivos
      && this.datos.rutina && this.datos.edad);
  }

  horasTotales = computed(() =>
    this.franjas().reduce((suma, f) => suma + this.duracion(f), 0)
  );

  ngOnInit(): void {
    // Alguien puede llegar aquí con parte del perfil hecho; se respeta todo lo
    // que ya haya rellenado.
    this.usuarioService.getMiPerfil().subscribe({
      next: perfil => {
        if (perfil?.nombre) this.nombre.set(perfil.nombre);

        const horarios = perfil?.horarios ?? [];
        this.franjasIniciales.set(horarios);
        this.franjas.set(horarios);

        this.datos = {
          gimnasioId: perfil?.gimnasioId ?? null,
          nivel: perfil?.nivel ?? '',
          objetivos: perfil?.objetivos ?? '',
          rutina: perfil?.rutina ?? '',
          edad: perfil?.edad ?? null
        };
        this.rutinas.set(perfil?.rutinasDisponibles ?? []);
      },
      error: () => {}
    });

    this.usuarioService.getGimnasios().subscribe({
      next: lista => this.gimnasios.set(lista ?? []),
      error: () => {}
    });
  }

  /** Del horario a los datos, guardando el horario por el camino. */
  siguiente(): void {
    if (!this.hayHorario() || this.guardando()) return;

    this.guardando.set(true);
    this.error.set(null);

    // Solo el horario. Desde que PUT /perfil parchea en vez de reemplazar, esto
    // ya no borra nivel, objetivo ni gimnasio de quien los tuviera: antes había
    // que releer el perfil entero y reenviarlo para no destruirlo.
    this.usuarioService.actualizarPerfil({ horarios: this.franjas() }).subscribe({
      next: () => {
        this.guardando.set(false);
        this.perfilEstado.olvidar();
        this.paso.set('datos');
      },
      error: () => {
        this.guardando.set(false);
        this.error.set('No se ha podido guardar tu horario. Inténtalo otra vez.');
      }
    });
  }

  alCambiar(franjas: Franja[]): void {
    this.franjas.set(franjas);
    this.error.set(null);
  }

  guardar(): void {
    if (!this.datosCompletos() || this.guardando()) return;

    this.guardando.set(true);
    this.error.set(null);

    this.usuarioService.actualizarPerfil({ ...this.datos }).subscribe({
      next: () => {
        // Se olvida en vez de darlo por hecho: quien decide si el perfil está
        // completo es el backend, y preguntárselo evita que la interfaz y el
        // motor tengan cada uno su versión de la respuesta.
        this.perfilEstado.olvidar();
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.guardando.set(false);
        this.error.set('No se han podido guardar tus datos. Inténtalo otra vez.');
      }
    });
  }

  private duracion(f: Franja): number {
    const hora = (v: string | undefined) => parseInt((v ?? '').split(':')[0], 10);
    const desde = hora(f.horaInicio ?? f.inicio);
    const hasta = hora(f.horaFin ?? f.fin);
    return Number.isNaN(desde) || Number.isNaN(hasta) ? 0 : hasta - desde;
  }
}
