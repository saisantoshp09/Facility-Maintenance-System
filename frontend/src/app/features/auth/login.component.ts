import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { AppError } from '../../core/models';

@Component({
  selector: 'app-login',
  template: `
    <div class="wrap">
      <mat-card class="card">
        <h1>Sign in</h1>
        <p class="muted">Property &amp; Facility Maintenance</p>
        <p class="notice" *ngIf="expired" role="status">Your session has expired. Please sign in again.</p>
        <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Email</mat-label>
            <input matInput type="email" autocomplete="username" formControlName="email" maxlength="254" cdkFocusInitial>
            <mat-error>Enter your email</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Password</mat-label>
            <input matInput type="password" autocomplete="current-password" formControlName="password" maxlength="128">
            <mat-error>Enter your password</mat-error>
          </mat-form-field>
          <p class="error-text" *ngIf="error" role="alert">{{ error }}</p>
          <button mat-flat-button color="primary" type="submit" class="full-width" [disabled]="busy">
            {{ busy ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>
      </mat-card>
    </div>`,
  styles: ['.wrap { display: flex; justify-content: center; padding: 64px 16px; } .card { width: 100%; max-width: 400px; padding: 24px; } h1 { margin-top: 0; } .notice { background: #fff8e1; padding: 8px 12px; }']
})
export class LoginComponent {
  busy = false;
  error: string | null = null;
  expired = false;
  form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.maxLength(254)]],
    password: ['', [Validators.required, Validators.maxLength(128)]]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router, route: ActivatedRoute) {
    this.expired = route.snapshot.queryParamMap.get('expired') === '1';
    this.returnUrl = route.snapshot.queryParamMap.get('returnUrl');
  }
  private returnUrl: string | null;

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.busy) { return; }
    this.busy = true;
    this.error = null;
    const { email, password } = this.form.getRawValue();
    this.auth.login(email.trim(), password).subscribe({
      next: () => {
        const target = this.returnUrl && this.returnUrl.startsWith('/') && !this.returnUrl.startsWith('//')
          && !this.returnUrl.startsWith('/login') ? this.returnUrl : '/dashboard';
        this.router.navigateByUrl(target);
      },
      error: (e: AppError) => {
        this.busy = false;
        this.form.controls.password.reset('');
        this.error = e.status === 401 ? 'Sign-in failed. Check your email and password.'
          : e.status === 0 || e.status >= 500 ? 'The service is unavailable right now. Please try again shortly.'
          : 'Sign-in failed. Please try again.';
      }
    });
  }
}
