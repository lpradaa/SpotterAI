import { IdiomaService } from '../../services/idioma.service';
import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { EventosService } from '../../services/eventos.service';
import { AvisosService } from '../../services/avisos.service';
import { HttpClient } from '@angular/common/http';
import { api } from '../../config/api';
import { mensajeDeError } from '../../utils/errores';
import { MINIMO_CONTRASENA } from '../restablecer/restablecer';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss' // OJO: he cambiado .css a .scss para que coincida con tu archivo
})
export class LoginComponent implements OnInit {
  private authService = inject(AuthService);
  /** protected: la plantilla llama a i18n.t() en cada texto. */
  protected i18n = inject(IdiomaService);
  private router = inject(Router);
  private eventos = inject(EventosService);
  private avisos = inject(AvisosService);
  private route = inject(ActivatedRoute);
  private http = inject(HttpClient);

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

  /** La misma regla que exige el backend, para poder decirla antes de fallar. */
  protected minimoContrasena = MINIMO_CONTRASENA;

  protected get contrasenaCorta(): boolean {
    return this.password.length > 0 && this.password.length < this.minimoContrasena;
  }

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

  protected olvidoEnviando = signal(false);
  protected olvidoPedido = signal(false);

  /**
   * Pedir el enlace para recuperar la contraseña.
   *
   * <p>Se usa el correo que ya está escrito en el formulario: si has llegado a
   * pulsar esto es porque has probado a entrar, así que pedirlo otra vez en otra
   * pantalla es un paso de más justo cuando la persona ya está molesta.
   *
   * <p>El mensaje es el mismo exista la cuenta o no, y el botón se sustituye por
   * él pase lo que pase. Cualquier otra cosa —"no encontramos ese correo"—
   * convertiría esto en un comprobador de quién está registrado.
   */
  protected olvideLaContrasena(): void {
    const email = (this.email || '').trim();
    if (!email) {
      this.errorMessage.set('Escribe tu correo y vuelve a pulsar.');
      return;
    }

    this.olvidoEnviando.set(true);
    this.errorMessage.set(null);

    this.http.post(api('/api/auth/olvide'), { email }).subscribe({
      next: () => { this.olvidoEnviando.set(false); this.olvidoPedido.set(true); },
      // Tampoco al fallar: un error distinto seguiría delatando la cuenta.
      error: () => { this.olvidoEnviando.set(false); this.olvidoPedido.set(true); },
    });
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

        // El 429 es del freno a la fuerza bruta. Sin distinguirlo, quien se ha
        // equivocado cinco veces seguiría leyendo "contraseña incorrecta" y
        // probando la buena una y otra vez sin entender por qué no entra.
        if (err?.status === 429) {
          this.errorMessage.set(typeof err.error === 'string'
            ? err.error
            : 'Demasiados intentos. Espera un rato antes de volver a probar.');
          return;
        }

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
        this.isLoading.set(false);
        // El backend ya dice el motivo concreto —correo repetido, contraseña
        // demasiado corta, edad fuera de rango—; mostrar siempre el mismo
        // texto genérico obligaba a adivinar qué campo revisar.
        this.errorMessage.set(mensajeDeError(err, 'Hubo un problema al crear la cuenta.'));
      }
    });
  }
}