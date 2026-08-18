import {
  Component, ChangeDetectorRef, inject, signal, computed, input, output, effect
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UsuarioService } from '../../services/usuario.service';
import { PerfilesService, Hito } from '../../services/perfiles.service';
import { Avatar, COLORES_AVATAR } from '../avatar/avatar';
import { ModalAccesible } from '../../directivas/modal-accesible';
import { HttpClient } from '@angular/common/http';
import { api } from '../../config/api';
import { MINIMO_CONTRASENA } from '../restablecer/restablecer';
import { VALORES_DE_DIA, etiquetaDeDia, etiquetaDeColor } from '../../utils/valores-de-perfil';
import { IdiomaService } from '../../services/idioma.service';

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
  imports: [CommonModule, FormsModule, Avatar, ModalAccesible],
  templateUrl: './modal-perfil.html',
  styleUrl: './modal-perfil.scss'
})
export class ModalPerfilComponent {

  private usuarioService = inject(UsuarioService);
  private perfiles = inject(PerfilesService);
  private cdr = inject(ChangeDetectorRef);
  private http = inject(HttpClient);

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

  /**
   * El nombre del color, para el boton que solo se oye.
   *
   * El circulo no lleva texto: quien mira ve el color y quien escucha oye esto.
   * Interpolar la clave guardada dejaba «ascua colour» en la pantalla inglesa.
   */
  protected nombreDelColor(color: string): string {
    return etiquetaDeColor(color, c => this.i18n.t(c));
  }

