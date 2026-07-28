import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { PerfilEstadoService } from '../services/perfil-estado.service';

/**
 * Sin lo mínimo no se pasa.
 *
 * Antes solo miraba el horario. Ahora mira todo lo que el motor necesita
 * declarado: sin ello, un perfil no se puede emparejar de verdad, solo ocupa
 * sitio en la lista con una puntuación calculada sobre tres factores que no se
 * puede comparar con la de nadie.
 *
 * Qué es "lo mínimo" lo decide el backend, en PerfilMinimo. Aquí solo se
 * pregunta.
 *
 * Va después del authGuard y cubre también a quien ya estaba registrado antes
 * de que esto fuera obligatorio, que es la mitad del problema: exigirlo solo en
 * el registro dejaría fuera para siempre a todos los perfiles existentes.
 */
export const perfilGuard: CanActivateFn = () => {
  const perfilEstado = inject(PerfilEstadoService);
  const router = inject(Router);

  return perfilEstado.comprobar().pipe(
    map(completo => completo ? true : router.createUrlTree(['/bienvenida']))
  );
};
