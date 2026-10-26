import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { safeReturnUrl } from './return-url';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  return auth.accessToken()
    ? true
    : inject(Router).createUrlTree(['/auth/login'], {
        queryParams: { returnUrl: safeReturnUrl(state.url) },
      });
};
export const guestGuard: CanActivateFn = (route) =>
  inject(AuthService).accessToken()
    ? inject(Router).parseUrl(safeReturnUrl(route.queryParamMap.get('returnUrl')))
    : true;
