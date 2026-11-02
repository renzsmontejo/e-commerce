import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-account',
  imports: [MatButtonModule, MatCardModule],
  template: `
    <main aria-labelledby="account-title">
      <mat-card appearance="outlined"><mat-card-content>
      <h1 id="account-title">Your account</h1>
      <p role="status">You’re signed in.</p>
      <dl>
        <dt>Email</dt>
        <dd>{{ auth.user()?.email }}</dd>
        <dt>Roles</dt>
        <dd>{{ auth.user()?.roles?.join(', ') }}</dd>
      </dl>
      @if (auth.notice() === 'forbidden') {
        <p role="alert">You don’t have permission to access that resource.</p>
      }
      <button mat-flat-button type="button" (click)="auth.logout()">Sign out</button>
      </mat-card-content></mat-card>
    </main>
  `,
  styles: `
    main {
      width: min(100% - 2 * var(--space-5), 34rem);
      max-width: 34rem;
      margin: 3rem auto;



      border-radius: var(--radius-card);
    }
    h1 {
      margin-top: 0;
    }
    dt {
      font-weight: 600;
    }
    dd {
      margin: 0 0 var(--space-4);
      overflow-wrap: anywhere;
    }
    button {
      min-height: var(--control-min-height);
      padding: var(--space-3) var(--space-5);
      border: 0;
      border-radius: var(--radius-control);
      background: var(--color-primary);
      color: white;
      cursor: pointer;
    }
  `,
})
export class Account {
  protected readonly auth = inject(AuthService);
}
