import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Home } from './home';

describe('Home category integration', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [Home],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }),
  );
  afterEach(() => TestBed.inject(HttpTestingController).verify());

  it('loads backend categories once and preserves the existing tile presentation', () => {
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading categories');
    TestBed.inject(HttpTestingController)
      .expectOne('/api/categories')
      .flush([
        { id: 31, name: 'Backend category', slug: 'accessories', parentId: null },
        { id: 32, name: 'New category', slug: 'new-category', parentId: null },
      ]);
    fixture.detectChanges();
    fixture.detectChanges();
    const tiles: NodeListOf<HTMLAnchorElement> = fixture.nativeElement.querySelectorAll('.tile');
    expect(tiles).toHaveLength(2);
    expect(tiles[0].textContent).toContain('Backend category');
    expect(tiles[0].querySelector('img')?.getAttribute('src')).toBe(
      '/images/categories/accessories.png',
    );
    expect(tiles[1].querySelector('img')?.getAttribute('src')).toBe(
      '/images/categories/home-living.png',
    );
    expect(tiles[0].getAttribute('href')).toBe('/products?category=accessories');
    TestBed.inject(HttpTestingController).expectNone('/api/categories');
  });

  it('shows an empty state without fake categories', () => {
    const fixture = TestBed.createComponent(Home);
    TestBed.inject(HttpTestingController).expectOne('/api/categories').flush([]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('No categories are available');
    expect(fixture.nativeElement.querySelectorAll('.tile')).toHaveLength(0);
  });

  it('keeps the home page usable when the backend fails', () => {
    const fixture = TestBed.createComponent(Home);
    TestBed.inject(HttpTestingController)
      .expectOne('/api/categories')
      .flush({}, { status: 503, statusText: 'Unavailable' });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain(
      'Categories could not be loaded',
    );
    expect(fixture.nativeElement.querySelector('.hero')).toBeTruthy();
  });
});
