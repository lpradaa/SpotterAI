import { Component, signal, computed, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UsuarioService, Match } from '../../services/usuario.service';
import { RejillaSemana, Franja } from '../rejilla-semana/rejilla-semana';
import { Avatar } from '../avatar/avatar';
import { PerfilPublicoComponent } from '../perfil-publico/perfil-publico';
import { Carga } from '../carga/carga';
import { PerfilesService } from '../../services/perfiles.service';
import { ModalAccesible } from '../../directivas/modal-accesible';

@Component({
  selector: 'app-explore',
  standalone: true,
  imports: [CommonModule, FormsModule, RejillaSemana, Avatar, PerfilPublicoComponent, Carga, ModalAccesible],
  templateUrl: './explore.html',
  styleUrl: './explore.scss'
})
export class Explore implements OnInit {
  private usuarioService = inject(UsuarioService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);
  private perfiles = inject(PerfilesService);

  /** A quién se está mirando, o null. */
  perfilAbierto = signal<number | null>(null);

  verPerfil(usuarioId: number): void {
    this.perfilAbierto.set(usuarioId);
  }

  cerrarPerfil(): void {
    this.perfilAbierto.set(null);
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

  ngOnInit(): void {
    this.cargarComunidad();
    this.cargarMisHorarios();
  }

  cargarComunidad(): void {
    this.usuarioService.getExplorarUsuarios().subscribe({
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
        // Sale de la lista: explorar muestra solo a quien no has escrito todavía
        this.usuarios.update(lista => lista.filter(u => u.id !== id));
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error(err);
        this.mostrarToast('No se pudo enviar la solicitud.', 'error');
      }
    });
  }
}
