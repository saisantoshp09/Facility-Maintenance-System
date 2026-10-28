import { Injectable } from '@angular/core';
import { Observable, catchError, map, of, shareReplay } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { IdentityApi } from '../api/identity.api';

export function shortId(id: string | null | undefined): string {
  return id ? id.substring(0, 8) : '';
}

/**
 * Resolves user ids to display names. The backend has no user directory for non-admins, so
 * only the current user and (for SYSTEM_ADMIN) GET /users/{id} can be resolved; otherwise
 * a short id is shown.
 */
@Injectable({ providedIn: 'root' })
export class UserDirectory {
  private cache = new Map<string, Observable<string>>();

  constructor(private auth: AuthService, private identity: IdentityApi) {}

  name$(id: string | null | undefined): Observable<string> {
    if (!id) { return of('—'); }
    if (id === this.auth.userId && this.auth.user) { return of(this.auth.user.displayName); }
    const cached = this.cache.get(id);
    if (cached) { return cached; }
    const obs = this.auth.isAdmin
      ? this.identity.get(id, true).pipe(map(u => u.displayName), catchError(() => of(shortId(id))), shareReplay(1))
      : of(shortId(id));
    this.cache.set(id, obs);
    return obs;
  }

  clear(): void { this.cache.clear(); }
}
