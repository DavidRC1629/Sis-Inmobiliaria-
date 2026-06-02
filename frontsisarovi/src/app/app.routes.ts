import { Routes } from '@angular/router';
import { adminGuard, authGuard } from './guards/auth.guard';

export const routes: Routes = [
    {
      path: 'proformas/historial',
      loadComponent: () => import('./pages/proformas/proformas-historial.component').then(m => m.ProformasHistorialComponent),
      canActivate: [adminGuard]
    },
    {
      path: 'proformas',
      loadComponent: () => import('./pages/proformas/proformas.component').then(m => m.ProformasComponent),
      canActivate: [adminGuard]
    },
  {
    path: '',
    redirectTo: '/login',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./pages/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent)
  },
  {
    path: 'change-temporary-password',
    loadComponent: () => import('./pages/change-temporary-password/change-temporary-password.component').then(m => m.ChangeTemporaryPasswordComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./pages/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'lotes-propios',
    redirectTo: 'terrenos-propios',
    pathMatch: 'full'
  },
  {
    path: 'profile',
    loadComponent: () => import('./pages/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [authGuard]
  },
  {
    path: 'gestion-usuarios',
    loadComponent: () => import('./pages/gestion-usuarios/gestion-usuarios.component').then(m => m.GestionUsuariosComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'gestion-proyectos',
    loadComponent: () => import('./pages/gestion-proyectos/gestion-proyectos.component').then(m => m.GestionProyectosComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'etapas/:projectId',
    loadComponent: () => import('./pages/etapas/etapas.component').then(m => m.EtapasComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'parcelas/:etapaId',
    loadComponent: () => import('./pages/parcelas/parcelas.component').then(m => m.ParcelasComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'lotes/:parcelaId',
    loadComponent: () => import('./pages/lotes/lotes.component').then(m => m.LotesComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'registro',
    loadComponent: () => import('./pages/registro/registro.component').then(m => m.RegistroComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'clientes',
    loadComponent: () => import('./pages/clientes/clientes.component').then(m => m.ClientesComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'cronogramas',
    loadComponent: () => import('./pages/cronogramas/cronogramas.component').then(m => m.CronogramasComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'liberacion',
    loadComponent: () => import('./pages/liberacion/liberacion.component').then(m => m.LiberacionComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'devolucion',
    loadComponent: () => import('./pages/devolucion/devolucion.component').then(m => m.DevolucionComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'terrenos-propios',
    loadComponent: () => import('./pages/terrenos-propios/terrenos-propios.component').then(m => m.TerrenosPropiosComponent),
    canActivate: [adminGuard]
  },
  {
    path: '**',
    redirectTo: '/login'
  }
];
