import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config/api.config';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const base = inject(API_BASE_URL);
  // Boundary match prevents token disclosure to other origins or /api-lookalike URLs.
  const isApi = request.url.startsWith(`${base}/`);
  const isPublicAuth =
    request.url.split('?')[0] === `${base}/auth/login` ||
    request.url.split('?')[0] === `${base}/auth/register`;
  const token = isApi && !isPublicAuth ? auth.accessToken() : null;
  const outgoing =
    token && !request.headers.has('Authorization')
      ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : request;
  return next(outgoing).pipe(
    catchError((error: unknown) => {
      // Only invalidate the session whose token this failed request actually used.
      if (
        isApi &&
        !isPublicAuth &&
        error instanceof HttpErrorResponse &&
        token &&
        outgoing.headers.get('Authorization') === `Bearer ${token}` &&
        auth.accessToken() === token
      ) {
        if (error.status === 401) auth.expire();
        if (error.status === 403) auth.notice.set('forbidden');
      }
      return throwError(() => error);
    }),
  );
};
