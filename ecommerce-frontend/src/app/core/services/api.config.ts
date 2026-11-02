import { InjectionToken } from '@angular/core';
import { environment } from '../../../environments/environment';

/** Override this token in isolated HTTP tests without changing endpoint code. */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => environment.apiBaseUrl,
});
