import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, switchMap, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginResponse, Role, UserResponse } from '../models';
import { TokenStore } from './token-store';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly base = `${environment.apiBaseUrl}/api/v1`;
  private readonly userSubject = new BehaviorSubject<UserResponse | null>(null);
  readonly user$ = this.userSubject.asObservable();

  constructor(private http: HttpClient, private tokens: TokenStore, private router: Router) {}

  get user(): UserResponse | null { return this.userSubject.value; }
  get userId(): string | null { return this.tokens.userId; }
  get roles(): Role[] { return this.tokens.roles; }
  get isAdmin(): boolean { return this.tokens.hasRole('SYSTEM_ADMIN'); }
  isAuthenticated(): boolean { return this.tokens.isAuthenticated(); }
  hasRole(...roles: Role[]): boolean { return this.tokens.hasRole(...roles); }

  login(email: string, password: string): Observable<UserResponse> {
    return this.http.post<LoginResponse>(`${this.base}/auth/login`, { email, password }).pipe(
      tap(res => {
        if (!this.tokens.set(res.access_token)) {
          throw new Error('Received an unreadable access token');
        }
      }),
      switchMap(() => this.loadProfile())
    );
  }

  loadProfile(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.base}/users/me`).pipe(tap(u => this.userSubject.next(u)));
  }

  /** Client-side only: there is no logout endpoint; the token simply expires server-side. */
  logout(returnUrl?: string, extra: Record<string, string> = {}): void {
    this.tokens.clear();
    this.userSubject.next(null);
    this.router.navigate(['/login'], { queryParams: { ...(returnUrl ? { returnUrl } : {}), ...extra } });
  }
}
