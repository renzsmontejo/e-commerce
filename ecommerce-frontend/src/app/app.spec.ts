import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './core/auth/auth.service';

describe('Authentication routing', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  afterEach(() => {
    const http = TestBed.inject(HttpTestingController);
    http.match('/api/categories').forEach((request) => request.flush([]));
    http.verify();
  });

  it('loads the real home inside the shop layout from the root route', async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(TestBed.inject(Router).url).toBe('/home');
    expect(harness.routeNativeElement?.querySelector('header')?.textContent).toContain('We Shop');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toEqual(
      expect.stringContaining('Redefine Comfort'),
    );
  });

  it('provides a working fallback for an unknown URL', async () => {
    const harness = await RouterTestingHarness.create('/missing-page');
    expect(TestBed.inject(Router).url).toBe('/home');
    expect(harness.routeNativeElement?.querySelector('main')).toBeTruthy();
  });

  it('preserves a protected destination for an anonymous visitor', async () => {
    await RouterTestingHarness.create('/account?tab=orders');
    expect(TestBed.inject(Router).url).toBe('/auth/login?returnUrl=%2Faccount%3Ftab%3Dorders');
  });

  it.each([
    ['/login', '/auth/login'],
    ['/register', '/auth/register'],
  ])('resolves %s to the implemented auth page', async (alias, destination) => {
    await RouterTestingHarness.create(alias);
    expect(TestBed.inject(Router).url).toBe(destination);
  });

  it('keeps a signed-in visitor out of the login page', async () => {
    vi.spyOn(TestBed.inject(AuthService), 'accessToken').mockReturnValue('session-token');
    await RouterTestingHarness.create('/auth/login?returnUrl=//evil.example');
    expect(TestBed.inject(Router).url).toBe('/home');
  });
});
