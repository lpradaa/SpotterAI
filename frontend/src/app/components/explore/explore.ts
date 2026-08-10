import { Component, signal, computed, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UsuarioService, Match } from '../../services/usuario.service';
import { RejillaSemana, Franja } from '../rejilla-semana/rejilla-semana';
import { Avatar } from '../avatar/avatar';
import { Carga } from '../carga/carga';
import { FichaSugerenciaComponent } from '../ficha-sugerencia/ficha-sugerencia';
import { PerfilesService } from '../../services/perfiles.service';
import { ModalAccesible } from '../../directivas/modal-accesible';
import { mensajeDeError } from '../../utils/errores';

@Component({
  selector: 'app-explore',
  standalone: true,
  imports: [CommonModule, FormsModule, RejillaSemana, Avatar, Carga, ModalAccesible, FichaSugerenciaComponent],
  templateUrl: './explore.html',
  styleUrl: './explore.scss'
})
export class Explore implements OnInit {
  private usuarioService = inject(UsuarioService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private perfiles = inject(PerfilesService);

  /**
   * A la pagina de una persona.
   *
   * Navegar y no abrir un panel: cada persona tiene su URL, asi que se puede
   * enlazar, volver con el boton del navegador y compartir.
   */
  verPerfil(usuarioId: number): void {
    this.router.navigate(['/u', usuarioId]);
  }

  irAlChat(usuarioId: number): void {
    this.router.navigate(['/conexiones'], { queryParams: { con: usuarioId } });
  }

  /** El backend devuelve rutas relativas; el avatar necesita una usable en src. */
  urlMedio(ruta: string | null): string | null {
    return this.perfiles.urlDeMedio(ruta);
  }

  usuarios = signal<Match[]>([]);
  isFiltrosOpen = signal(false);

  /** Mis franjas, que sirven de fondo en la rejilla de cada ficha. */
  misHorarios = signal<Franja[]>([]);

  filtrosActivos = signal({
    busqueda: '',
    nivel: '',
    objetivo: '',
    gimnasio: '',
    genero: '',
    edadMin: null as number | null,
    edadMax: null as number | null,

    /**
     * Rutina y fuerza: los dos factores más nuevos del motor, 15 de los 100
     * puntos entre ambos, y hasta ahora no se podía filtrar por ninguno.
     *
     * `soloSiPodemosCubrirnos` deja fuera también a quien no tiene marcas: sin
     * ellas no se sabe, y quien busca a alguien que le aguante la barra no
     * quiere "quizás".
     */
    rutina: '',
    soloSiPodemosCubrirnos: false
  });

  /** Rutinas presentes en los resultados, para poblar el desplegable. */
  rutinasDisponibles = computed(() =>
    [...new Set(this.usuarios().map(u => u.rutina).filter((r): r is string => !!r))].sort());

  toast: { show: boolean, message: string, type: 'success' | 'error' } = { show: false, message: '', type: 'success' };
  private toastTimeout: any;

  /** Gimnasios presentes en los resultados, para poblar el filtro. */
  gimnasiosDisponibles = computed(() =>
    [...new Set(this.usuarios().map(u => u.gimnasioNombre).filter((g): g is string => !!g))]
  );

  hayFiltrosActivos = computed(() => {
    const f = this.filtrosActivos();
    return !!(f.nivel || f.objetivo || f.gimnasio || f.genero || f.edadMin || f.edadMax
      || f.rutina || f.soloSiPodemosCubrirnos || f.busqueda.trim());
  });

  usuariosFiltrados = computed(() => {
    let lista = this.usuarios();
    const f = this.filtrosActivos();

    if (f.busqueda.trim()) {
      const termino = f.busqueda.toLowerCase();
      lista = lista.filter(u => u.nombre.toLowerCase().includes(termino));
    }
    if (f.nivel) lista = lista.filter(u => u.nivel === f.nivel);
    if (f.objetivo) lista = lista.filter(u => u.objetivos === f.objetivo);
    if (f.gimnasio) lista = lista.filter(u => u.gimnasioNombre === f.gimnasio);
    if (f.genero) lista = lista.filter(u => u.genero === f.genero);
    if (f.edadMin !== null && f.edadMin > 0) lista = lista.filter(u => (u.edad ?? 0) >= f.edadMin!);
    if (f.edadMax !== null && f.edadMax > 0) lista = lista.filter(u => (u.edad ?? 0) <= f.edadMax!);
    if (f.rutina) lista = lista.filter(u => u.rutina === f.rutina);
    // Estrictamente true: null es "no se sabe", y aquí no vale.
    if (f.soloSiPodemosCubrirnos) lista = lista.filter(u => u.fuerzaCompatible === true);

    return lista;
  });

  /**
   * Cuánta gente de tu propio gimnasio hay.
   *
   * Lo dice el backend por id en `mismoGimnasio`; aquí no se comparan nombres.
   */
  enMiGimnasio = computed(() => this.usuarios().filter(u => u.mismoGimnasio).length);

  /**
   * Ser el primero de tu gimnasio no es lo mismo que no tener resultados.
   *
   * Hay gente en la lista, pero ninguna donde tú entrenas, y por eso todas
   * puntúan bajo: coincidir en un horario estando en dos edificios distintos de
   * la ciudad no es coincidir, y el motor descuenta ese solape a una cuarta
   * parte. Sin decirlo, la pantalla es una pared de números bajos sin motivo.
   */
  soyElPrimeroDeMiGimnasio = computed(
    () => this.usuarios().length > 0 && this.enMiGimnasio() === 0);

  /** Nadie todavía, que no es lo mismo que "tus filtros no dejan pasar a nadie". */
  comunidadVacia = computed(() => this.usuarios().length === 0);

  /**
   * Traer a alguien de tu gimnasio.
   *
   * Comparte el enlace de registro, sin código ni invitación registrada en la
   * base: eso pide una tabla, un token y una caducidad, y lo que resuelve el
   * arranque en frío es que puedas mandarle algo a tu compañero por WhatsApp.
   *
   * `navigator.share` en el móvil —que es donde se manda esto— y copiar al
   * portapapeles en el escritorio. El aviso se dice en el propio botón y no en
   * una alerta: una alerta hay que cerrarla y esto no merece un clic más.
   */
  protected textoInvitar = signal('Invitar a alguien de tu gimnasio');

  async invitar(): Promise<void> {
    const enlace = `${location.origin}/login`;
    const mensaje = `Estoy usando SpotterAI para no entrenar solo. Si te apuntas, `
      + `cruzamos horarios y vemos qué días coincidimos: ${enlace}`;

    try {
      if (navigator.share) {
        await navigator.share({ title: 'SpotterAI', text: mensaje, url: enlace });
        return;
      }
      await navigator.clipboard.writeText(mensaje);
      this.avisarEnElBoton('Enlace copiado');
    } catch {
      // Cancelar el diálogo de compartir entra por aquí y no es un error: no se
      // dice nada. Solo se avisa si de verdad no se ha podido copiar.
      if (!navigator.share) this.avisarEnElBoton('No se ha podido copiar');
    }
  }

  private avisarEnElBoton(texto: string): void {
    const original = 'Invitar a alguien de tu gimnasio';
    this.textoInvitar.set(texto);
    setTimeout(() => this.textoInvitar.set(original), 2500);
  }

  ngOnInit(): void {
    this.cargarComunidad();
    this.cargarMisHorarios();
  }

  /**
   * La gente, de la única lista que hay.
   *
   * Antes esto pedía /explorar, un segundo endpoint que devolvía a las mismas
   * personas por otro camino y ademas puntuadas sin levantamientos: la misma
   * pareja salía con 90 en el tablero y 83 aquí. Se borró aquel camino en vez de
   * vigilarlo.
   */
  cargarComunidad(): void {
    this.usuarioService.getMatches().subscribe({
      next: (data) => { this.usuarios.set(data || []); this.cdr.detectChanges(); },
      error: (err) => console.error('Error cargando la comunidad:', err)
    });
  }

  /** Sin mis horarios la rejilla no tiene fondo sobre el que dibujar el solape. */
  private cargarMisHorarios(): void {
    this.usuarioService.getMiPerfil().subscribe({
      next: (perfil) => { this.misHorarios.set(perfil?.horarios || []); this.cdr.detectChanges(); },
      error: (err) => console.error('Error cargando mis horarios:', err)
    });
  }

  mostrarToast(mensaje: string, tipo: 'success' | 'error' = 'success'): void {
    this.toast = { show: true, message: mensaje, type: tipo };
    this.cdr.detectChanges();

    if (this.toastTimeout) clearTimeout(this.toastTimeout);
    this.toastTimeout = setTimeout(() => {
      this.toast.show = false;
      this.cdr.detectChanges();
    }, 3000);
  }

  toggleFiltros(): void { this.isFiltrosOpen.update(v => !v); }

  actualizarFiltros(campo: string, evento: any): void {
    const valor = evento?.target ? evento.target.value : evento;
    this.filtrosActivos.update(f => ({ ...f, [campo]: valor }));
  }

  /** Los interruptores no traen valor en el evento: hay que leer el checked. */
  alternarFiltro(campo: string, evento: Event): void {
    const marcado = (evento.target as HTMLInputElement).checked;
    this.filtrosActivos.update(f => ({ ...f, [campo]: marcado }));
  }

  resetearFiltros(): void {
    this.filtrosActivos.set({
      busqueda: '', nivel: '', objetivo: '',
      gimnasio: '', genero: '', edadMin: null, edadMax: null,
      rutina: '', soloSiPodemosCubrirnos: false
    });
  }

  /** Tramo de compatibilidad, para no repetir umbrales en la plantilla. */
  tramo(puntuacion: number): 'alta' | 'media' | 'baja' {
    if (puntuacion >= 70) return 'alta';
    if (puntuacion >= 40) return 'media';
    return 'baja';
  }

  conectarConUsuario(id: number): void {
    this.usuarioService.enviarSolicitudConexion(id).subscribe({
      next: () => {
        this.mostrarToast('Solicitud enviada.');
        // Se queda en la lista, cambiando de estado. Antes desaparecia, que era
        // coherente cuando esta pantalla solo ensenaba a desconocidos; ahora
        // ensena a todo el mundo y hacer desaparecer a alguien al conectar seria
        // exactamente lo contrario de lo que acabas de hacer.
        this.usuarios.update(lista => lista.map(u =>
          u.id === id ? { ...u, solicitudPendiente: true } : u));
        this.cdr.detectChanges();
      },
      error: (err) => {
        // "Ya existe una solicitud", "No se puede conectar con esa persona"
        // (bloqueo)... un texto fijo obligaba a adivinar cuál de los dos era.
        this.mostrarToast(mensajeDeError(err, 'No se pudo enviar la solicitud.'), 'error');
      }
    });
  }

  /** Retira una solicitud enviada y deja a esa persona otra vez como candidata. */
  retirarSolicitud(usuario: Match): void {
    this.usuarioService.deshacerRelacion(usuario.id).subscribe({
      next: () => {
        this.usuarios.update(lista => lista.map(u =>
          u.id === usuario.id ? { ...u, solicitudPendiente: false } : u));
        this.mostrarToast(`Solicitud a ${usuario.nombre} retirada.`);
        this.cdr.detectChanges();
      },
      error: () => this.mostrarToast('No se ha podido retirar la solicitud.', 'error')
    });
  }

  // ================= Fichas de una en una =================

  isSugerenciasOpen = signal(false);

  /**
   * Con quien todavia no hay nada.
   *
   * El carrusel es para descubrir, asi que no tiene sentido pasear por gente con
   * la que ya has conectado o a la que ya has escrito.
   */
  sinContactar = computed(() =>
    this.usuarios().filter(u => !u.yaConectado && !u.solicitudPendiente));

  abrirSugerencias(): void {
    if (this.sinContactar().length === 0) {
      this.mostrarToast('No hay nadie nuevo por ahora.', 'error');
      return;
    }
    this.isSugerenciasOpen.set(true);
  }

  conectarDesdeLaFicha(usuarioId: number): void {
    this.conectarConUsuario(usuarioId);
    this.isSugerenciasOpen.set(false);
  }
}
