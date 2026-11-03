import { Component, inject, input } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';

interface NavLink {
  label: string;
  slug: string;
}

@Component({
  selector: 'app-site-header',
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './site-header.html',
  styleUrl: './site-header.css',
})
export class SiteHeader {
  private readonly router = inject(Router);

  readonly cartCount = input(0);

  readonly navLinks: NavLink[] = [
    { label: 'Home & Living', slug: 'home-living' },
    { label: 'Accessories', slug: 'accessories' },
    { label: 'Healthcare', slug: 'healthcare' },
    { label: 'Electronic', slug: 'electronic' },
    { label: "Kid's Fashion", slug: 'kids-fashion' },
    { label: 'Health & Beauty', slug: 'health-beauty' },
    { label: 'Sports & Outdoor', slug: 'sports-outdoor' },
    { label: 'Toys & Games', slug: 'toys-games' },
    { label: 'Fashion', slug: 'fashion' },
    { label: "Men's", slug: 'men' },
    { label: "Women's", slug: 'women' },
  ];

  search(term: string): void {
    const q = term.trim();
    if (!q) return;
    this.router.navigate(['/products'], { queryParams: { q } });
  }
}