import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { PerfilEstadoService } from '../services/perfil-estado.service';

/** Rutas donde un 401 significa "credenciales incorrectas", no "sesion caducada". */
const RUTAS_PUBLICAS = ['/api/auth/login', '/api/usuarios/registro'];

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const perfilEstado = inject(PerfilEstadoService);

  // Buscamos el token en el almacenamiento del navegador
  const token = localStorage.getItem('token');

  // Si el token existe, clonamos la petición y le inyectamos la cabecera Authorization
  const peticion = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      /*
       * El guardian solo mira la caducidad al navegar. Si el token muere con la
       * aplicacion ya abierta, sin esto cada peticion falla por su cuenta y el
       * usuario se queda mirando pantallas vacias sin saber que ha pasado.
       *
       * Se excluyen login y registro porque ahi un 401 es "contrasena
       * incorrecta", y echar a alguien al login desde el login no arregla nada.
       */
      const esPublica = RUTAS_PUBLICAS.some(ruta => req.url.includes(ruta));

      if (!esPublica && (error.status === 401 || error.status === 403)) {
        authService.logout();
        // Si no, el siguiente que entre en este navegador hereda el "ya tiene
        // horario" del anterior y se salta la bienvenida.
        perfilEstado.olvidar();
        router.navigate(['/login'], { queryParams: { sesion: 'caducada' } });
      }

      return throwError(() => error);
    })
  );
};
