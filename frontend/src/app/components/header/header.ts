import { Component, signal, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { TemaService } from '../../services/tema.service';
import { EventosService } from '../../services/eventos.service';
import { PerfilEstadoService } from '../../services/perfil-estado.service';
import { AvisosService } from '../../services/avisos.service';
import { AuthService } from '../../services/auth.service';
import { IdiomaService } from '../../services/idioma.service';
import { UsuarioService } from '../../services/usuario.service';
import { Avatar } from '../avatar/avatar';

@Component({
  selector: 'app-header',
  standalone: true, // Asegúrate de que sea standalone si lo estabas usando así
  imports: [CommonModule, RouterModule, Avatar],
  templateUrl: './header.html',
  styleUrl: './header.scss'
})
export class Header {
  isSidebarOpen = signal(false);

  // Inyectamos el router para poder redirigir al login
  private router = inject(Router);
  private tema = inject(TemaService);
  private eventos = inject(EventosService);
  private perfilEstado = inject(PerfilEstadoService);
  private avisos = inject(AvisosService);
  private usuarios = inject(UsuarioService);
  /** protected: el sidebar lee auth.usuario() directamente en la plantilla. */
  protected auth = inject(AuthService);
  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);

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
    return pendientes === 0
      ? this.i18n.t('cabecera.sinAvisos')
      : this.i18n.t('cabecera.avisos', { cuenta: pendientes });
  });

  temaActual = this.tema.tema;

  alternarTema(): void {
    this.tema.alternar();
  }

  idiomaActual = this.i18n.idioma;

  /**
   * Cambia el idioma de la pantalla y deja apuntado el de los correos.
   *
   * <p>Lo segundo va al servidor porque los avisos por correo se mandan cuando
   * no hay ninguna petición de la que sacar el idioma. Y va sin esperar
   * respuesta: la pantalla ya ha cambiado, y un error aquí no es algo que la
   * persona pueda arreglar ni tenga que saber.
   */
  alternarIdioma(): void {
    this.i18n.alternar();
    this.usuarios.guardarIdioma(this.i18n.idioma()).subscribe({ error: () => {} });
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
    this.isSidebarOpen.set(false);
    this.router.navigate(['/login']); 
  }
}