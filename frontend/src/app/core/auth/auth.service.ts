import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { map, switchMap, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  AuthState,
  CurrentUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from './auth.models';
import { safeReturnUrl } from './return-url';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = inject(API_BASE_URL);
  private readonly session = signal<AuthState>({ status: 'anonymous' });
  private expirationTimer?: ReturnType<typeof setTimeout>;
  private generation = 0;
  readonly state = this.session.asReadonly();
  readonly user = computed(() => {
    const state = this.session();
    return state.status === 'authenticated' ? state.user : null;
  });
  readonly notice = signal<'expired' | 'forbidden' | null>(null);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.clearSession());
  }

  accessToken(): string | null {
    const state = this.session();
    return state.status === 'authenticated' && state.expiresAt > Date.now()
      ? state.accessToken
      : null;
  }

  register(request: RegisterRequest) {
    return this.http.post<RegisterResponse>(`${this.baseUrl}/auth/register`, {
      firstName: request.firstName.trim(),
      lastName: request.lastName.trim(),
      email: request.email.trim().toLowerCase(),
      password: request.password,
      phone: request.phone?.trim() || null,
    });
  }

  login(credentials: LoginRequest) {
    const generation = ++this.generation;
    return this.http
      .post<LoginResponse>(`${this.baseUrl}/auth/login`, {
        email: credentials.email.trim().toLowerCase(),
        password: credentials.password,
      })
      .pipe(
        switchMap((response) => {
          if (response.tokenType !== 'Bearer' || !response.accessToken)
            throw new Error('Invalid login response');
          // The server verifies authenticity via /me. This decode is for expiry scheduling only.
          const payload = JSON.parse(
            atob(response.accessToken.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')),
          );
          const expiresAt = Number(payload.exp) * 1000;
          if (!Number.isFinite(expiresAt) || expiresAt <= Date.now())
            throw new Error('Expired login response');
          return this.http
            .get<CurrentUser>(`${this.baseUrl}/auth/me`, {
              headers: { Authorization: `Bearer ${response.accessToken}` },
            })
            .pipe(map((user) => ({ response, user, expiresAt })));
        }),
        tap(({ response, user, expiresAt }) => {
          if (generation !== this.generation) throw new Error('Login cancelled');
          if (
            expiresAt <= Date.now() ||
            user.id !== response.userId ||
            user.email !== response.email ||
            !Array.isArray(user.roles)
          ) {
            throw new Error('Invalid account response');
          }
          clearTimeout(this.expirationTimer);
          this.notice.set(null);
          this.session.set({
            status: 'authenticated',
            user,
            accessToken: response.accessToken,
            expiresAt,
          });
          this.expirationTimer = setTimeout(
            () => this.expire(),
            Math.min(expiresAt - Date.now(), 2_147_483_647),
          );
        }),
        map(({ user }) => user),
      );
  }

  logout(): void {
    this.clearSession();
    this.notice.set(null);
    void this.router.navigate(['/auth/login']);
  }

  expire(): void {
    this.clearSession();
    this.notice.set('expired');
    void this.router.navigate(['/auth/login'], {
      queryParams: { returnUrl: safeReturnUrl(this.router.url) },
    });
  }

  private clearSession(): void {
    ++this.generation;
    clearTimeout(this.expirationTimer);
    this.session.set({ status: 'anonymous' });
  }
}
