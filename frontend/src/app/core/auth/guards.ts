import { Injectable } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivate, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Role } from '../models';
import { TokenStore } from './token-store';

/** UX-only guards; every call is authorised again by the backend. */
@Injectable({ providedIn: 'root' })
export class AuthGuard implements CanActivate {
  constructor(private tokens: TokenStore, private router: Router) {}

  canActivate(_route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
    if (this.tokens.isAuthenticated()) { return true; }
    this.tokens.clear();
    return this.router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
  }
}

@Injectable({ providedIn: 'root' })
export class RoleGuard implements CanActivate {
  constructor(private tokens: TokenStore, private router: Router) {}

  canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): boolean | UrlTree {
    if (!this.tokens.isAuthenticated()) {
      return this.router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
    }
    const roles = (route.data['roles'] as Role[] | undefined) ?? [];
    return roles.length === 0 || this.tokens.hasRole(...roles) ? true : this.router.createUrlTree(['/forbidden']);
  }
}
