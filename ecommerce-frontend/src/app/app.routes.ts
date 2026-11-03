import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guards';
import { ShopLayout } from './shared/components/shop-layout/shop-layout';

export const routes: Routes = [
  { path: 'login', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'register', redirectTo: 'auth/register', pathMatch: 'full' },
  // Auth pages stay outside ShopLayout, so login/register have no header
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.authRoutes),
  },

  // Storefront: header + page content
  {
    path: '',
    component: ShopLayout,
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'home',
      },
      {
        path: 'home',
        title: 'We Shop Smarter',
        loadComponent: () => import('./features/home/pages/home/home').then((m) => m.Home),
      },
      {
        path: 'account',
        title: 'Your account | We Shop Smarter',
        canActivate: [authGuard],
        loadComponent: () => import('./features/account/account').then((m) => m.Account),
      },
      // products, cart ... go here later
    ],
  },

  { path: '**', redirectTo: '' },
];
