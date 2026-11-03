import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Category } from '../../../../shared/models/category';
import { CategoryService } from '../../../../core/services/category.service';

interface Perk {
  title: string;
  text: string;
  icon: 'return' | 'star' | 'shipping' | 'dispatch';
  color: string;
}

@Component({
  selector: 'app-home',
  imports: [RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home {
  private readonly categoryService = inject(CategoryService);
  private readonly destroyRef = inject(DestroyRef);
  readonly categories = signal<Category[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  constructor() {
    this.categoryService
      .list()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (categories) => {
          this.categories.set(
            categories.map((category) => ({
              ...category,
              ...this.categoryStyle(category.slug),
            })),
          );
          this.loading.set(false);
        },
        error: () => {
          this.error.set(true);
          this.loading.set(false);
        },
      });
  }

  private categoryStyle(slug: string): Pick<Category, 'imageUrl' | 'tint'> {
    const index = this.categorySlugs.indexOf(slug);
    const image =
      slug === 'accessories'
        ? 'accessories'
        : slug === 'electronic' || slug === 'electronics'
          ? 'electronics'
          : 'home-living';
    return {
      imageUrl: `/images/categories/${image}.png`,
      tint: this.categoryTints[index] ?? '#E2EBFD',
    };
  }

  // Existing artwork is presentation metadata; category identity and names come from the API.
  private readonly categorySlugs = [
    'home-living',
    'accessories',
    'healthcare',
    'electronic',
    'phone-ipads',
    'health-beauty',
    'sports-outdoor',
    'toys-games',
  ];
  private readonly categoryTints = [
    '#FDE5E5',
    '#E2EBFD',
    '#FAF3C6',
    '#FCE3FC',
    '#FFE1E8',
    '#DDF5DB',
    '#D5F1F5',
    '#FEF3E0',
  ];
  readonly perks: Perk[] = [
    {
      title: 'Free & flexible return',
      text: 'Easy Returns, No Hassle',
      icon: 'return',
      color: '#7C83F7',
    },
    {
      title: 'Our Google Reviews',
      text: 'See What Our Customers Say',
      icon: 'star',
      color: '#FBBF24',
    },
    {
      title: 'Free UK Shipping',
      text: 'Enjoy Fast, Free Delivery!',
      icon: 'shipping',
      color: '#00C310',
    },
    {
      title: 'Same day Dispatch',
      text: 'Prompt Shipping, Quick Arrival',
      icon: 'dispatch',
      color: '#D9682B',
    },
  ];
}
