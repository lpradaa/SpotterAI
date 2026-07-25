import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { PerfilEstadoService } from '../services/perfil-estado.service';

/**
 * Sin horario no se pasa.
 *
 * El horario vale el 40% de la compatibilidad y es la única forma de saber si
 * dos personas pueden coincidir en el gimnasio: un perfil sin él no se puede
 * emparejar, solo ocupa sitio en la lista.
 *
 * Va después del authGuard y cubre también a quien ya estaba registrado antes
 * de que esto fuera obligatorio, que es la mitad del problema: obligar solo en
 * el registro dejaría fuera para siempre a todos los perfiles existentes.
 */
export const horarioGuard: CanActivateFn = () => {
  const perfilEstado = inject(PerfilEstadoService);
  const router = inject(Router);

  return perfilEstado.comprobar().pipe(
    map(tieneHorario => tieneHorario ? true : router.createUrlTree(['/bienvenida']))
  );
};
