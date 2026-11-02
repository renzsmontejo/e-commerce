import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guards';

export const routes: Routes = [
  {
    path: 'account',
    title: 'Your account | We Shop Smarter',
    canActivate: [authGuard],
    loadComponent: () => import('./features/account/account').then((m) => m.Account),
  },
  { path: '', pathMatch: 'full', redirectTo: 'auth/login' },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.authRoutes),
  },
  { path: '**', redirectTo: 'auth/login' },
];
