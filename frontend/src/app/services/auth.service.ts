import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, of, tap } from 'rxjs';
import { api } from '../config/api';

/** Lo que el login devuelve ahora. Sin token: ese va en la galleta. */
interface RespuestaLogin {
  id: number;
  email: string;
  nombre: string;
  /** Color de identidad: para que el menú lateral pinte a la persona como el resto de la app. */
  avatar: string | null;
  /** Milisegundos desde 1970, comparable con Date.now(). */
  expiraEn: number;
}

/**
 * La sesión, que ya no vive aquí.
 *
 * <p>El token estaba en `localStorage`, o sea al alcance de cualquier script
 * que se ejecutara en la página. Ahora está en una galleta `HttpOnly` que este
 * código no puede leer ni escribir, y de la que solo el servidor puede
 * deshacerse.
 *
 * <p>Lo que queda guardado aquí no son credenciales: el nombre, el id, el correo
 * y **cuándo caduca la sesión**. Nada de eso permite iniciar una; con todo ello
 * copiado a otro navegador no se entra. Sirve para lo mismo que antes se sacaba
 * leyendo el token: no montar la aplicación entera con una sesión ya muerta y
 * dejar al usuario mirando pantallas vacías sin explicación.
 *
 * <p>Quien decide de verdad sigue siendo el backend, que valida la firma. Esto
 * es comodidad, no seguridad — y ahora se nota más, porque el navegador puede
 * tener la galleta caducada y estos datos no, o al revés.
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private URL_AUTH = api('/api/auth');

  /** Quién está dentro, o null. Reactivo para que la cabecera se entere. */
  usuario = signal<{ id: number; nombre: string; email: string; avatar: string } | null>(this.leerGuardado());

  login(credenciales: { email: string; password: string }): Observable<RespuestaLogin> {
    return this.http.post<RespuestaLogin>(`${this.URL_AUTH}/login`, credenciales).pipe(
      tap(respuesta => {
        if (!respuesta) return;

        localStorage.setItem('usuario_nombre', respuesta.nombre ?? '');
        localStorage.setItem('usuario_email', respuesta.email ?? '');
        localStorage.setItem('usuario_avatar', respuesta.avatar ?? '');
        localStorage.setItem('userId', String(respuesta.id));
        localStorage.setItem('sesion_expira', String(respuesta.expiraEn));

        this.usuario.set({ id: respuesta.id, nombre: respuesta.nombre, email: respuesta.email,
                          avatar: respuesta.avatar ?? '' });
      })
    );
  }

  register(nuevoUsuario: any): Observable<any> {
    return this.http.post<any>(api('/api/usuarios/registro'), nuevoUsuario);
  }

  /**
   * Cerrar sesión de verdad.
   *
   * <p>Antes bastaba con olvidarse del token, porque lo teníamos nosotros. Ahora
   * la galleta la guarda el navegador y solo el servidor puede borrarla, así que
   * **hay que pedírselo**: limpiar `localStorage` sin llamar aquí dejaría la
   * sesión abierta con la interfaz diciendo que no lo está.
   *
   * <p>Los datos locales se borran pase lo que pase con la llamada. Si el
   * servidor no responde, lo que el usuario quiere sigue siendo salir.
   */
  logout(): Observable<unknown> {
    return this.http.post(`${this.URL_AUTH}/logout`, {}).pipe(
      catchError(() => of(null)),
      tap(() => this.olvidarSesion())
    );
  }

  /** Borra lo local. No cierra la sesión del servidor: para eso está `logout()`. */
  olvidarSesion(): void {
    localStorage.removeItem('usuario_nombre');
    localStorage.removeItem('usuario_email');
    localStorage.removeItem('usuario_avatar');
    localStorage.removeItem('userId');
    localStorage.removeItem('sesion_expira');
    this.usuario.set(null);
  }

  /**
   * Hay sesión utilizable, hasta donde se puede saber desde aquí.
   *
   * <p>Ya no se lee la caducidad del token —no lo tenemos— sino la que el login
   * dijo. Si no consta, se da por buena y que responda el backend: bloquear al
   * usuario por un dato que nos falta es peor que dejar que llegue un 401.
   */
  isAuthenticated(): boolean {
    if (!this.usuario()) return false;

    const expira = Number(localStorage.getItem('sesion_expira'));
    if (!expira) return true;

    // Margen para no entrar con una sesión que muere a los dos segundos y deja
    // la aplicación a medias.
    const MARGEN_MS = 10_000;
    if (expira - MARGEN_MS <= Date.now()) {
      this.olvidarSesion();
      return false;
    }
    return true;
  }

  private leerGuardado(): { id: number; nombre: string; email: string; avatar: string } | null {
    const id = Number(localStorage.getItem('userId'));
    if (!id) return null;

    return {
      id,
      nombre: localStorage.getItem('usuario_nombre') ?? '',
      email: localStorage.getItem('usuario_email') ?? '',
      avatar: localStorage.getItem('usuario_avatar') ?? '',
    };
  }
}
