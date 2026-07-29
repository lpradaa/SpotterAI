import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { PerfilEstadoService } from '../services/perfil-estado.service';

/** Rutas donde un 401 significa "credenciales incorrectas", no "sesion caducada". */
const RUTAS_PUBLICAS = ['/api/auth/login', '/api/usuarios/registro'];

/** Métodos que no cambian nada y por tanto no necesitan la defensa contra CSRF. */
const SOLO_LEEN = ['GET', 'HEAD', 'OPTIONS'];

/**
 * Lee una galleta por su nombre.
 *
 * La del CSRF sí se puede leer desde JavaScript, y tiene que poder: la defensa
 * consiste precisamente en que esta página la copie a una cabecera, porque otro
 * sitio no puede leer nuestras galletas para hacer lo mismo. La de sesión, la
 * que de verdad importa, es `HttpOnly` y desde aquí no existe.
 */
function galleta(nombre: string): string | null {
  const encontrada = document.cookie
    .split('; ')
    .find(c => c.startsWith(`${nombre}=`));

  return encontrada ? decodeURIComponent(encontrada.slice(nombre.length + 1)) : null;
}

/**
 * Lleva la sesión y la defensa contra CSRF en cada petición.
 *
 * <p>Ya no añade ninguna cabecera `Authorization`: el token vive en una galleta
 * `HttpOnly` que este código no puede leer, y el navegador la manda solo. Lo que
 * sí hace falta es `withCredentials`, porque en desarrollo la aplicación
 * (:4200) y la API (:8080) son orígenes distintos y sin eso el navegador no
 * manda galletas.
 *
 * <p>Y copia el token CSRF de su galleta a la cabecera. Angular trae un
 * interceptor que hace esto, pero **solo para peticiones del mismo origen**, así
 * que en desarrollo no haría nada y todo lo que escribe daría 403. Hacerlo aquí
 * funciona igual en los dos casos.
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const perfilEstado = inject(PerfilEstadoService);

  const cabeceras: Record<string, string> = {};

  const csrf = galleta('XSRF-TOKEN');
  if (csrf && !SOLO_LEEN.includes(req.method)) {
    cabeceras['X-XSRF-TOKEN'] = csrf;
  }

  const peticion = req.clone({ withCredentials: true, setHeaders: cabeceras });

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      /*
       * El guardian solo mira la caducidad al navegar. Si la sesion muere con la
       * aplicacion ya abierta, sin esto cada peticion falla por su cuenta y el
       * usuario se queda mirando pantallas vacias sin saber que ha pasado.
       *
       * Se excluyen login y registro porque ahi un 401 es "contrasena
       * incorrecta", y echar a alguien al login desde el login no arregla nada.
       */
      const esPublica = RUTAS_PUBLICAS.some(ruta => req.url.includes(ruta));

      if (!esPublica && (error.status === 401 || error.status === 403)) {
        authService.olvidarSesion();
        // Si no, el siguiente que entre en este navegador hereda el "ya tiene
        // horario" del anterior y se salta la bienvenida.
        perfilEstado.olvidar();
        router.navigate(['/login'], { queryParams: { sesion: 'caducada' } });
      }

      return throwError(() => error);
    })
  );
};
