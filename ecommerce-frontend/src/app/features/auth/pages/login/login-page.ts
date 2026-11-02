import { Component, DestroyRef, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { LoginRequest } from '../../../../shared/models/auth.models';
import { safeReturnUrl } from '../../../../core/auth/return-url';
import { Login } from './login';

@Component({
  selector: 'app-login-page',
  imports: [Login],
  template: `
    @if (auth.notice() === 'expired') {
      <p role="status">Your session has ended. Please sign in again.</p>
    }
    <app-login
      [submitting]="submitting()"
      [authenticationError]="error()"
      [returnUrl]="returnUrl"
      (loginSubmitted)="signIn($event)"
    />
  `,
})
export class LoginPage {
  protected readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  protected readonly returnUrl = safeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl'));
  private readonly destroyRef = inject(DestroyRef);
  protected readonly submitting = signal(false);
  protected readonly error = signal<string | null>(null);

  protected signIn(credentials: LoginRequest): void {
    if (this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    this.auth
      .login(credentials)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: () =>
          void this.router.navigateByUrl(
            safeReturnUrl(this.route.snapshot.queryParamMap.get('returnUrl')),
          ),
        error: (error: unknown) =>
          this.error.set(
            error instanceof HttpErrorResponse && error.status === 0
              ? 'network'
              : error instanceof HttpErrorResponse && error.status >= 500
                ? 'unavailable'
                : 'credentials',
          ),
      });
  }
}
