import { Injectable } from '@angular/core';
import { HttpClient, HttpContext } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { NotificationAttempt, NotificationResponse, NotificationStatus, Page, PageQuery } from '../models';
import { SKIP_ERROR_TOAST } from '../services/error-utils';
import { buildParams } from './params';

@Injectable({ providedIn: 'root' })
export class NotificationApi {
  private readonly base = `${environment.apiBaseUrl}/api/v1`;
  constructor(private http: HttpClient) {}

  list(q: PageQuery, unread?: boolean | null, status?: NotificationStatus | null, quiet = false): Observable<Page<NotificationResponse>> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, quiet);
    return this.http.get<Page<NotificationResponse>>(`${this.base}/notifications`, {
      params: buildParams('notifications', q, { unread: unread ? true : undefined, status }), context
    });
  }
  markRead(id: string): Observable<NotificationResponse> {
    return this.http.post<NotificationResponse>(`${this.base}/notifications/${id}/read`, {});
  }
  unreadCount(): Observable<{ count: number }> {
    const context = new HttpContext().set(SKIP_ERROR_TOAST, true);
    return this.http.get<{ count: number }>(`${this.base}/notifications/unread-count`, { context });
  }
  attempts(id: string): Observable<NotificationAttempt[]> {
    return this.http.get<NotificationAttempt[]>(`${this.base}/admin/notifications/${id}/attempts`);
  }
}
