import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login'; 
import { DashboardComponent } from './components/dashboard/dashboard'; 
import { SolicitudesComponent } from './components/solicitudes/solicitudes';
import { MisConexionesComponent } from './components/mis-conexiones/mis-conexiones';
import { Explore } from './components/explore/explore';
import { BienvenidaComponent } from './components/bienvenida/bienvenida';
import { PerfilPublicoComponent } from './components/perfil-publico/perfil-publico';
import { authGuard } from './guards/auth.guard'; // 🔥 Importamos el cerrojo de seguridad
import { horarioGuard } from './guards/horario.guard';

export const routes: Routes = [
  // 🔓 Ruta pública (Cualquiera puede entrar a loguearse o registrarse)
  { path: 'login', component: LoginComponent },

  // Puerta de entrada: pide el horario, que es lo unico sin lo cual no se puede
  // emparejar a nadie. Lleva authGuard pero no horarioGuard, que si no seria un
  // bucle de redirecciones consigo misma.
  { path: 'bienvenida', component: BienvenidaComponent, canActivate: [authGuard] },

  // 🔒 RUTAS BLINDADAS (Solo entras si el authGuard devuelve true)
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard, horarioGuard] },
  { path: 'solicitudes', component: SolicitudesComponent, canActivate: [authGuard, horarioGuard] },
  { path: 'conexiones', component: MisConexionesComponent, canActivate: [authGuard, horarioGuard] },
  { path: 'explorar', component: Explore, canActivate: [authGuard, horarioGuard] },

  // Cada persona, un sitio. Antes la ficha era un panel flotante incrustado a la
  // vez en el tablero y en Explorar: no habia URL a la que enlazar, el boton
  // "atras" no hacia lo esperable y el mismo componente vivia en dos pantallas.
  { path: 'u/:id', component: PerfilPublicoComponent, canActivate: [authGuard, horarioGuard] },

  // Tu propia pagina, que es donde te ves como te ven. Hasta ahora solo existia
  // editarte, que es otra cosa.
  { path: 'yo', component: PerfilPublicoComponent, canActivate: [authGuard, horarioGuard] },
  
  // Los mensajes viven en /conexiones. Antes habia ademas /chat/:id con una
  // segunda implementacion copiada, que se ha eliminado: se llega igual con
  // /conexiones?con=ID y solo hay un sitio que mantener.
  { path: 'chat/:id', redirectTo: 'conexiones', pathMatch: 'full' },

  // Redirecciones por defecto
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  
  // 🚨 EL COMODÍN DEBE SER SIEMPRE LA ÚLTIMA LÍNEA DEL ARRAY 🚨
  { path: '**', redirectTo: '/dashboard' } 
];