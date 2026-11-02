import {
  ChangeDetectorRef,
  Component,
  ElementRef,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LoginRequest } from '../../../../shared/models/auth.models';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  private readonly changeDetector = inject(ChangeDetectorRef);
  private readonly emailInput = viewChild.required<ElementRef<HTMLInputElement>>('emailInput');
  private readonly passwordInput =
    viewChild.required<ElementRef<HTMLInputElement>>('passwordInput');
  readonly submitting = input(false);
  readonly returnUrl = input<string | null>(null);
  readonly authenticationError = input<string | null>(null);
  readonly loginSubmitted = output<LoginRequest>();
  protected readonly passwordVisible = signal(false);
  protected readonly form = new FormGroup({
    email: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected submit(): void {
    if (this.submitting()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      // Render described errors before moving assistive-technology focus.
      this.changeDetector.detectChanges();
      const invalidInput = this.form.controls.email.invalid
        ? this.emailInput()
        : this.passwordInput();
      invalidInput.nativeElement.focus();
      return;
    }
    this.loginSubmitted.emit(this.form.getRawValue());
  }
}
