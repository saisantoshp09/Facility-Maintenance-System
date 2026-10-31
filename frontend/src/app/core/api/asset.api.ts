import { Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AssetFilter, AssetRequest, AssetUpdateRequest, AssetView, Page, PageQuery, ScheduleRequest, ScheduleView,
  ServiceRecordRequest, ServiceRecordView, TaskFilter, TaskView
} from '../models';
import { SKIP_ERROR_TOAST } from '../services/error-utils';
import { buildParams } from './params';

@Injectable({ providedIn: 'root' })
export class AssetApi {
  private readonly root = `${environment.apiBaseUrl}/api/v1`;
  constructor(private http: HttpClient) {}

  list(f: AssetFilter, q: PageQuery, quiet = false): Observable<Page<AssetView>> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, quiet);
    return this.http.get<Page<AssetView>>(`${this.root}/assets`, {
      params: buildParams('assets', q, { propertyId: f.propertyId, status: f.status, category: f.category }), context
    });
  }
  get(id: string, quiet = false): Observable<AssetView> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, quiet);
    return this.http.get<AssetView>(`${this.root}/assets/${id}`, { context });
  }
  create(req: AssetRequest): Observable<AssetView> { return this.http.post<AssetView>(`${this.root}/assets`, req); }
  update(id: string, req: AssetUpdateRequest): Observable<AssetView> {
    return this.http.put<AssetView>(`${this.root}/assets/${id}`, req);
  }
  retire(id: string): Observable<AssetView> { return this.http.post<AssetView>(`${this.root}/assets/${id}/retire`, {}); }

  schedules(assetId: string): Observable<ScheduleView[]> {
    return this.http.get<ScheduleView[]>(`${this.root}/assets/${assetId}/schedules`);
  }
  createSchedule(assetId: string, req: ScheduleRequest): Observable<ScheduleView> {
    return this.http.post<ScheduleView>(`${this.root}/assets/${assetId}/schedules`, req);
  }
  deactivateSchedule(scheduleId: string): Observable<ScheduleView> {
    return this.http.patch<ScheduleView>(`${this.root}/schedules/${scheduleId}/deactivate`, {});
  }

  serviceRecords(assetId: string, q: PageQuery): Observable<Page<ServiceRecordView>> {
    return this.http.get<Page<ServiceRecordView>>(`${this.root}/assets/${assetId}/service-records`, {
      params: buildParams('serviceRecords', q)
    });
  }
  addServiceRecord(assetId: string, req: ServiceRecordRequest): Observable<ServiceRecordView> {
    return this.http.post<ServiceRecordView>(`${this.root}/assets/${assetId}/service-records`, req);
  }

  tasks(f: TaskFilter, q: PageQuery, quiet = false): Observable<Page<TaskView>> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, quiet);
    return this.http.get<Page<TaskView>>(`${this.root}/tasks`, {
      params: buildParams('tasks', q, {
        propertyId: f.propertyId, status: f.status, overdue: f.overdue ?? undefined, dueBefore: f.dueBefore
      }), context
    });
  }
  tasksDue(propertyId: string, withinDays = 7, quiet = false): Observable<TaskView[]> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, quiet);
    const days = Math.min(Math.max(Math.trunc(withinDays), 0), 365);
    return this.http.get<TaskView[]>(`${this.root}/tasks/due`, {
      params: buildParams(null, {}, { propertyId, withinDays: days }), context
    });
  }
}
