import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { AppError } from '../../core/models';

@Component({
  selector: 'app-register',
  template: `
    <div class="wrap">
      <mat-card class="card">
        <h1>Create account</h1>
        <p class="muted">Register for Property &amp; Facility Maintenance</p>
        <p class="notice">New accounts are registered as tenants. A property manager can add you to a property.</p>
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Full name</mat-label>
            <input matInput autocomplete="name" formControlName="displayName" maxlength="120">
            <mat-error *ngIf="form.controls.displayName.hasError('required')">Enter your name</mat-error>
            <mat-error *ngIf="form.controls.displayName.hasError('maxlength')">Name must be at most 120 characters</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email</mat-label>
            <input matInput type="email" autocomplete="email" formControlName="email" maxlength="254">
            <mat-error *ngIf="form.controls.email.hasError('required')">Enter your email</mat-error>
            <mat-error *ngIf="form.controls.email.hasError('email')">Enter a valid email</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Password</mat-label>
            <input matInput type="password" autocomplete="new-password" formControlName="password" maxlength="72">
            <mat-hint>Use 12 to 72 characters.</mat-hint>
            <mat-error *ngIf="form.controls.password.hasError('required')">Enter a password</mat-error>
            <mat-error *ngIf="form.controls.password.hasError('minlength')">Use at least 12 characters</mat-error>
          </mat-form-field>
          <p class="error-text" *ngIf="error" role="alert">{{ error }}</p>
          <button mat-flat-button color="primary" type="submit" class="full-width" [disabled]="busy">
            {{ busy ? 'Creating account...' : 'Create account' }}
          </button>
        </form>
        <p class="register-link">Already registered? <a routerLink="/login">Sign in</a></p>
      </mat-card>
    </div>`,
  styles: ['.wrap { display: flex; justify-content: center; padding: 64px 16px; } .card { width: 100%; max-width: 400px; padding: 24px; } h1 { margin-top: 0; } .notice { background: #eef4ff; padding: 8px 12px; }']
})
export class RegisterComponent {
  busy = false;
  error: string | null = null;
  form = this.fb.nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.minLength(12), Validators.maxLength(72)]]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit(): void {
    this.form.controls.displayName.setValue(this.form.controls.displayName.value.trim());
    this.form.controls.email.setValue(this.form.controls.email.value.trim());
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    this.error = null;
    const { displayName, email, password } = this.form.getRawValue();
    this.auth.register({ displayName: displayName.trim(), email: email.trim(), password }).subscribe({
      next: () => this.router.navigate(['/login'], { queryParams: { registered: '1', email: email.trim() } }),
      error: (e: AppError) => {
        this.busy = false;
        this.form.controls.password.reset('');
        this.error = e.status === 409 ? 'An account with this email already exists.'
          : e.status === 400 ? 'Check your name, email, and password and try again.'
          : e.status === 0 || e.status >= 500 ? 'The service is unavailable right now. Please try again shortly.'
          : 'Unable to create your account. Please try again.';
      }
    });
  }
}
