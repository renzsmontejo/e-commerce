import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { safeReturnUrl } from './return-url';

const account = { id: 1, email: 'admin@example.com', roles: ['ADMIN'] };
describe('Authentication integration', () => {
  let auth: AuthService;
  let http: HttpTestingController;
  let client: HttpClient;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    client = TestBed.inject(HttpClient);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });
  afterEach(() => {
    auth.logout();
    http.verify();
    vi.useRealTimers();
  });
  function signIn(expiresIn = 900): string {
    const token = `header.${btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + expiresIn }))}.signature`;
    auth.login({ email: ' ADMIN@EXAMPLE.COM ', password: ' unchanged ' }).subscribe();
    const login = http.expectOne('/api/auth/login');
    expect(login.request.body).toEqual({ email: 'admin@example.com', password: ' unchanged ' });
    expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({ accessToken: token, tokenType: 'Bearer', userId: 1, email: account.email });
    expect(auth.user()).toBeNull();
    const me = http.expectOne('/api/auth/me');
    expect(me.request.headers.get('Authorization')).toBe(`Bearer ${token}`);
    me.flush(account);
    return token;
  }
  it('establishes a session only after server confirmation and scopes bearer headers', () => {
    const token = signIn();
    expect(auth.user()).toEqual(account);
    for (const url of [
      '/api/orders',
      '/api-lookalike/orders',
      'https://other.example/api/orders',
    ]) {
      client.get(url).subscribe();
      const request = http.expectOne(url);
      expect(request.request.headers.get('Authorization')).toBe(
        url === '/api/orders' ? `Bearer ${token}` : null,
      );
      request.flush({});
    }
  });
  it('does not authenticate after rejected credentials', () => {
    let failed = false;
    auth
      .login({ email: account.email, password: 'wrong' })
      .subscribe({ error: () => (failed = true) });
    http.expectOne('/api/auth/login').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(failed).toBe(true);
    expect(auth.accessToken()).toBeNull();
    http.expectNone('/api/auth/me');
  });
  it('does not accept a token when the server rejects /me', () => {
    const token = `header.${btoa(JSON.stringify({ exp: Date.now() / 1000 + 900 }))}.signature`;
    auth.login({ email: account.email, password: 'wrong' }).subscribe({ error: () => {} });
    http
      .expectOne('/api/auth/login')
      .flush({ accessToken: token, tokenType: 'Bearer', userId: 1, email: account.email });
    http.expectOne('/api/auth/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.user()).toBeNull();
  });
  it('clears a rejected session on 401 without retrying and preserves it on 403', () => {
    signIn();
    client.get('/api/admin/test').subscribe({ error: () => {} });
    http.expectOne('/api/admin/test').flush({}, { status: 403, statusText: 'Forbidden' });
    expect(auth.user()).toEqual(account);
    expect(auth.notice()).toBe('forbidden');
    client.get('/api/orders').subscribe({ error: () => {} });
    http.expectOne('/api/orders').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.user()).toBeNull();
    expect(auth.notice()).toBe('expired');
  });
  it('expires idle sessions and prevents late responses from restoring a logged-out session', () => {
    vi.useFakeTimers();
    signIn(2);
    vi.advanceTimersByTime(2000);
    expect(auth.accessToken()).toBeNull();
    expect(auth.notice()).toBe('expired');
    const token = `header.${btoa(JSON.stringify({ exp: Date.now() / 1000 + 900 }))}.signature`;
    auth.login({ email: account.email, password: 'test' }).subscribe({ error: () => {} });
    http
      .expectOne('/api/auth/login')
      .flush({ accessToken: token, tokenType: 'Bearer', userId: 1, email: account.email });
    auth.logout();
    http.expectOne('/api/auth/me').flush(account);
    expect(auth.user()).toBeNull();
  });
  it('allows only implemented internal return URLs', () => {
    for (const url of [
      'https://evil.example',
      '//evil.example',
      '/\\evil.example',
      '/auth/login',
      '/account/../auth/login',
      '/%2f%2fevil.example',
    ])
      expect(safeReturnUrl(url)).toBe('/account');
    expect(safeReturnUrl('/account?tab=orders#recent')).toBe('/account?tab=orders#recent');
  });

  it('ignores stale 401 and 403 responses from a previous session', () => {
    signIn();
    client.get('/api/old-unauthorized').subscribe({ error: () => {} });
    client.get('/api/old-forbidden').subscribe({ error: () => {} });
    const unauthorized = http.expectOne('/api/old-unauthorized');
    const forbidden = http.expectOne('/api/old-forbidden');
    auth.logout();
    const currentToken = signIn(899);
    unauthorized.flush({}, { status: 401, statusText: 'Unauthorized' });
    forbidden.flush({}, { status: 403, statusText: 'Forbidden' });
    expect(auth.accessToken()).toBe(currentToken);
    expect(auth.notice()).toBeNull();
  });

  it('preserves explicit headers and excludes public authentication requests from session handling', () => {
    const token = signIn();
    client
      .get('/api/other-session', { headers: { Authorization: 'Bearer explicit' } })
      .subscribe({ error: () => {} });
    const explicit = http.expectOne('/api/other-session');
    expect(explicit.request.headers.get('Authorization')).toBe('Bearer explicit');
    explicit.flush({}, { status: 403, statusText: 'Forbidden' });
    client.post('/api/auth/register', {}).subscribe({ error: () => {} });
    const registration = http.expectOne('/api/auth/register');
    expect(registration.request.headers.has('Authorization')).toBe(false);
    registration.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(auth.accessToken()).toBe(token);
    expect(auth.notice()).toBeNull();
  });

  it('rejects malformed, expired, and mismatched identity responses', () => {
    for (const token of [
      'invalid',
      `h.${btoa(JSON.stringify({ exp: Date.now() / 1000 - 1 }))}.s`,
    ]) {
      let failed = false;
      auth
        .login({ email: account.email, password: 'test' })
        .subscribe({ error: () => (failed = true) });
      http
        .expectOne('/api/auth/login')
        .flush({ accessToken: token, tokenType: 'Bearer', userId: 1, email: account.email });
      http.expectNone('/api/auth/me');
      expect(failed).toBe(true);
      expect(auth.accessToken()).toBeNull();
    }
    const token = `h.${btoa(JSON.stringify({ exp: Date.now() / 1000 + 900 }))}.s`;
    let failed = false;
    auth
      .login({ email: account.email, password: 'test' })
      .subscribe({ error: () => (failed = true) });
    http
      .expectOne('/api/auth/login')
      .flush({ accessToken: token, tokenType: 'Bearer', userId: 1, email: account.email });
    http.expectOne('/api/auth/me').flush({ ...account, id: 2 });
    expect(failed).toBe(true);
    expect(auth.accessToken()).toBeNull();
  });
});
