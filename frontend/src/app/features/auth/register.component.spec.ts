import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { SharedModule } from '../../shared/shared.module';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let auth: jasmine.SpyObj<AuthService>;
  let router: Router;

  beforeEach(() => {
    auth = jasmine.createSpyObj('AuthService', ['register']);
    TestBed.configureTestingModule({
      declarations: [RegisterComponent],
      imports: [SharedModule, NoopAnimationsModule, RouterTestingModule],
      providers: [{ provide: AuthService, useValue: auth }]
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
    fixture = TestBed.createComponent(RegisterComponent);
    fixture.detectChanges();
  });

  it('does not submit invalid registration details', () => {
    fixture.componentInstance.submit();
    expect(auth.register).not.toHaveBeenCalled();
  });

  it('registers the user then routes them to sign in', () => {
    auth.register.and.returnValue(of({} as any));
    fixture.componentInstance.form.setValue({
      displayName: '  New Tenant ',
      email: ' new@example.test ',
      password: 'a-long-test-password'
    });

    fixture.componentInstance.submit();

    expect(auth.register).toHaveBeenCalledWith({
      displayName: 'New Tenant',
      email: 'new@example.test',
      password: 'a-long-test-password'
    });
    expect(router.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { registered: '1', email: 'new@example.test' }
    });
  });

  it('shows duplicate-account errors and clears the password', () => {
    auth.register.and.returnValue(throwError(() => ({ status: 409 })));
    fixture.componentInstance.form.setValue({
      displayName: 'New Tenant',
      email: 'new@example.test',
      password: 'a-long-test-password'
    });

    fixture.componentInstance.submit();
    fixture.detectChanges();

    expect(fixture.componentInstance.error).toContain('already exists');
    expect(fixture.componentInstance.form.controls.password.value).toBe('');
  });
});
