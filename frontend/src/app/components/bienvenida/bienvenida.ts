import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  imports: [CommonModule, RejillaEditable],
  templateUrl: './bienvenida.html',
  styleUrl: './bienvenida.scss'
})
export class BienvenidaComponent implements OnInit {

  private usuarioService = inject(UsuarioService);
  private perfilEstado = inject(PerfilEstadoService);
  private router = inject(Router);

  nombre = signal(localStorage.getItem('usuario_nombre') || '');
  franjas = signal<Franja[]>([]);
  franjasIniciales = signal<Franja[]>([]);
  guardando = signal(false);
  error = signal<string | null>(null);

  /** El perfil que ya existe, para no perder lo demás al guardar. */
  private perfil: any = null;

  puedeContinuar = computed(() => this.franjas().length > 0);

  horasTotales = computed(() =>
    this.franjas().reduce((suma, f) => suma + this.duracion(f), 0)
  );

  ngOnInit(): void {
    // Alguien puede llegar aquí con parte del perfil hecho; se respeta.
    this.usuarioService.getMiPerfil().subscribe({
      next: perfil => {
        this.perfil = perfil ?? {};
        if (perfil?.nombre) this.nombre.set(perfil.nombre);
        const horarios = perfil?.horarios ?? [];
        this.franjasIniciales.set(horarios);
        this.franjas.set(horarios);
      },
      error: () => { this.perfil = {}; }
    });
  }

  alCambiar(franjas: Franja[]): void {
    this.franjas.set(franjas);
    this.error.set(null);
  }

  guardar(): void {
    if (!this.puedeContinuar() || this.guardando()) return;

    this.guardando.set(true);
    this.error.set(null);

    // Se envía el perfil completo porque PUT /perfil reemplaza: mandar solo los
    // horarios borraría nivel, objetivo y gimnasio de quien ya los tuviera.
    const cuerpo = {
      ...(this.perfil ?? {}),
      horarios: this.franjas()
    };

    this.usuarioService.actualizarPerfil(cuerpo).subscribe({
      next: () => {
        this.perfilEstado.marcarIndicado();
        this.router.navigate(['/dashboard']);
      },
      error: () => {
        this.guardando.set(false);
        this.error.set('No se ha podido guardar tu horario. Inténtalo otra vez.');
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
