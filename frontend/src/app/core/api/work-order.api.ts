import { Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CommentResponse, CreateWorkOrderRequest, HistoryResponse, Page, PageQuery, TransitionRequest, WorkOrderFilter,
  WorkOrderResponse
} from '../models';
import { SKIP_ERROR_TOAST } from '../services/error-utils';
import { buildParams } from './params';

@Injectable({ providedIn: 'root' })
export class WorkOrderApi {
  private readonly base = `${environment.apiBaseUrl}/api/v1/work-orders`;
  constructor(private http: HttpClient) {}

  list(f: WorkOrderFilter, q: PageQuery, quiet = false): Observable<Page<WorkOrderResponse>> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, quiet);
    const params = buildParams('workOrders', q, {
      propertyId: f.propertyId, status: f.status, priority: f.priority, assigneeId: f.assigneeId,
      overdue: f.overdue === null || f.overdue === undefined ? undefined : f.overdue, dueBefore: f.dueBefore
    });
    return this.http.get<Page<WorkOrderResponse>>(this.base, { params, context });
  }
  get(id: string): Observable<WorkOrderResponse> { return this.http.get<WorkOrderResponse>(`${this.base}/${id}`); }
  create(req: CreateWorkOrderRequest): Observable<WorkOrderResponse> { return this.http.post<WorkOrderResponse>(this.base, req); }
  transition(id: string, req: TransitionRequest): Observable<WorkOrderResponse> {
    return this.http.post<WorkOrderResponse>(`${this.base}/${id}/transitions`, req);
  }
  setDueDate(id: string, dueAt: string): Observable<WorkOrderResponse> {
    return this.http.patch<WorkOrderResponse>(`${this.base}/${id}/due-date`, { dueAt });
  }
  comments(id: string, q: PageQuery): Observable<Page<CommentResponse>> {
    return this.http.get<Page<CommentResponse>>(`${this.base}/${id}/comments`, { params: buildParams('comments', q) });
  }
  addComment(id: string, body: string): Observable<CommentResponse> {
    return this.http.post<CommentResponse>(`${this.base}/${id}/comments`, { body });
  }
  history(id: string): Observable<HistoryResponse[]> { return this.http.get<HistoryResponse[]>(`${this.base}/${id}/history`); }
}
