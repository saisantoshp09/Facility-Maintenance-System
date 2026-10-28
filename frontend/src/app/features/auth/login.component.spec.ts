import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { SharedModule } from '../../shared/shared.module';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  function setup(query: Record<string, string> = {}) {
    auth = jasmine.createSpyObj('AuthService', ['login']);
    TestBed.configureTestingModule({
      declarations: [LoginComponent],
      imports: [SharedModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: (k: string) => query[k] ?? null } } } }
      ]
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
  }

  const fill = (email: string, password: string) => {
    fixture.componentInstance.form.setValue({ email, password });
  };

  it('does not submit an invalid form', () => {
    setup();
    fixture.componentInstance.submit();
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('logs in and navigates to the return URL', () => {
    setup({ returnUrl: '/work-orders/42' });
    auth.login.and.returnValue(of({} as any));
    fill(' a@example.test ', 'secret');
    fixture.componentInstance.submit();
    expect(auth.login).toHaveBeenCalledWith('a@example.test', 'secret');
    expect(router.navigateByUrl).toHaveBeenCalledWith('/work-orders/42');
  });

  it('ignores open-redirect return URLs', () => {
    setup({ returnUrl: '//evil.example' });
    auth.login.and.returnValue(of({} as any));
    fill('a@example.test', 'x');
    fixture.componentInstance.submit();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('shows one generic message on 401 and clears the password', () => {
    setup();
    auth.login.and.returnValue(throwError(() => ({ status: 401, code: 'AUTHENTICATION_FAILED', message: 'No such user', fieldErrors: [] })));
    fill('a@example.test', 'wrong');
    fixture.componentInstance.submit();
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent!;
    expect(text).toContain('Sign-in failed. Check your email and password.');
    expect(text).not.toContain('No such user');
    expect(fixture.componentInstance.form.controls.password.value).toBe('');
  });

  it('shows the session-expired notice', () => {
    setup({ expired: '1' });
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('session has expired');
  });
});
