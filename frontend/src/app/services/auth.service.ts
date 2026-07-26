import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { api } from '../config/api';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private URL_AUTH = api('/api/auth'); 

  // Almacén reactivo del Token recuperado del almacenamiento del navegador
  currentUserToken = signal<string | null>(localStorage.getItem('token'));

  login(credenciales: { email: string; password: string }): Observable<any> {
    return this.http.post<any>(`${this.URL_AUTH}/login`, credenciales).pipe(
      tap(response => {
        if (response && response.token) {
          localStorage.setItem('token', response.token);
          
          if (response.nombre) {
            localStorage.setItem('usuario_nombre', response.nombre);
          }

          // Guardamos el ID del usuario
          const userId = response.id || response.usuarioId; 
          if (userId) {
            localStorage.setItem('userId', userId.toString());
          }

          this.currentUserToken.set(response.token);
        }
      })
    );
  }

  // 🔥 MODIFICADO: Ahora apunta al endpoint correcto del backend para registrar usuarios
  // Fíjate en la última palabra de la URL (/registro)
  register(nuevoUsuario: any): Observable<any> {
    return this.http.post<any>(api('/api/usuarios/registro'), nuevoUsuario);
  }

  logout(): void {
    localStorage.removeItem('token');
    localStorage.removeItem('usuario_nombre');
    localStorage.removeItem('userId'); 
    this.currentUserToken.set(null);
  }

  /**
   * Hay sesión utilizable, no solo un token guardado.
   *
   * Comprobar unicamente que exista dejaba entrar con un token caducado: la
   * aplicacion se montaba entera y luego fallaban todas las llamadas en
   * silencio, asi que lo que veias eran pantallas vacias sin ninguna
   * explicacion. Esto es comodidad, no seguridad: quien decide sigue siendo el
   * backend, que valida la firma.
   */
  isAuthenticated(): boolean {
    const token = this.currentUserToken();
    if (!token) return false;

    if (this.haCaducado(token)) {
      this.logout(); // no tiene sentido conservar un token muerto
      return false;
    }
    return true;
  }

  private haCaducado(token: string): boolean {
    const caduca = this.caducidadDe(token);

    // Un token cuya caducidad no se puede leer se da por bueno: puede ser un
    // formato que no esperabamos, y bloquear al usuario por eso seria peor que
    // dejar que el backend responda 401.
    if (caduca === null) return false;

    // Margen para no entrar con un token que muere a los dos segundos y deja la
    // sesion a medias.
    const MARGEN_MS = 10_000;
    return caduca - MARGEN_MS <= Date.now();
  }

  /** Milisegundos de la caducidad del JWT, o null si no se puede leer. */
  private caducidadDe(token: string): number | null {
    try {
      const carga = token.split('.')[1];
      if (!carga) return null;

      // base64url -> base64, y de ahi a texto respetando UTF-8: el correo del
      // usuario viaja dentro y atob() por si solo destroza los acentos.
      const base64 = carga.replace(/-/g, '+').replace(/_/g, '/');
      const bytes = Uint8Array.from(atob(base64), c => c.charCodeAt(0));
      const datos = JSON.parse(new TextDecoder().decode(bytes));

      return typeof datos.exp === 'number' ? datos.exp * 1000 : null;
    } catch {
      return null;
    }
  }
}