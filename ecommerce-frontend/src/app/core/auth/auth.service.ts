import { computed, DestroyRef, inject, Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom, map, switchMap, tap, timeout } from 'rxjs';
import { API_BASE_URL } from '../services/api.config';
import {
  AuthState,
  CurrentUser,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from '../../shared/models/auth.models';
import { safeReturnUrl } from './return-url';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'ecommerce.accessToken';
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
    inject(DestroyRef).onDestroy(() => {
      ++this.generation;
      clearTimeout(this.expirationTimer);
    });
  }

  async restoreSession(): Promise<void> {
    const generation = this.generation;
    try {
      const token = sessionStorage.getItem(this.storageKey);
      if (!token) return;
      const expiresAt = this.tokenExpiry(token);
      const user = await firstValueFrom(
        this.http
          .get<CurrentUser>(`${this.baseUrl}/auth/me`, {
            headers: { Authorization: `Bearer ${token}` },
          })
          .pipe(timeout(10000)),
      );
      if (generation === this.generation) this.establishSession(token, user, expiresAt);
    } catch {
      if (generation === this.generation) this.clearSession();
    }
  }

  private tokenExpiry(token: string): number {
    // Server /me verifies authenticity; decoding here only schedules expiry.
    const payload = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')));
    const expiresAt = Number(payload.exp) * 1000;
    if (!Number.isFinite(expiresAt) || expiresAt <= Date.now()) throw new Error('Expired token');
    return expiresAt;
  }

  private establishSession(accessToken: string, user: CurrentUser, expiresAt: number): void {
    if (expiresAt <= Date.now() || !user.id || !user.email || !Array.isArray(user.roles))
      throw new Error('Invalid account response');
    clearTimeout(this.expirationTimer);
    this.notice.set(null);
    this.session.set({ status: 'authenticated', user, accessToken, expiresAt });
    // Tab-scoped persistence supports refresh; no credentials or roles are trusted from storage.
    try {
      sessionStorage.setItem(this.storageKey, accessToken);
    } catch {
      /* Memory-only if unavailable. */
    }
    this.expirationTimer = setTimeout(
      () => this.expire(),
      Math.min(expiresAt - Date.now(), 2_147_483_647),
    );
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
          const expiresAt = this.tokenExpiry(response.accessToken);
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
          this.establishSession(response.accessToken, user, expiresAt);
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
    try {
      sessionStorage.removeItem(this.storageKey);
    } catch {
      /* Storage may be unavailable. */
    }
  }
}
