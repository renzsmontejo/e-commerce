/** Allow only implemented destinations; auth pages and encoded paths cannot loop or escape. */
export function safeReturnUrl(value: string | null | undefined): string {
  if (!value || /[\\\u0000-\u0020]/.test(value)) return '/home';
  return /^\/(?:account|home)(?:[?#]|$)/.test(value) ? value : '/home';
}
