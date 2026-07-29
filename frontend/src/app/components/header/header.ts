import { Component, signal, computed, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { TemaService } from '../../services/tema.service';
import { EventosService } from '../../services/eventos.service';
import { PerfilEstadoService } from '../../services/perfil-estado.service';
import { AvisosService } from '../../services/avisos.service';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true, // Asegúrate de que sea standalone si lo estabas usando así
  imports: [CommonModule, RouterModule],
  templateUrl: './header.html',
  styleUrl: './header.scss'
})
export class Header implements OnInit {
  isSidebarOpen = signal(false);
  isLoggedIn = signal(true);
  
  // Inyectamos el router para poder redirigir al login
  private router = inject(Router);
  private tema = inject(TemaService);
  private eventos = inject(EventosService);
  private perfilEstado = inject(PerfilEstadoService);
  private avisos = inject(AvisosService);
  private auth = inject(AuthService);

  solicitudesPendientes = this.avisos.solicitudesPendientes;
  mensajesSinLeer = this.avisos.mensajesSinLeer;

  /*
   * Cada indicador cuenta solo lo que hay donde lleva. Al principio la campana
   * sumaba solicitudes y mensajes, y quedaba un "1" que te mandaba a Solicitudes
   * cuando lo pendiente era un mensaje. Los mensajes se avisan en Compañeros,
   * que es donde se contestan.
   */
  /**
   * Todo lo que espera tu respuesta: solicitudes y propuestas de sesión.
   *
   * Las dos cosas juntas porque son la misma clase de cosa, y las dos viven en
   * /solicitudes, que es a donde lleva la campana. Contar aquí algo que allí no
   * estuviera sería mandar a alguien a una pantalla donde no hay nada que hacer.
   */
  porResponder = computed(() =>
    this.avisos.solicitudesPendientes() + this.avisos.sesionesPendientes());

  etiquetaAvisos = computed(() => {
    const pendientes = this.porResponder();
    if (pendientes === 0) return 'No hay nada pendiente';
    return `${pendientes} ${pendientes === 1 ? 'cosa pendiente' : 'cosas pendientes'} por responder`;
  });

  temaActual = this.tema.tema;

  alternarTema(): void {
    this.tema.alternar();
  }

  // Variables dinámicas para el usuario
  currentUser = signal({
    name: 'Usuario',
    email: '',
    initial: 'U'
  });

  ngOnInit() {
    // 🔥 Leemos los datos reales del usuario logueado
    const nombre = localStorage.getItem('usuario_nombre') || 'Usuario';
    const email = localStorage.getItem('usuario_email') || '';
    
    this.currentUser.set({
      name: nombre,
      email: email,
      initial: nombre.charAt(0).toUpperCase()
    });
  }

  toggleSidebar() {
    this.isSidebarOpen.update(isOpen => !isOpen);
  }

  // 🔥 NUEVO: Función real para cerrar sesión
  cerrarSesion() {
    // 0. Cerramos el canal de eventos antes de borrar el token: si no, seguiría
    //    reintentando conectar en bucle contra un backend que ya nos rechaza.
    this.eventos.desconectar();
    this.avisos.limpiar();

    // Sin esto, el siguiente que entrase en este navegador heredaria el "ya tiene
    // horario" del anterior y se saltaria la bienvenida.
    this.perfilEstado.olvidar();

    // 1. Se lo pedimos al servidor. La sesión vive en una galleta HttpOnly que
    //    este código no puede borrar: limpiar localStorage y ya, como se hacía
    //    antes, dejaría la sesión abierta con la interfaz diciendo que no lo
    //    está. Y el nombre lo dice —"cerrar sesión"—, así que tiene que cerrarla.
    //    Se navega igual si la llamada falla: quien pulsa esto quiere salir.
    this.auth.logout().subscribe({
      next: () => this.salir(),
      error: () => this.salir(),
    });
  }

  private salir(): void {
    this.isLoggedIn.set(false);
    this.isSidebarOpen.set(false);
    this.router.navigate(['/login']); 
  }
}