import { Routes } from '@angular/router';
import { guestGuard } from '../../core/guards/auth.guards';

export const authRoutes: Routes = [
  {
    path: '',
    loadComponent: () => import('../../shared/components/auth-layout/auth-layout').then((m) => m.AuthLayout),
    children: [
      {
        path: 'register',
        canActivate: [guestGuard],
        title: 'Create account | We Shop Smarter',
        loadComponent: () => import('./pages/register/register').then((m) => m.Register),
      },
      { path: '', pathMatch: 'full', redirectTo: 'login' },
      {
        path: 'login',
        canActivate: [guestGuard],
        title: 'Sign in | We Shop Smarter',
        loadComponent: () => import('./pages/login/login-page').then((m) => m.LoginPage),
      },
    ],
  },
];
