import { AuthService } from '../../../../core/auth/auth.service';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter, Router } from '@angular/router';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { LoginPage } from './login-page';

describe('Login page API wiring', () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { queryParamMap: convertToParamMap({ returnUrl: '//evil.example' }) },
          },
        },
      ],
    }),
  );
  it('shows loading, blocks duplicate submissions, and recovers from a network failure', () => {
    const fixture = TestBed.createComponent(LoginPage);
    const root: HTMLElement = fixture.nativeElement;
    fixture.detectChanges();
    for (const [id, value] of [
      ['email', 'admin@example.com'],
      ['password', 'password'],
    ]) {
      const input = root.querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    const submit = () => {
      root.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
      fixture.detectChanges();
    };
    submit();
    submit();
    expect(root.querySelector<HTMLButtonElement>('.sign-in')!.disabled).toBe(true);
    const http = TestBed.inject(HttpTestingController);
    const request = http.expectOne('/api/auth/login');
    request.error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(root.querySelector('[role="alert"]')!.textContent).toContain('Cannot connect');
    expect(root.querySelector<HTMLButtonElement>('.sign-in')!.disabled).toBe(false);
    http.verify();
  });
  it.each([
    [null, '/home'],
    ['//evil.example', '/home'],
    ['/account?tab=orders', '/account?tab=orders'],
  ])('redirects a customer login from %s to %s', (returnUrl, expected) => {
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: { snapshot: { queryParamMap: convertToParamMap({ returnUrl }) } },
    });
    const fixture = TestBed.createComponent(LoginPage);
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    fixture.detectChanges();
    const root: HTMLElement = fixture.nativeElement;
    for (const [id, value] of [
      ['email', 'admin@example.com'],
      ['password', 'password'],
    ]) {
      const input = root.querySelector<HTMLInputElement>(`#${id}`)!;
      input.value = value;
      input.dispatchEvent(new Event('input'));
    }
    root.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    const http = TestBed.inject(HttpTestingController);
    const token = `header.${btoa(JSON.stringify({ exp: Date.now() / 1000 + 900 }))}.signature`;
    http
      .expectOne('/api/auth/login')
      .flush({ accessToken: token, tokenType: 'Bearer', userId: 1, email: 'admin@example.com' });
    http
      .expectOne('/api/auth/me')
      .flush({ id: 1, email: 'admin@example.com', roles: ['CUSTOMER'] });
    expect(navigate).toHaveBeenCalledWith(expected);
    TestBed.inject(AuthService).logout();
    http.verify();
  });
});