  /**
   * Los días del desplegable de cada horario.
   *
   * <p>Del vocabulario compartido y no de una lista propia: este desplegable
   * **escribe** el día en la franja que se guarda, igual que la rejilla de
   * pintar, así que las dos tienen que decir exactamente lo mismo. Era la cuarta
   * copia de la lista, y la última.
   */
  diasSemana = VALORES_DE_DIA;

  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);

  /**
   * Cómo se lee un día guardado.
   *
   * <p>El desplegable de arriba escribe el valor en español —es lo que se
   * cruza con la semana de otra persona— y esto es lo único que cambia.
   */
  protected nombreDeDia(valor: string): string {
    return etiquetaDeDia(valor, c => this.i18n.t(c));
  }

  /** Coincide con el límite de la columna y con el recorte del servidor. */
  readonly maxBiografia = 280;

  /**
   * Los dos topes que la pantalla dice en voz alta.
   *
   * <p>Escritos una vez y no repetidos en la plantilla: el número aparece en el
   * contador («2 de 3») y en la condición que decide si se puede añadir otro,
   * y separados acabarían diciendo cosas distintas. Los mismos que aplica el
   * backend, que es quien de verdad los impone.
   */
  protected readonly MAX_LEVANTAMIENTOS = 3;
  protected readonly MAX_HITOS = 12;

  /**
   * Tope de franjas marcadas como "Voy siempre".
   *
   * Sin tope todo el mundo las marcaría todas y el campo dejaría de distinguir
   * nada, igual que unos deslizadores que todos ponen al máximo. El backend
   * aplica el mismo límite: esto solo evita que la interfaz prometa algo que
   * luego se recorta al guardar.
   */
  readonly maxHabituales = 3;

  // --- BLOQUEADOS ---
  protected bloqueados = signal<{ usuarioId: number; nombre: string }[]>([]);

  private cargarBloqueados(): void {
    this.http.get<{ usuarioId: number; nombre: string }[]>(api('/api/bloqueos'))
      .subscribe({ next: l => { this.bloqueados.set(l ?? []); this.cdr.detectChanges(); },
                   error: () => {} });
  }

  protected desbloquear(otroId: number): void {
    this.http.delete(api(`/api/bloqueos/${otroId}`)).subscribe({
      next: () => this.bloqueados.update(l => l.filter(b => b.usuarioId !== otroId)),
      error: () => {},
    });
  }

  // --- CAMBIAR LA CONTRASEÑA ---
  protected cambiandoContrasena = signal(false);
  protected cambioEnCurso = signal(false);
  protected errorContrasena = signal<string | null>(null);
  protected contrasenaActual = '';
  protected contrasenaNueva = '';
  protected readonly minimoContrasena = MINIMO_CONTRASENA;

  protected get nuevaContrasenaCorta(): boolean {
    return this.contrasenaNueva.length > 0 && this.contrasenaNueva.length < MINIMO_CONTRASENA;
  }

  /**
   * Cambia la contraseña sin pasar por "la he olvidado".
   *
   * <p>Se pide la actual aunque haya sesión abierta: una sesión abierta en un
   * ordenador prestado no debería bastar para quedarse con la cuenta.
   *
   * <p>El backend invalida todas las sesiones al cambiarla, incluida esta, así
   * que se sale igual que al borrar la cuenta: `location.href` y no el router,
   * para dejar el navegador limpio del todo y no arrastrar servicios que
   * seguirían pidiendo datos de una sesión que ya no vale.
   */
  protected cambiarContrasena(): void {
    if (this.nuevaContrasenaCorta || !this.contrasenaActual) return;

    this.cambioEnCurso.set(true);
    this.errorContrasena.set(null);

    this.http.post(api('/api/auth/contrasena'),
      { actual: this.contrasenaActual, nueva: this.contrasenaNueva })
      .subscribe({
        next: () => {
          localStorage.clear();
          location.href = '/login';
        },
        error: (e) => {
          this.cambioEnCurso.set(false);
          this.errorContrasena.set(e?.error?.error ?? this.i18n.t('perfilEd.errorContrasena'));
        },
      });
  }

  // --- BORRAR LA CUENTA ---
  protected borrando = signal(false);
  protected borradoEnCurso = signal(false);
  protected errorBorrado = signal<string | null>(null);
  protected contrasenaParaBorrar = '';

  /**
   * Borra la cuenta y saca a la persona de la aplicación.
   *
   * <p>Se navega con `location.href` y no con el router: hay que dejar el
   * estado del navegador limpio del todo. Con una navegación de Angular, los
   * servicios siguen vivos y algunos seguirían pidiendo datos de una cuenta que
   * ya no existe.
   */
  protected borrarCuenta(): void {
    this.borradoEnCurso.set(true);
    this.errorBorrado.set(null);

    this.http.post(api('/api/auth/cuenta/borrar'), { password: this.contrasenaParaBorrar })
      .subscribe({
        next: () => {
          localStorage.clear();
          location.href = '/login';
        },
        error: (e) => {
          this.borradoEnCurso.set(false);
          this.errorBorrado.set(e?.error?.error ?? this.i18n.t('perfilEd.errorBorrado'));
        },
      });
  }

  perfilForm: any = {
    avatar: '', edad: null, genero: '', peso: null, nivel: '',
    objetivos: '', gimnasioId: null, nuevoGimnasioNombre: '', biografia: '',
    horarios: [], levantamientos: [], metaSemanal: 4, rutina: '', fotoUrl: null,
    avisosPorCorreo: true, puedoDesplazarme: false
  };

  /** Los catálogos los manda el backend: duplicarlos aquí es que diverjan. */
  ejerciciosDisponibles = signal<{ clave: string, nombre: string, basico?: boolean }[]>([]);
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
    this.cargarBloqueados();
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
      fotoUrl: data.fotoUrl ?? null,
      // ?? y no ||: con || un false del servidor se convertiria en true y la
      // pantalla diria que recibes avisos justo despues de que te dieras de baja.
      avisosPorCorreo: data.avisosPorCorreo ?? true,
      puedoDesplazarme: data.puedoDesplazarme ?? false
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
        this.avisar(err, this.i18n.t('perfilEd.errorFoto'));
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

  /**
   * Añade una fila, ya con un ejercicio propuesto.
   *
   * <p>Propone el siguiente **básico** que no esté puesto —sentadilla, banca,
   * peso muerto— y solo cae en la lista general si ya están los tres. No es
   * una preferencia estética: el factor de fuerza solo puede comparar cuando
   * las dos personas han apuntado *el mismo* ejercicio, y con seis a elegir eso
   * pasaba en el 22 % de las parejas. Sugerirlos lo sube al 30 %, medido en
   * `docs/medir-el-motor.md`.
   *
   * <p>Se sugiere el ejercicio y **nunca el peso**. Rellenar un número por
   * alguien sería inventarle un dato al factor del que depende el nombre del
   * producto, que es justo lo que `PerfilMinimo` evita al no exigir las marcas.
   * Y se puede cambiar: quien entrene otra cosa la apunta igual.
   */
  anadirLevantamiento(): void {
    if (this.perfilForm.levantamientos.length >= this.MAX_LEVANTAMIENTOS) return;

    this.perfilForm.levantamientos.push({
      ejercicio: this.siguienteSugerido(),
      peso: null,
      repeticiones: null,
    });
  }

  /** El primer ejercicio sugerido que todavía no está en la lista, o ninguno. */
  private siguienteSugerido(): string {
    const puestos = new Set(
      (this.perfilForm.levantamientos ?? []).map((l: any) => l.ejercicio).filter(Boolean));

    // El backend los manda con los básicos delante, así que el primero libre de
    // la lista ya es el que hay que proponer.
    const libre = this.ejerciciosDisponibles()
      .find(e => (e as any).basico && !puestos.has(e.clave));

    return libre?.clave ?? '';
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
        this.avisar(err, this.i18n.t('perfilEd.errorArchivo'));
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
      error: err => this.avisar(err, this.i18n.t('perfilEd.errorMarca'))
    });
  }

  borrarHito(hitoId: number): void {
    this.perfiles.borrarHito(hitoId).subscribe({
      next: () => {
        this.misHitos.update(lista => lista.filter(h => h.id !== hitoId));
        this.cdr.detectChanges();
      },
      error: () => this.aviso.emit({ texto: this.i18n.t('perfilEd.errorBorrarMarca'), tipo: 'error' })
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
        texto: this.i18n.t('perfilEd.topeFijas', { tope: this.maxHabituales }), tipo: 'error'
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
        this.aviso.emit({ texto: this.i18n.t('perfilEd.guardado'), tipo: 'success' });
        this.guardado.emit();
      },
      error: () => this.aviso.emit({
        texto: this.i18n.t('perfilEd.errorGuardar'), tipo: 'error'
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
