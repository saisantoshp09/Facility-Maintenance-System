import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { fakeJwt } from '../testing/fake-jwt';
import { AuthGuard, RoleGuard } from './guards';
import { TokenStore } from './token-store';

describe('guards', () => {
  let tokens: TokenStore;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [RouterTestingModule] });
    tokens = TestBed.inject(TokenStore);
    router = TestBed.inject(Router);
  });
  afterEach(() => tokens.clear());

  const state = (url: string) => ({ url } as any);
  const route = (roles?: string[]) => ({ data: { roles } } as any);

  it('AuthGuard redirects to login and preserves the return URL', () => {
    const res = TestBed.inject(AuthGuard).canActivate(route(), state('/work-orders/1')) as UrlTree;
    expect(router.serializeUrl(res)).toBe('/login?returnUrl=%2Fwork-orders%2F1');
  });

  it('AuthGuard allows an authenticated user', () => {
    tokens.set(fakeJwt());
    expect(TestBed.inject(AuthGuard).canActivate(route(), state('/x'))).toBeTrue();
  });

  it('RoleGuard sends users without the role to /forbidden', () => {
    tokens.set(fakeJwt({ roles: ['TENANT'] }));
    const res = TestBed.inject(RoleGuard).canActivate(route(['SYSTEM_ADMIN']), state('/users')) as UrlTree;
    expect(router.serializeUrl(res)).toBe('/forbidden');
  });

  it('RoleGuard allows a matching role', () => {
    tokens.set(fakeJwt({ roles: ['SYSTEM_ADMIN'] }));
    expect(TestBed.inject(RoleGuard).canActivate(route(['SYSTEM_ADMIN']), state('/users'))).toBeTrue();
  });
});
