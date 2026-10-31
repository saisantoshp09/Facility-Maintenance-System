import { Injectable } from '@angular/core';
import { HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStore } from './token-store';

/** Adds the bearer token only to requests aimed at the configured gateway. */
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private tokens: TokenStore) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const token = this.tokens.accessToken;
    if (token && req.url.startsWith(environment.apiBaseUrl)) {
      return next.handle(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
    }
    return next.handle(req);
  }
}
