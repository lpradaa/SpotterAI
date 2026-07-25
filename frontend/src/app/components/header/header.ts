import { Component, signal, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { TemaService } from '../../services/tema.service';
import { EventosService } from '../../services/eventos.service';
import { PerfilEstadoService } from '../../services/perfil-estado.service';

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

    // Sin esto, el siguiente que entrase en este navegador heredaria el "ya tiene
    // horario" del anterior y se saltaria la bienvenida.
    this.perfilEstado.olvidar();

    // 1. Borramos el token y todos los datos de sesión del navegador
    localStorage.clear();
    
    // 2. Cerramos el panel y actualizamos el estado
    this.isLoggedIn.set(false);
    this.isSidebarOpen.set(false);
    
    // 3. Redirigimos a la pantalla de login
    this.router.navigate(['/login']); 
  }
}