import {
  Component, ChangeDetectorRef, inject, signal, computed, input, output, effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '../../services/usuario.service';
import { PerfilesService, Hito } from '../../services/perfiles.service';
import { Avatar, COLORES_AVATAR } from '../avatar/avatar';

/** Lo que sale hacia el tablero para que enseñe un aviso. */
export interface AvisoPerfil {
  texto: string;
  tipo: 'success' | 'error';
}

/**
 * Editar el perfil propio: color o foto, datos, levantamientos, marcas, meta,
 * gimnasio y horarios.
 *
 * <p>Vivía dentro del tablero, y era la mitad de él: 290 de sus 690 líneas de
 * plantilla y 366 de sus 1.184 de estilos. Con esas cifras, `dashboard.scss`
 * llegó a 18,9 kB compilado contra un tope duro de 20 en el que la build de
 * producción falla —y el CI la ejecuta—, así que cada cosa que se añadiera al
 * tablero acercaba el momento de romperlo.
 *
 * <p>Se lleva su formulario entero, incluidas las subidas de archivo y las
 * marcas. El tablero solo decide si se ve, le pasa el perfil ya cargado y se
 * entera de que se ha guardado.
 */
@Component({
  selector: 'app-modal-perfil',
  standalone: true,
  imports: [CommonModule, FormsModule, Avatar],
  templateUrl: './modal-perfil.html',
  styleUrl: './modal-perfil.scss'
})
export class ModalPerfilComponent {

  private usuarioService = inject(UsuarioService);
  private perfiles = inject(PerfilesService);
  private cdr = inject(ChangeDetectorRef);

  /** El perfil tal y como lo devuelve /api/usuarios/perfil. */
  perfil = input.required<any>();
  gimnasios = input<any[]>([]);
  nombre = input<string>('');

  cerrar = output<void>();
  /** Guardado con éxito: al tablero le toca recargar perfil y matches. */
  guardado = output<void>();
  aviso = output<AvisoPerfil>();

  /**
   * Colores de identidad. Antes esto era una rejilla de emoticonos, que además
   * de no parecer un producto serio no identificaba a nadie: dos personas con
   * el mismo emoji eran indistinguibles en una lista.
   */
  coloresAvatar = COLORES_AVATAR;
  diasSemana = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

  /** Coincide con el límite de la columna y con el recorte del servidor. */
  readonly maxBiografia = 280;

  /**
   * Tope de franjas marcadas como "Voy siempre".
   *
   * Sin tope todo el mundo las marcaría todas y el campo dejaría de distinguir
   * nada, igual que unos deslizadores que todos ponen al máximo. El backend
   * aplica el mismo límite: esto solo evita que la interfaz prometa algo que
   * luego se recorta al guardar.
   */
  readonly maxHabituales = 3;

  perfilForm: any = {
    avatar: '', edad: null, genero: '', peso: null, nivel: '',
    objetivos: '', gimnasioId: null, nuevoGimnasioNombre: '', biografia: '',
    horarios: [], levantamientos: [], metaSemanal: 4, rutina: '', fotoUrl: null
  };

  /** Los catálogos los manda el backend: duplicarlos aquí es que diverjan. */
  ejerciciosDisponibles = signal<{ clave: string, nombre: string }[]>([]);
  rutinasDisponibles = signal<{ clave: string, nombre: string }[]>([]);

  nuevoGimnasioNombre = '';
  mostrarInputGimnasio = false;

  misHitos = signal<Hito[]>([]);
  subiendo = signal(false);
  nuevoHito = {
    titulo: '', descripcion: '', fecha: '',
    medioUrl: null as string | null, medioTipo: null as string | null
  };

  /**
   * Señal aparte y no un computed sobre perfilForm: perfilForm es un objeto
   * plano, así que cambiarle un campo no notifica a nadie y la foto no se
   * refrescaría al subirla.
   */
  fotoActual = signal<string | null>(null);
  subiendoFoto = signal(false);
  urlFoto = computed(() => this.perfiles.urlDeMedio(this.fotoActual()));

  /**
   * La meta mientras se arrastra el deslizador.
   *
   * Es una señal propia y no la del tablero: antes arrastrarlo movía la barra
   * de progreso de detrás sin haber guardado nada, o sea que la pantalla
   * enseñaba un dato que no estaba en ningún sitio. Ahora la de fuera se entera
   * al guardar, como el resto del formulario.
   */
  metaSemanal = signal(4);

  /** Cuántos levantamientos están completos, que es lo que cuenta para el motor. */
  levantamientosPuestos = computed(() =>
    (this.perfilForm.levantamientos ?? [])
      .filter((l: any) => l.ejercicio && l.peso > 0 && l.repeticiones > 0).length);

  constructor() {
    // El perfil llega ya cargado por el tablero; esto lo vuelca en el
    // formulario cada vez que cambia, que en la práctica es al abrirse.
    effect(() => this.volcar(this.perfil()));
    this.cargarMisHitos();
  }

  private volcar(data: any): void {
    if (!data) return;

    this.perfilForm = {
      avatar: data.avatar || '', edad: data.edad, genero: data.genero, peso: data.peso,
      // Sin defecto inventado: rellenar 'Intermedio' hacía que la barra
      // mostrara un nivel que nadie había elegido, al lado de un aviso que
      // decía que faltaba justo eso.
      nivel: data.nivel || '', objetivos: data.objetivos || '', gimnasioId: data.gimnasioId,
      biografia: data.biografia || '',
      horarios: (data.horarios || []).map((h: any) => ({ ...h })),
      levantamientos: (data.levantamientos || []).map((l: any) => ({ ...l })),
      metaSemanal: data.metaSemanal || 4,
      rutina: data.rutina || '',
      fotoUrl: data.fotoUrl ?? null
    };

    this.ejerciciosDisponibles.set(data.ejerciciosDisponibles || []);
    this.rutinasDisponibles.set(data.rutinasDisponibles || []);
    this.fotoActual.set(data.fotoUrl ?? null);
    this.metaSemanal.set(this.perfilForm.metaSemanal);
  }

  restanteBiografia(): number {
    return this.maxBiografia - (this.perfilForm.biografia?.length ?? 0);
  }

  // --- Color y foto ---

  /**
   * Elegir un color implica querer las iniciales: con foto puesta, el color no
   * se ve por ningún lado y el selector parecería estropeado.
   */
  seleccionarAvatar(color: string): void {
    this.perfilForm.avatar = color;
    this.quitarFoto();
  }

  alElegirFoto(evento: Event): void {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    if (!archivo) return;

    this.subiendoFoto.set(true);
    this.perfiles.subirMedio(archivo).subscribe({
      next: r => {
        this.perfilForm.fotoUrl = r.url;
        this.fotoActual.set(r.url);
        this.subiendoFoto.set(false);
        // Sin esto, elegir el mismo archivo dos veces seguidas no dispara change
        entrada.value = '';
        this.cdr.detectChanges();
      },
      error: err => {
        this.subiendoFoto.set(false);
        entrada.value = '';
        this.avisar(err, 'No se ha podido subir la foto.');
      }
    });
  }

  quitarFoto(): void {
    this.perfilForm.fotoUrl = null;
    this.fotoActual.set(null);
    this.cdr.detectChanges();
  }

  urlMedio(ruta: string | null): string | null {
    return this.perfiles.urlDeMedio(ruta);
  }

  // --- Levantamientos ---

  anadirLevantamiento(): void {
    if (this.perfilForm.levantamientos.length >= 3) return;
    this.perfilForm.levantamientos.push({ ejercicio: '', peso: null, repeticiones: null });
  }

  quitarLevantamiento(indice: number): void {
    this.perfilForm.levantamientos.splice(indice, 1);
  }

  // --- Marcas ---

  private cargarMisHitos(): void {
    this.perfiles.misHitos().subscribe({
      next: lista => { this.misHitos.set(lista); this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  /** Foto del hito que se está redactando. */
  alElegirMedio(evento: Event): void {
    const archivo = (evento.target as HTMLInputElement).files?.[0];
    if (!archivo) return;

    this.subiendo.set(true);
    this.perfiles.subirMedio(archivo).subscribe({
      next: r => {
        this.nuevoHito.medioUrl = r.url;
        this.nuevoHito.medioTipo = r.tipo;
        this.subiendo.set(false);
        this.cdr.detectChanges();
      },
      // El servidor explica el motivo (tipo no admitido, pasa de 15 MB…) y ese
      // texto es más útil que uno genérico nuestro.
      error: err => {
        this.subiendo.set(false);
        this.avisar(err, 'No se ha podido subir el archivo.');
      }
    });
  }

  anadirHito(): void {
    if (!this.nuevoHito.titulo.trim()) return;

    this.perfiles.crearHito({
      titulo: this.nuevoHito.titulo,
      descripcion: this.nuevoHito.descripcion,
      fecha: this.nuevoHito.fecha || undefined,
      medioUrl: this.nuevoHito.medioUrl,
      medioTipo: this.nuevoHito.medioTipo
    }).subscribe({
      next: hito => {
        this.misHitos.update(lista => [hito, ...lista]);
        this.nuevoHito = { titulo: '', descripcion: '', fecha: '', medioUrl: null, medioTipo: null };
        this.cdr.detectChanges();
      },
      error: err => this.avisar(err, 'No se ha podido guardar la marca.')
    });
  }

  borrarHito(hitoId: number): void {
    this.perfiles.borrarHito(hitoId).subscribe({
      next: () => {
        this.misHitos.update(lista => lista.filter(h => h.id !== hitoId));
        this.cdr.detectChanges();
      },
      error: () => this.aviso.emit({ texto: 'No se ha podido borrar.', tipo: 'error' })
    });
  }

  // --- Meta semanal ---

  actualizarMetaDesdeSlider(event: Event): void {
    const nuevoValor = parseInt((event.target as HTMLInputElement).value, 10);
    this.perfilForm.metaSemanal = nuevoValor;
    this.metaSemanal.set(nuevoValor);
  }

  // --- Gimnasio ---

  toggleNuevoGimnasio(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.mostrarInputGimnasio = (select.value === 'NUEVO');
    if (!this.mostrarInputGimnasio) this.nuevoGimnasioNombre = '';
    this.cdr.detectChanges();
  }

  // --- Horarios ---

  agregarHorario(): void {
    this.perfilForm.horarios.push({
      diaSemana: 'Lunes', horaInicio: '10:00', horaFin: '12:00', habitual: false
    });
  }

  eliminarHorario(index: number): void {
    this.perfilForm.horarios.splice(index, 1);
  }

  habitualesMarcadas(): number {
    return this.perfilForm.horarios.filter((h: any) => h.habitual).length;
  }

  /** Si esta franja puede pasar a habitual, o ya se ha agotado el cupo. */
  puedeMarcarHabitual(horario: any): boolean {
    return horario.habitual || this.habitualesMarcadas() < this.maxHabituales;
  }

  alternarHabitual(horario: any): void {
    if (!this.puedeMarcarHabitual(horario)) {
      this.aviso.emit({
        texto: `Puedes marcar ${this.maxHabituales} franjas como fijas.`, tipo: 'error'
      });
      return;
    }
    horario.habitual = !horario.habitual;
    this.cdr.detectChanges();
  }

  // --- Guardar ---

  guardarPerfil(): void {
    if (this.mostrarInputGimnasio) {
      this.perfilForm.nuevoGimnasioNombre = this.nuevoGimnasioNombre;
      this.perfilForm.gimnasioId = null;
    }

    this.usuarioService.actualizarPerfil(this.perfilForm).subscribe({
      next: () => {
        this.aviso.emit({ texto: 'Perfil actualizado.', tipo: 'success' });
        this.guardado.emit();
      },
      error: () => this.aviso.emit({
        texto: 'Hubo un error al guardar tu perfil.', tipo: 'error'
      })
    });
  }

  /** El servidor suele explicar el motivo mejor que un texto genérico nuestro. */
  private avisar(err: any, porDefecto: string): void {
    this.aviso.emit({
      texto: typeof err?.error === 'string' ? err.error : porDefecto,
      tipo: 'error'
    });
    this.cdr.detectChanges();
  }
}
