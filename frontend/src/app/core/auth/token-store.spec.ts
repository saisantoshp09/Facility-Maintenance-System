import { fakeAsync, tick } from '@angular/core/testing';
import { fakeJwt } from '../testing/fake-jwt';
import { TokenStore, decodeJwtClaims } from './token-store';

describe('TokenStore', () => {
  it('decodes claims and reports roles (in memory only)', () => {
    const store = new TokenStore();
    expect(store.set(fakeJwt({ sub: 'abc', roles: ['SYSTEM_ADMIN', 'TENANT'] }))).toBeTrue();
    expect(store.userId).toBe('abc');
    expect(store.hasRole('SYSTEM_ADMIN')).toBeTrue();
    expect(store.hasRole('TECHNICIAN')).toBeFalse();
    expect(store.isAuthenticated()).toBeTrue();
    store.clear();
    expect(store.accessToken).toBeNull();
    expect(store.isAuthenticated()).toBeFalse();
  });

  it('never persists the token in web storage', () => {
    const store = new TokenStore();
    store.set(fakeJwt());
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
    store.clear();
  });

  it('rejects unreadable tokens', () => {
    const store = new TokenStore();
    expect(store.set('garbage')).toBeFalse();
    expect(decodeJwtClaims('a.b.c')).toBeNull();
  });

  it('treats an expired token as unauthenticated', () => {
    const store = new TokenStore();
    store.set(fakeJwt({ expInSeconds: -10 }));
    expect(store.isAuthenticated()).toBeFalse();
    store.clear();
  });

  it('emits a warning before expiry and an expiry event', fakeAsync(() => {
    const store = new TokenStore();
    let warned = 0;
    let expired = 0;
    store.expiringSoon$.subscribe(() => warned++);
    store.expired$.subscribe(() => expired++);
    store.set(fakeJwt({ expInSeconds: 120 }));
    tick(59_000);
    expect(warned).toBe(0);
    tick(2_000);
    expect(warned).toBe(1);
    tick(70_000);
    expect(expired).toBe(1);
    store.clear();
  }));
});
