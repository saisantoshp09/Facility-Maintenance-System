import { Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CreateUserRequest, Page, PageQuery, Role, UserResponse, UserStatus } from '../models';
import { SKIP_ERROR_TOAST } from '../services/error-utils';
import { buildParams } from './params';

@Injectable({ providedIn: 'root' })
export class IdentityApi {
  private readonly base = `${environment.apiBaseUrl}/api/v1/users`;
  constructor(private http: HttpClient) {}

  me(): Observable<UserResponse> { return this.http.get<UserResponse>(`${this.base}/me`); }
  list(q: PageQuery): Observable<Page<UserResponse>> {
    return this.http.get<Page<UserResponse>>(this.base, { params: buildParams('users', q) });
  }
  get(id: string, quiet = false): Observable<UserResponse> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, quiet);
    return this.http.get<UserResponse>(`${this.base}/${id}`, { context });
  }
  create(req: CreateUserRequest): Observable<UserResponse> { return this.http.post<UserResponse>(this.base, req); }
  setStatus(id: string, status: UserStatus): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.base}/${id}/status`, { status });
  }
  setRoles(id: string, roles: Role[]): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.base}/${id}/roles`, { roles });
  }
}
