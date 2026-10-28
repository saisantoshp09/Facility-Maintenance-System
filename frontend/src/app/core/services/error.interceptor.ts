import { Injectable } from '@angular/core';
import { HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, catchError, retry, throwError, timer } from 'rxjs';
import { environment } from '../../../environments/environment';
import { TokenStore } from '../auth/token-store';
import { ErrorNotifier } from './error-notifier';
import { SKIP_ERROR_TOAST, toAppError } from './error-utils';

const RETRYABLE = [0, 502, 503, 504];

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private tokens: TokenStore, private router: Router, private notifier: ErrorNotifier) {}

  intercept(req: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    const isApi = req.url.startsWith(environment.apiBaseUrl);
    const isLogin = req.url.endsWith('/auth/login');
    // Only idempotent GETs are retried (max 2); mutating calls never are.
    const stream = req.method === 'GET'
      ? next.handle(req).pipe(retry({
        count: 2,
        delay: (err: HttpErrorResponse, n: number) =>
          RETRYABLE.includes(err.status) ? timer(300 * n) : throwError(() => err)
      }))
      : next.handle(req);

    return stream.pipe(catchError((err: HttpErrorResponse) => {
      if (!isApi) { return throwError(() => err); }
      const appErr = toAppError(err);
      if (appErr.status === 401 && !isLogin) {
        this.tokens.clear();
        if (!this.router.url.startsWith('/login')) {
          this.router.navigate(['/login'], { queryParams: { returnUrl: this.router.url, expired: '1' } });
        }
      } else if (!req.context.get(SKIP_ERROR_TOAST) && !isLogin
        && !(appErr.status === 400 && appErr.code === 'VALIDATION_ERROR')) {
        this.notifier.show(appErr);
      }
      return throwError(() => appErr);
    }));
  }
}
