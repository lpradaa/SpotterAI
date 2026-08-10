import { Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { api } from '../../config/api';

/** Lo mismo que exige el backend. Duplicarlo aquí es para avisar antes, no para decidir. */
export const MINIMO_CONTRASENA = 12;

/**
 * Poner una contraseña nueva con el enlace del correo.
 *
 * <p>Sin sesión, como la pantalla de baja: quien ha olvidado su contraseña no
 * puede iniciar sesión, que es justamente el problema.
 *
 * <p>La comprobación de longitud está también aquí, y eso no es duplicar la
 * regla: quien decide sigue siendo el backend —esta pantalla no puede
 * garantizar nada—. Lo que hace es avisar antes de gastar el enlace, porque el
 * token es de un solo uso y quedarse sin él por escribir algo corto sería
 * quedarse sin cuenta.
 */
@Component({
  selector: 'app-restablecer',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './restablecer.html',
  styleUrl: './restablecer.scss',
})
export class RestablecerComponent {
  private http = inject(HttpClient);
  private ruta = inject(ActivatedRoute);
  private router = inject(Router);

  private token = this.ruta.snapshot.queryParamMap.get('t') ?? '';

  protected minimo = MINIMO_CONTRASENA;
  protected sinEnlace = !this.token;
  protected contrasena = '';
  protected enviando = signal(false);
  protected error = signal<string | null>(null);
  protected hecho = signal(false);

  protected get demasiadoCorta(): boolean {
    return this.contrasena.length > 0 && this.contrasena.length < MINIMO_CONTRASENA;
  }

  protected guardar(): void {
    if (this.contrasena.length < MINIMO_CONTRASENA) return;

    this.enviando.set(true);
    this.error.set(null);

    this.http.post(api('/api/auth/restablecer'), { token: this.token, password: this.contrasena })
      .subscribe({
        next: () => {
          this.hecho.set(true);
          this.enviando.set(false);
          // Al login y no dentro: la contraseña nueva hay que estrenarla, y
          // entrar solo demostraría que el enlace funcionaba.
          setTimeout(() => this.router.navigate(['/login']), 2500);
        },
        error: (e) => {
          this.enviando.set(false);
          this.error.set(e?.error?.error ?? 'No se ha podido cambiar la contraseña.');
        },
      });
  }
}
