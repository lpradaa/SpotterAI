import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Se mira antes de preguntar: isAuthenticated() borra el token si esta
  // caducado, asi que despues ya no se puede distinguir "se le acabo la sesion"
  // de "nunca entro".
  const habiaToken = localStorage.getItem('token') !== null;

  if (authService.isAuthenticated()) {
    return true; // Le abrimos la puerta, puede pasar a la pantalla
  }

  // Se avisa igual que hace el interceptor: aparecer en el login sin ninguna
  // explicacion se lee como que algo se ha roto.
  return router.createUrlTree(['/login'], {
    queryParams: habiaToken ? { sesion: 'caducada' } : {}
  });
};
