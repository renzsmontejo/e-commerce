import {
  ChangeDetectorRef,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { AuthService } from '../../../../core/auth/auth.service';
import { safeReturnUrl } from '../../../../core/auth/return-url';

const nonBlank = (control: AbstractControl): ValidationErrors | null =>
  typeof control.value === 'string' && control.value.trim() ? null : { required: true };
const bcryptLength = (control: AbstractControl): ValidationErrors | null =>
  new TextEncoder().encode(control.value || '').length > 72 ? { passwordBytes: true } : null;

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly auth = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formElement = viewChild<ElementRef<HTMLFormElement>>('formElement');
  private readonly successHeading = viewChild<ElementRef<HTMLHeadingElement>>('successHeading');
  protected readonly returnUrl = safeReturnUrl(
    inject(ActivatedRoute).snapshot.queryParamMap.get('returnUrl'),
  );
  protected readonly submitting = signal(false);
  protected readonly success = signal(false);
  protected readonly error = signal('');
  protected readonly passwordVisible = signal(false);
  protected readonly confirmVisible = signal(false);
  protected readonly textFields = [
    { key: 'firstName', label: 'First name', autocomplete: 'given-name', type: 'text', max: 150 },
    { key: 'lastName', label: 'Last name', autocomplete: 'family-name', type: 'text', max: 150 },
    { key: 'email', label: 'Email address', autocomplete: 'email', type: 'email', max: 255 },
    { key: 'phone', label: 'Phone number (optional)', autocomplete: 'tel', type: 'tel', max: 50 },
  ] as const;
  protected readonly form = new FormGroup(
    {
      firstName: new FormControl('', {
        nonNullable: true,
        validators: [nonBlank, Validators.maxLength(150)],
      }),
      lastName: new FormControl('', {
        nonNullable: true,
        validators: [nonBlank, Validators.maxLength(150)],
      }),
      email: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required, Validators.email, Validators.maxLength(255)],
      }),
      phone: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(50)] }),
      password: new FormControl('', {
        nonNullable: true,
        validators: [nonBlank, Validators.minLength(8), bcryptLength],
      }),
      confirmPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
    },
    {
      validators: (control) =>
        control.get('password')?.value === control.get('confirmPassword')?.value
          ? null
          : { mismatch: true },
    },
  );

  protected fieldError(key: keyof typeof this.form.controls): string {
    const control = this.form.controls[key];
    if (!control.touched) return '';
    if (control.hasError('required')) return key === 'phone' ? '' : 'This field is required.';
    if (control.hasError('email')) return 'Enter a valid email address.';
    if (control.hasError('maxlength')) return 'This value is too long.';
    if (control.hasError('minlength')) return 'Use at least 8 characters.';
    if (control.hasError('passwordBytes'))
      return 'This password is too long. Try fewer characters or symbols.';
    if (key === 'confirmPassword' && this.form.hasError('mismatch')) return 'Passwords must match.';
    return '';
  }

  protected submit(): void {
    if (this.submitting() || this.success()) return;
    this.error.set('');
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      this.changeDetector.detectChanges();
      const key =
        (Object.keys(this.form.controls) as (keyof typeof this.form.controls)[]).find(
          (key) => this.form.controls[key].invalid,
        ) || 'confirmPassword';
      this.formElement()
        ?.nativeElement.querySelector<HTMLInputElement>(`#register-${key}`)
        ?.focus();
      return;
    }
    const { confirmPassword: _confirmation, ...request } = this.form.getRawValue();
    this.submitting.set(true);
    this.auth
      .register(request)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe({
        next: () => {
          this.form.reset();
          this.passwordVisible.set(false);
          this.confirmVisible.set(false);
          this.success.set(true);
          // Let the success heading render before moving keyboard/screen-reader focus.
          setTimeout(() => this.successHeading()?.nativeElement.focus());
        },
        error: (error: unknown) =>
          this.error.set(
            error instanceof HttpErrorResponse && error.status === 409
              ? 'An account already uses this email address. Try signing in.'
              : error instanceof HttpErrorResponse && error.status === 0
                ? 'Cannot connect. Please check your connection and try again.'
                : error instanceof HttpErrorResponse && error.status === 400
                  ? 'Please check your details and try again.'
                  : 'Account creation is temporarily unavailable. Please try again shortly.',
          ),
      });
  }
}
