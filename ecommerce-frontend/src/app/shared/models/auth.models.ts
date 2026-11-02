/** Matches the existing Spring Boot /api/auth/login request. */
export interface LoginRequest {
  readonly email: string;
  readonly password: string;
}

/** The backend currently returns no roles or refresh token. */
export interface LoginResponse {
  readonly accessToken: string;
  readonly tokenType: string;
  readonly userId: number;
  readonly email: string;
}

export interface RegisterRequest {
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly password: string;
  readonly phone?: string | null;
}

export interface RegisterResponse {
  readonly id: number;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly status: string;
  readonly message: string;
}

export interface CurrentUser {
  readonly id: number;
  readonly email: string;
  readonly roles: readonly string[];
}

/** Kept in memory, never persisted in browser storage. */
export type AuthState =
  | { readonly status: 'anonymous' }
  | {
      readonly status: 'authenticated';
      readonly user: CurrentUser;
      readonly accessToken: string;
      readonly expiresAt: number;
    };
