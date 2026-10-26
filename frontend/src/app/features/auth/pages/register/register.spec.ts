import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Register } from './register';
import { AuthService } from '../../../../core/auth/auth.service';

describe('Registration', () => {
  let fixture: ComponentFixture<Register>;
  let root: HTMLElement;
  let http: HttpTestingController;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Register],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(Register);
    fixture.detectChanges();
    root = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  function fill(key: string, value: string) {
    const input = root.querySelector<HTMLInputElement>(`#register-${key}`)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }
  function validForm() {
    for (const [key, value] of Object.entries({
      firstName: ' Test ',
      lastName: ' Shopper ',
      email: 'SHOPPER@example.com',
      phone: ' +1 555 0100 ',
      password: ' password123 ',
      confirmPassword: ' password123 ',
    }))
      fill(key, value);
  }
  function submit() {
    root.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }
  it('shows required errors, focuses the first invalid field, and sends no invalid request', () => {
    const focus = vi.spyOn(root.querySelector<HTMLInputElement>('#register-firstName')!, 'focus');
    submit();
    expect(root.querySelector('#firstName-error')?.textContent).toContain('required');
    expect(focus).toHaveBeenCalled();
    http.expectNone('/api/auth/register');
  });
  it('requires matching passwords and rejects passwords over BCrypt’s byte limit', () => {
    validForm();
    fill('confirmPassword', 'different');
    submit();
    expect(root.querySelector('#confirmPassword-error')?.textContent).toContain('must match');
    fill('password', 'é'.repeat(37));
    fill('confirmPassword', 'é'.repeat(37));
    submit();
    expect(root.querySelector('#register-password-error')?.textContent).toContain('too long');
    http.expectNone('/api/auth/register');
  });
  it('normalizes profile fields, preserves passwords, blocks duplicates, and clears secrets on success', () => {
    validForm();
    submit();
    submit();
    const request = http.expectOne('/api/auth/register');
    expect(request.request.body).toEqual({
      firstName: 'Test',
      lastName: 'Shopper',
      email: 'shopper@example.com',
      phone: '+1 555 0100',
      password: ' password123 ',
    });
    expect(root.querySelector<HTMLButtonElement>('[type="submit"]')?.disabled).toBe(true);
    request.flush({
      id: 2,
      firstName: 'Test',
      lastName: 'Shopper',
      email: 'shopper@example.com',
      status: 'ACTIVE',
      message: 'Registration successful',
    });
    fixture.detectChanges();
    expect(root.querySelector('h1')?.textContent).toContain('Your account is ready');
    expect(root.querySelector('input[type="password"]')).toBeNull();
    expect(TestBed.inject(AuthService).user()).toBeNull();
    expect(root.querySelector('a')?.getAttribute('href')).toContain('/auth/login');
  });
  it('reports conflicts safely and allows retry after failure', () => {
    validForm();
    submit();
    http
      .expectOne('/api/auth/register')
      .flush({ detail: 'private SQL error' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('already uses this email');
    expect(root.textContent).not.toContain('private SQL');
    expect(root.querySelector<HTMLButtonElement>('[type="submit"]')?.disabled).toBe(false);
  });
  it('supports network error feedback and optional phone numbers', () => {
    validForm();
    fill('phone', '');
    submit();
    const request = http.expectOne('/api/auth/register');
    expect(request.request.body.phone).toBeNull();
    request.error(new ProgressEvent('error'));
    fixture.detectChanges();
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('Cannot connect');
  });
});
