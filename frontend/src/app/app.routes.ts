import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { perfilGuard } from './guards/perfil.guard';

/*
 * Todas las pantallas se cargan bajo demanda.
 * ---------------------------------------------------------------------------
 * Ocho de las doce rutas se importaban arriba del fichero, o sea que entraban
 * en el paquete inicial. Y las ocho eran las gordas: el tablero, el chat,
 * Explorar y la ficha de una persona —que a su vez arrastra el desglose, la
 * rejilla, el avatar, los discos, proponer sesion y el formulario de perfil
 * entero—.
 *
 * En la practica eso significaba que quien abria /login se descargaba el
 * tablero, el chat, Explorar y el formulario de editar perfil ANTES de poder
 * escribir su correo. Y quien abre /login suele estar en un gimnasio, con datos
 * moviles, mirando una pantalla de dos campos.
 *
 * Los guardias siguen siendo la primera barrera: `canActivate` se evalua antes
 * de `loadComponent`, asi que a quien no ha iniciado sesion ni se le llega a
 * pedir el trozo de la pantalla que no puede ver.
 *
 * `u/:id` y `yo` apuntan al mismo import a proposito: es el mismo componente y
 * el empaquetador les da el mismo trozo, asi que navegar de una a otra no
 * descarga nada nuevo.
 */
export const routes: Routes = [
  // 🔓 Ruta pública (Cualquiera puede entrar a loguearse o registrarse)
  {
    path: 'login',
    loadComponent: () => import('./components/login/login').then(m => m.LoginComponent),
  },

  // Puerta de entrada: pide el horario, que es lo unico sin lo cual no se puede
  // emparejar a nadie. Lleva authGuard pero no perfilGuard, que si no seria un
  // bucle de redirecciones consigo misma.
  {
    path: 'bienvenida',
    canActivate: [authGuard],
    loadComponent: () => import('./components/bienvenida/bienvenida').then(m => m.BienvenidaComponent),
  },

  // 🔒 RUTAS BLINDADAS (Solo entras si el authGuard devuelve true)
  {
    path: 'dashboard',
    canActivate: [authGuard, perfilGuard],
    loadComponent: () => import('./components/dashboard/dashboard').then(m => m.DashboardComponent),
  },
  {
    path: 'solicitudes',
    canActivate: [authGuard, perfilGuard],
    loadComponent: () => import('./components/solicitudes/solicitudes').then(m => m.SolicitudesComponent),
  },
  {
    path: 'conexiones',
    canActivate: [authGuard, perfilGuard],
    loadComponent: () => import('./components/mis-conexiones/mis-conexiones').then(m => m.MisConexionesComponent),
  },
  {
    path: 'explorar',
    canActivate: [authGuard, perfilGuard],
    loadComponent: () => import('./components/explore/explore').then(m => m.Explore),
  },

  // Cada persona, un sitio. Antes la ficha era un panel flotante incrustado a la
  // vez en el tablero y en Explorar: no habia URL a la que enlazar, el boton
  // "atras" no hacia lo esperable y el mismo componente vivia en dos pantallas.
  {
    path: 'u/:id',
    canActivate: [authGuard, perfilGuard],
    loadComponent: () => import('./components/perfil-publico/perfil-publico').then(m => m.PerfilPublicoComponent),
  },

  // Tu propia pagina, que es donde te ves como te ven. Hasta ahora solo existia
  // editarte, que es otra cosa.
  {
    path: 'yo',
    canActivate: [authGuard, perfilGuard],
    loadComponent: () => import('./components/perfil-publico/perfil-publico').then(m => m.PerfilPublicoComponent),
  },

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

  // Los reportes, para quien está en AdminEmails. No hay guardia propia de rol
  // porque no hay roles en la aplicación: authGuard solo exige sesión, y quien
  // no es admin recibe un 404 del backend, que aquí se enseña como "sin acceso".
  {
    path: 'admin/reportes',
    canActivate: [authGuard],
    loadComponent: () => import('./components/admin-reportes/admin-reportes').then(m => m.AdminReportesComponent),
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
