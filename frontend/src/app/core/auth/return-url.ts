/** Only the currently implemented private destination is allowed. */
export function safeReturnUrl(value: string | null | undefined): string {
  if (!value || /[\\\u0000-\u0020]/.test(value)) return '/account';
  return /^\/account(?:[?#]|$)/.test(value) ? value : '/account';
}
