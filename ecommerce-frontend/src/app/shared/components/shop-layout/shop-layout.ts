import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SiteHeader } from '../site-header/site-header';

/** Storefront shell (header + page). Login/register stay outside this layout. */
@Component({
  selector: 'app-shop-layout',
  imports: [RouterOutlet, SiteHeader],
  template: `
    <app-site-header [cartCount]="cartCount()" />
    <main><router-outlet /></main>
  `,
})
export class ShopLayout {
  // TODO: replace with CartService item count once cart is wired to the backend
  readonly cartCount = signal(1);
}