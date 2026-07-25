import { Routes } from '@angular/router';
import { LoginComponent } from './components/login/login'; 
import { DashboardComponent } from './components/dashboard/dashboard'; 
import { SolicitudesComponent } from './components/solicitudes/solicitudes';
import { MisConexionesComponent } from './components/mis-conexiones/mis-conexiones';
import { Explore } from './components/explore/explore';
import { authGuard } from './guards/auth.guard'; // 🔥 Importamos el cerrojo de seguridad

export const routes: Routes = [
  // 🔓 Ruta pública (Cualquiera puede entrar a loguearse o registrarse)
  { path: 'login', component: LoginComponent },
  
  // 🔒 RUTAS BLINDADAS (Solo entras si el authGuard devuelve true)
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'solicitudes', component: SolicitudesComponent, canActivate: [authGuard] },
  { path: 'conexiones', component: MisConexionesComponent, canActivate: [authGuard] },
  { path: 'explorar', component: Explore, canActivate: [authGuard] },
  
  // Los mensajes viven en /conexiones. Antes habia ademas /chat/:id con una
  // segunda implementacion copiada, que se ha eliminado: se llega igual con
  // /conexiones?con=ID y solo hay un sitio que mantener.
  { path: 'chat/:id', redirectTo: 'conexiones', pathMatch: 'full' },

  // Redirecciones por defecto
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  
  // 🚨 EL COMODÍN DEBE SER SIEMPRE LA ÚLTIMA LÍNEA DEL ARRAY 🚨
  { path: '**', redirectTo: '/dashboard' } 
];