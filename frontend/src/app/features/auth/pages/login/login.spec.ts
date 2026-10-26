import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Login } from './login';
import { provideRouter } from '@angular/router';

describe('Login form', () => {
  let fixture: ComponentFixture<Login>;
  let root: HTMLElement;
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    root = fixture.nativeElement;
  });
  function enter(id: string, value: string): void {
    const field = root.querySelector<HTMLInputElement>(`#${id}`)!;
    field.value = value;
    field.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }
  function submit(): void {
    root.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    fixture.detectChanges();
  }
  it('shows associated errors and does not submit an empty form', () => {
    const emit = vi.spyOn(fixture.componentInstance.loginSubmitted, 'emit');
    submit();
    expect(root.querySelector('#email')?.getAttribute('aria-describedby')).toBe('email-error');
    expect(root.querySelector('#password-error')?.textContent).toContain('Enter your password');
    expect(emit).not.toHaveBeenCalled();
  });
  it('rejects malformed email and accepts an existing short password without new-password rules', () => {
    const emit = vi.spyOn(fixture.componentInstance.loginSubmitted, 'emit');
    enter('email', 'invalid');
    enter('password', 'short');
    submit();
    expect(root.querySelector('#email-error')?.textContent).toContain('valid email');
    expect(emit).not.toHaveBeenCalled();
    enter('email', 'shopper@example.com');
    submit();
    expect(emit).toHaveBeenCalledWith({ email: 'shopper@example.com', password: 'short' });
  });
  it('toggles password visibility without submitting', () => {
    const emit = vi.spyOn(fixture.componentInstance.loginSubmitted, 'emit');
    const toggle = root.querySelector<HTMLButtonElement>('.password-toggle')!;
    toggle.click();
    fixture.detectChanges();
    expect(root.querySelector('#password')?.getAttribute('type')).toBe('text');
    expect(toggle.getAttribute('aria-label')).toBe('Hide password');
    expect(emit).not.toHaveBeenCalled();
  });
  it('disables repeated submission while busy and announces a generic authentication error', () => {
    enter('email', 'shopper@example.com');
    enter('password', 'short');
    const emit = vi.spyOn(fixture.componentInstance.loginSubmitted, 'emit');
    fixture.componentRef.setInput('submitting', true);
    fixture.componentRef.setInput('authenticationError', 'Internal database details');
    fixture.detectChanges();
    submit();
    expect(root.querySelector<HTMLButtonElement>('.sign-in')?.disabled).toBe(true);
    expect(root.querySelector('form')?.getAttribute('aria-busy')).toBe('true');
    expect(root.querySelector('[role="alert"]')?.textContent).toContain('Unable to sign in');
    expect(root.textContent).not.toContain('Internal database details');
    expect(emit).not.toHaveBeenCalled();
  });
});
