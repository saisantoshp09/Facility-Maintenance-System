import { HTTP_INTERCEPTORS, HttpClient } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { environment } from '../../../environments/environment';
import { AuthInterceptor } from '../auth/auth.interceptor';
import { TokenStore } from '../auth/token-store';
import { fakeJwt } from '../testing/fake-jwt';
import { ErrorInterceptor } from './error.interceptor';
import { ErrorNotifier } from './error-notifier';

describe('HTTP interceptors', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let tokens: TokenStore;
  let router: Router;
  let notifier: jasmine.SpyObj<ErrorNotifier>;
  const api = environment.apiBaseUrl;

  beforeEach(() => {
    notifier = jasmine.createSpyObj('ErrorNotifier', ['show', 'info']);
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule, MatSnackBarModule],
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true },
        { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
        { provide: ErrorNotifier, useValue: notifier }
      ]
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
    tokens = TestBed.inject(TokenStore);
    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });
  afterEach(() => { tokens.clear(); });

  it('adds the bearer token only to gateway requests', () => {
    tokens.set(fakeJwt());
    http.get(`${api}/api/v1/users/me`).subscribe();
    http.get('https://other.example/x').subscribe();
    expect(ctrl.expectOne(`${api}/api/v1/users/me`).request.headers.has('Authorization')).toBeTrue();
    expect(ctrl.expectOne('https://other.example/x').request.headers.has('Authorization')).toBeFalse();
  });

  it('handles a 401 with an empty body: clears the token and redirects to login', () => {
    tokens.set(fakeJwt());
    let err: any;
    http.get(`${api}/api/v1/properties`).subscribe({ error: e => err = e });
    ctrl.expectOne(`${api}/api/v1/properties`).flush(null, { status: 401, statusText: 'Unauthorized' });
    expect(err.status).toBe(401);
    expect(tokens.accessToken).toBeNull();
    expect(router.navigate).toHaveBeenCalled();
  });

  it('maps the error envelope and shows the correlation id', () => {
    let err: any;
    http.post(`${api}/api/v1/work-orders`, {}).subscribe({ error: e => err = e });
    ctrl.expectOne(`${api}/api/v1/work-orders`).flush(
      { status: 409, code: 'CONFLICT', message: 'Invalid transition', correlationId: 'cid-1', fieldErrors: [] },
      { status: 409, statusText: 'Conflict' });
    expect(err.code).toBe('CONFLICT');
    expect(err.correlationId).toBe('cid-1');
    expect(notifier.show).toHaveBeenCalled();
  });

  it('does not toast field validation errors', () => {
    http.post(`${api}/api/v1/users`, {}).subscribe({ error: () => undefined });
    ctrl.expectOne(`${api}/api/v1/users`).flush(
      { status: 400, code: 'VALIDATION_ERROR', message: 'bad', fieldErrors: [{ field: 'email', message: 'invalid' }] },
      { status: 400, statusText: 'Bad Request' });
    expect(notifier.show).not.toHaveBeenCalled();
  });

  it('retries idempotent GETs on 503 but at most twice', (done) => {
    http.get(`${api}/api/v1/properties`).subscribe({ error: e => { expect(e.status).toBe(503); done(); } });
    const fail = () => ctrl.expectOne(`${api}/api/v1/properties`).flush({}, { status: 503, statusText: 'Unavailable' });
    fail();
    setTimeout(() => {
      fail();
      setTimeout(() => fail(), 700);
    }, 400);
  });

  it('never retries POST', () => {
    http.post(`${api}/api/v1/work-orders`, {}).subscribe({ error: () => undefined });
    ctrl.expectOne(`${api}/api/v1/work-orders`).flush({}, { status: 503, statusText: 'Unavailable' });
    ctrl.expectNone(`${api}/api/v1/work-orders`);
  });
});
