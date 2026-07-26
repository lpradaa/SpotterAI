import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { EventosService } from '../../services/eventos.service';
import { AvisosService } from '../../services/avisos.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss' // OJO: he cambiado .css a .scss para que coincida con tu archivo
})
export class LoginComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private eventos = inject(EventosService);
  private avisos = inject(AvisosService);
  private route = inject(ActivatedRoute);

  ngOnInit(): void {
    // El interceptor manda aquí cuando el token muere con la app abierta. Sin
    // este aviso, la persona aparece en el login sin saber por qué y cree que
    // algo se ha roto.
    if (this.route.snapshot.queryParamMap.get('sesion') === 'caducada') {
      this.errorMessage.set('Tu sesión ha caducado. Vuelve a entrar.');
    }
  }

  // 🔄 Interruptor de modo (Login vs Registro)
  isLoginMode = true;

  // ⏳ Estado de carga para dar feedback visual al botón
  isLoading = signal(false);

  // Variables bindeadas al formulario
  email = '';
  password = ''; 
  nombre = '';
  edad: number | null = null;
  genero = '';
  peso: number | null = null;

  // Signal para manejar los errores visuales de la interfaz
  errorMessage = signal<string | null>(null);

  // Alternar entre pantallas
  toggleMode(): void {
    this.isLoginMode = !this.isLoginMode;
    this.errorMessage.set(null); 
  }

  // Función principal que decide qué hacer al darle al botón
  onSubmit(): void {
    this.errorMessage.set(null);
    this.isLoading.set(true); // Bloqueamos el botón

    if (this.isLoginMode) {
      this.ejecutarLogin();
    } else {
      this.ejecutarRegistro();
    }
  }

  private ejecutarLogin(): void {
    const credenciales = { email: this.email, password: this.password };

    this.authService.login(credenciales).subscribe({
      next: (response) => {
        console.log('¡Login correcto! Token guardado de forma segura.');
        this.isLoading.set(false);
        // Ahora sí hay token: el canal de eventos ya puede abrirse. En el
        // arranque de la app no pudo, porque aún no habíamos entrado.
        this.eventos.conectar();
        this.avisos.iniciar();
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        console.error('Error en el login:', err);
        this.isLoading.set(false);
        this.errorMessage.set('Email o contraseña incorrectos. Inténtalo de nuevo.');
      }
    });
  }

  private ejecutarRegistro(): void {
    const nuevoUsuario = {
      nombre: this.nombre,
      email: this.email,
      password: this.password,
      edad: this.edad,
      genero: this.genero,
      peso: this.peso
    };

    this.authService.register(nuevoUsuario).subscribe({
      next: (response) => {
        console.log('¡Cuenta creada con éxito! Iniciando sesión automáticamente...');
        this.ejecutarLogin(); // Redirige al login automáticamente
      },
      error: (err) => {
        console.error('Error en el registro:', err);
        this.isLoading.set(false);
        this.errorMessage.set('Hubo un problema al crear la cuenta. Revisa los datos.');
      }
    });
  }
}