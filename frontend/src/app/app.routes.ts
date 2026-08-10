import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login'; 
import { DashboardComponent } from './components/dashboard/dashboard'; 
import { SolicitudesComponent } from './components/solicitudes/solicitudes';
import { MisConexionesComponent } from './components/mis-conexiones/mis-conexiones';
import { Explore } from './components/explore/explore';
import { BienvenidaComponent } from './components/bienvenida/bienvenida';
import { PerfilPublicoComponent } from './components/perfil-publico/perfil-publico';
import { authGuard } from './guards/auth.guard'; // 🔥 Importamos el cerrojo de seguridad
import { perfilGuard } from './guards/perfil.guard';

export const routes: Routes = [
  // 🔓 Ruta pública (Cualquiera puede entrar a loguearse o registrarse)
  { path: 'login', component: LoginComponent },

  // Puerta de entrada: pide el horario, que es lo unico sin lo cual no se puede
  // emparejar a nadie. Lleva authGuard pero no perfilGuard, que si no seria un
  // bucle de redirecciones consigo misma.
  { path: 'bienvenida', component: BienvenidaComponent, canActivate: [authGuard] },

  // 🔒 RUTAS BLINDADAS (Solo entras si el authGuard devuelve true)
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard, perfilGuard] },
  { path: 'solicitudes', component: SolicitudesComponent, canActivate: [authGuard, perfilGuard] },
  { path: 'conexiones', component: MisConexionesComponent, canActivate: [authGuard, perfilGuard] },
  { path: 'explorar', component: Explore, canActivate: [authGuard, perfilGuard] },

  // Cada persona, un sitio. Antes la ficha era un panel flotante incrustado a la
  // vez en el tablero y en Explorar: no habia URL a la que enlazar, el boton
  // "atras" no hacia lo esperable y el mismo componente vivia en dos pantallas.
  { path: 'u/:id', component: PerfilPublicoComponent, canActivate: [authGuard, perfilGuard] },

  // Tu propia pagina, que es donde te ves como te ven. Hasta ahora solo existia
  // editarte, que es otra cosa.
  { path: 'yo', component: PerfilPublicoComponent, canActivate: [authGuard, perfilGuard] },
  
  // Los mensajes viven en /conexiones. Antes habia ademas /chat/:id con una
  // segunda implementacion copiada, que se ha eliminado: se llega igual con
  // /conexiones?con=ID y solo hay un sitio que mantener.
  { path: 'chat/:id', redirectTo: 'conexiones', pathMatch: 'full' },

  // Si el motor acierta. Fuera de la navegacion principal a proposito: no es
  // una pantalla que le sirva a nadie para entrenar, es la que dice si lo que
  // hace el resto de la aplicacion esta justificado. Se llega desde el pie.
  {
    path: 'embudo',
    canActivate: [authGuard],
    loadComponent: () => import('./components/embudo/embudo').then(m => m.EmbudoComponent),
  },

  // Dejar de recibir avisos por correo. La unica pantalla sin authGuard aparte
  // del login, y a proposito: quien quiere dejar de recibir correos no va a
  // iniciar sesion para conseguirlo. Lo que hace es marcar el remitente como no
  // deseado, y entonces tampoco le llegan los avisos que si queria.
  {
    path: 'baja',
    loadComponent: () => import('./components/baja/baja').then(m => m.BajaComponent),
  },

  // Poner una contraseña nueva con el enlace del correo. Sin authGuard por lo
  // mismo que /baja: quien ha olvidado su contraseña no puede iniciar sesion,
  // que es exactamente el problema que viene a resolver.
  {
    path: 'restablecer',
    loadComponent: () => import('./components/restablecer/restablecer').then(m => m.RestablecerComponent),
  },

  // Redirecciones por defecto
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  
  // 🚨 EL COMODÍN DEBE SER SIEMPRE LA ÚLTIMA LÍNEA DEL ARRAY 🚨
  { path: '**', redirectTo: '/dashboard' } 
];