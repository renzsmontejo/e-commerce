import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { provideHttpClient } from '@angular/common/http';
import { AuthService } from './core/auth/auth.service';

describe('Authentication routing', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient()] });
  });

  it('loads the auth shell and login destination from the root route', async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(TestBed.inject(Router).url).toBe('/auth/login');
    expect(harness.routeNativeElement?.querySelector('header')?.textContent).toContain('We Shop');
    expect(harness.routeNativeElement?.querySelector('h1')?.textContent).toBe(
      'Sign in to your account',
    );
  });

  it('provides a working fallback for an unknown URL', async () => {
    const harness = await RouterTestingHarness.create('/missing-page');
    expect(TestBed.inject(Router).url).toBe('/auth/login');
    expect(harness.routeNativeElement?.querySelector('main')).toBeTruthy();
  });

  it('preserves a protected destination for an anonymous visitor', async () => {
    await RouterTestingHarness.create('/account?tab=orders');
    expect(TestBed.inject(Router).url).toBe('/auth/login?returnUrl=%2Faccount%3Ftab%3Dorders');
  });

  it('keeps a signed-in visitor out of the login page', async () => {
    vi.spyOn(TestBed.inject(AuthService), 'accessToken').mockReturnValue('session-token');
    await RouterTestingHarness.create('/auth/login?returnUrl=//evil.example');
    expect(TestBed.inject(Router).url).toBe('/account');
  });
});
