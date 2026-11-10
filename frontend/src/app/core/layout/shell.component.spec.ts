import { Component } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { BreakpointObserver } from '@angular/cdk/layout';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { TokenStore } from '../auth/token-store';
import { PropertyContext } from '../services/property-context';
import { ErrorNotifier } from '../services/error-notifier';
import { UserDirectory } from '../services/user-directory';
import { SharedModule } from '../../shared/shared.module';
import { ShellComponent } from './shell.component';

@Component({ selector: 'app-notification-bell', template: '<button aria-label="Notifications">Notifications</button>' })
class NotificationBellStubComponent {}

describe('ShellComponent', () => {
  let fixture: ComponentFixture<ShellComponent>;
  const properties$ = new BehaviorSubject([
    { id: 'p1', name: 'First property' },
    { id: 'p2', name: 'Second property' }
  ]);
  const selectedId$ = new BehaviorSubject<string | null>('p1');
  const ctx = {
    properties$, selectedId$, access: null, canUseAssets: true,
    load: jasmine.createSpy('load').and.returnValue(of([])),
    select: jasmine.createSpy('select'),
    reset: jasmine.createSpy('reset')
  };

  beforeEach(() => {
    ctx.select.calls.reset();
    TestBed.configureTestingModule({
      declarations: [ShellComponent, NotificationBellStubComponent],
      imports: [SharedModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: PropertyContext, useValue: ctx },
        { provide: AuthService, useValue: { user: { displayName: 'User', email: 'user@example.test' }, isAdmin: false } },
        { provide: TokenStore, useValue: { expiringSoon$: new Subject(), expired$: new Subject() } },
        { provide: BreakpointObserver, useValue: { observe: () => of({ matches: false }) } },
        { provide: ErrorNotifier, useValue: {} },
        { provide: UserDirectory, useValue: { clear: () => undefined } }
      ]
    });
    fixture = TestBed.createComponent(ShellComponent);
    fixture.detectChanges();
  });

  it('keeps the existing navigation links and notification control', () => {
    const links: HTMLAnchorElement[] = Array.from(fixture.nativeElement.querySelectorAll('mat-nav-list a'));
    expect(links.map(link => link.getAttribute('href'))).toEqual([
      '/dashboard', '/properties', '/work-orders', '/assets', '/preventive-maintenance', '/notifications'
    ]);
    expect(fixture.nativeElement.querySelector('[aria-label="Notifications"]')).not.toBeNull();
  });

  it('selects a property through the dropdown', fakeAsync(() => {
    fixture.nativeElement.querySelector('.mat-mdc-select-trigger').click();
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    const options = document.querySelectorAll('mat-option');
    expect(options.length).toBe(2);
    (options[1] as HTMLElement).click();
    fixture.detectChanges();
    expect(ctx.select).toHaveBeenCalledWith('p2');
    tick(1000);
  }));
});
