import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../environments/environment';
import { AssetApi } from './asset.api';
import { IdentityApi } from './identity.api';
import { NotificationApi } from './notification.api';
import { PropertyApi } from './property.api';
import { WorkOrderApi } from './work-order.api';

describe('API clients: pagination and sort whitelists', () => {
  let ctrl: HttpTestingController;
  const base = `${environment.apiBaseUrl}/api/v1`;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [HttpClientTestingModule] });
    ctrl = TestBed.inject(HttpTestingController);
  });
  afterEach(() => ctrl.verify());

  it('sends page, size and a whitelisted sort', () => {
    TestBed.inject(WorkOrderApi).list({ propertyId: 'p1', status: 'OPEN' }, { page: 2, size: 50, sort: { field: 'dueAt', dir: 'desc' } }).subscribe();
    const r = ctrl.expectOne(req => req.url === `${base}/work-orders`);
    expect(r.request.params.get('propertyId')).toBe('p1');
    expect(r.request.params.get('status')).toBe('OPEN');
    expect(r.request.params.get('page')).toBe('2');
    expect(r.request.params.get('size')).toBe('50');
    expect(r.request.params.get('sort')).toBe('dueAt,desc');
    r.flush({ content: [], page: 2, size: 50, totalElements: 0, totalPages: 0 });
  });

  it('drops unsupported sort fields instead of causing a 400', () => {
    TestBed.inject(WorkOrderApi).list({ propertyId: 'p1' }, { sort: { field: 'title', dir: 'asc' } }).subscribe();
    const r = ctrl.expectOne(req => req.url === `${base}/work-orders`);
    expect(r.request.params.has('sort')).toBeFalse();
    r.flush({ content: [] });
  });

  it('clamps page size to the backend maximum of 100', () => {
    TestBed.inject(PropertyApi).list({ page: 0, size: 500 }).subscribe();
    const r = ctrl.expectOne(req => req.url === `${base}/properties`);
    expect(r.request.params.get('size')).toBe('100');
    r.flush({ content: [] });
  });

  it('tasks only sort by dueDate; users by their four fields', () => {
    TestBed.inject(AssetApi).tasks({ propertyId: 'p' }, { sort: { field: 'status', dir: 'asc' } }).subscribe();
    const t = ctrl.expectOne(req => req.url === `${base}/tasks`);
    expect(t.request.params.has('sort')).toBeFalse();
    t.flush({ content: [] });

    TestBed.inject(IdentityApi).list({ sort: { field: 'email', dir: 'asc' } }).subscribe();
    const u = ctrl.expectOne(req => req.url === `${base}/users`);
    expect(u.request.params.get('sort')).toBe('email,asc');
    u.flush({ content: [] });
  });

  it('notifications use the documented filters and sort fields', () => {
    TestBed.inject(NotificationApi).list({ sort: { field: 'readAt', dir: 'asc' } }, true, 'SENT').subscribe();
    const r = ctrl.expectOne(req => req.url === `${base}/notifications`);
    expect(r.request.params.get('unread')).toBe('true');
    expect(r.request.params.get('status')).toBe('SENT');
    expect(r.request.params.get('sort')).toBe('readAt,asc');
    r.flush({ content: [] });
  });

  it('tasks/due clamps withinDays to 0..365', () => {
    TestBed.inject(AssetApi).tasksDue('p', 9999).subscribe();
    const r = ctrl.expectOne(req => req.url === `${base}/tasks/due`);
    expect(r.request.params.get('withinDays')).toBe('365');
    r.flush([]);
  });
});
