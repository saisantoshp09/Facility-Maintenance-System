import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { Role } from '../models';

export interface TokenClaims { sub: string; roles: Role[]; exp: number; }

export function decodeJwtClaims(token: string): TokenClaims | null {
  try {
    const part = token.split('.')[1];
    const b64 = part.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(b64.padEnd(b64.length + ((4 - (b64.length % 4)) % 4), '='))
        .split('').map(c => '%' + c.charCodeAt(0).toString(16).padStart(2, '0')).join(''));
    const p = JSON.parse(json);
    return { sub: String(p.sub), roles: Array.isArray(p.roles) ? p.roles : [], exp: Number(p.exp) };
  } catch {
    return null;
  }
}

/**
 * Holds the access token in memory only (never localStorage/sessionStorage).
 * Claims are decoded purely as UI hints; the backend remains the security boundary.
 */
@Injectable({ providedIn: 'root' })
export class TokenStore {
  static readonly WARN_BEFORE_MS = 60_000;
  private token: string | null = null;
  private claimsValue: TokenClaims | null = null;
  private warnTimer: ReturnType<typeof setTimeout> | undefined;
  private expireTimer: ReturnType<typeof setTimeout> | undefined;
  readonly expiringSoon$ = new Subject<void>();
  readonly expired$ = new Subject<void>();

  get accessToken(): string | null { return this.token; }
  get claims(): TokenClaims | null { return this.claimsValue; }
  get userId(): string | null { return this.claimsValue?.sub ?? null; }
  get roles(): Role[] { return this.claimsValue?.roles ?? []; }

  isAuthenticated(): boolean {
    return !!this.token && !!this.claimsValue && this.claimsValue.exp * 1000 > Date.now();
  }

  hasRole(...roles: Role[]): boolean {
    return roles.some(r => this.roles.includes(r));
  }

  set(token: string): boolean {
    const claims = decodeJwtClaims(token);
    if (!claims) { this.clear(); return false; }
    this.clearTimers();
    this.token = token;
    this.claimsValue = claims;
    const msLeft = claims.exp * 1000 - Date.now();
    this.expireTimer = setTimeout(() => this.expired$.next(), Math.min(Math.max(0, msLeft), 2_147_000_000));
    if (msLeft > TokenStore.WARN_BEFORE_MS) {
      this.warnTimer = setTimeout(() => this.expiringSoon$.next(), Math.min(msLeft - TokenStore.WARN_BEFORE_MS, 2_147_000_000));
    }
    return true;
  }

  clear(): void {
    this.clearTimers();
    this.token = null;
    this.claimsValue = null;
  }

  private clearTimers(): void {
    if (this.warnTimer) { clearTimeout(this.warnTimer); }
    if (this.expireTimer) { clearTimeout(this.expireTimer); }
    this.warnTimer = undefined;
    this.expireTimer = undefined;
  }
}
